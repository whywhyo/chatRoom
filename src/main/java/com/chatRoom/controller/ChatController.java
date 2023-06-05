package com.chatRoom.controller;

import com.chatRoom.domain.DB.FriendDto;
import com.chatRoom.domain.DB.GroupDto;
import com.chatRoom.domain.Message;
import com.chatRoom.exception.ChatRoomException;
import com.chatRoom.service.client.ClientService;
import com.mysql.cj.util.StringUtils;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Callback;
import lombok.extern.slf4j.Slf4j;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.chatRoom.constant.MessageType.*;

@Slf4j
public class ChatController {
    @FXML
    private ListView<FriendData> friendListView;
    @FXML
    private ListView<MessageData> chatListView;
    @FXML
    private ListView<SimpleStringProperty> newFriendAskListView;

    @FXML
    private Button newFriendButton;
    @FXML
    private Button sendChatMessageButton;
    @FXML
    private Button exitButton;
    @FXML
    private Button changeRegisterButton;

    @FXML
    private Label userLoginNameLabel;

    @FXML
    private TextArea messageTextArea;

    private String username;

    private String tmpReceiver;
    private String friendOrGroup;

    private Socket chatClientSocket;

    List<SimpleStringProperty> newFriendDataList = new ArrayList<SimpleStringProperty>();

    public ObservableList<FriendData> observableFriendList = FXCollections.observableArrayList(new Callback<FriendData, Observable[]>() {
        @Override
        public Observable[] call(FriendData param) {
            SimpleStringProperty[] array = new SimpleStringProperty[]{param.getNameProperty(), param.getIsOnlineProperty()};
            return array;
        }
    });

    public ObservableList<SimpleStringProperty> observableAddFriendList = FXCollections.observableArrayList(new Callback<SimpleStringProperty, Observable[]>() {
        @Override
        public Observable[] call(SimpleStringProperty param) {
            SimpleStringProperty[] array = new SimpleStringProperty[]{param};
            return array;
        }
    });

    Map<String, ObservableList<MessageData>> userMessageMap = new HashMap<>();

    public void init() {
        userLoginNameLabel.setText(username);

        //初始化
        renewFriendList();
        renewAddFriendList();

        ClientService clientService = new ClientService();
        Message<String> message = new Message<>();
        message.setSender(username);
        List<FriendDto> friendList = clientService.getFriendList(message);
        friendList.forEach((friend) -> {
            ObservableList<MessageData> messageDataObservableList = FXCollections.observableArrayList(new Callback<MessageData, Observable[]>() {
                @Override
                public Observable[] call(MessageData param) {
                    SimpleStringProperty[] array = new SimpleStringProperty[]{param.getNameProperty(), param.getMessageProperty()};
                    return array;
                }
            });
            userMessageMap.put(friend.getUsername(), messageDataObservableList);
        });


        Message<String> getGroupMessage = new Message<>();
        getGroupMessage.setSender(username);
        List<GroupDto> groupList = clientService.getMyGroup(getGroupMessage);
        groupList.forEach((group) -> {
            ObservableList<MessageData> messageDataObservableList = FXCollections.observableArrayList(new Callback<MessageData, Observable[]>() {
                @Override
                public Observable[] call(MessageData param) {
                    SimpleStringProperty[] array = new SimpleStringProperty[]{param.getNameProperty(), param.getMessageProperty()};
                    return array;
                }
            });
            userMessageMap.put(group.getName(), messageDataObservableList);
        });

        new Thread(() -> {
            ObjectInputStream clientInputStream2 = null;
            Message readMessage = null;
            while (true) {
                try {
                    clientInputStream2 = new ObjectInputStream(chatClientSocket.getInputStream());
                    readMessage = (Message) clientInputStream2.readObject();//阻塞读取

                } catch (EOFException exception) {
                    log.info("登出成功");
                    break;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                log.info("接收者读到了信息");
                if (readMessage.getMessageType() == MESSAGE_SEND) {
                    String messageContent = (String) readMessage.getMessage();
                    String sender = readMessage.getSender();
                    log.info("收到来自{}的信息:{}", sender, messageContent);

                    // 存储进userMessageMap
                    Platform.runLater(() -> {
                        ObservableList<MessageData> messageDataList = userMessageMap.get(sender);
                        MessageData messageData = new MessageData(sender, messageContent, "对方");
                        messageDataList.add(messageData);
                    });

                    // TODO: 2023-06-02 收到信息后把记录存到本地
                }
                else if (readMessage.getMessageType() == FRIEND_INVITE) {
                    //接收到一条好友申请
                    //页面这里把一条好友申请的信息加到对应的列表中
                    log.info("收到来自{}的好友申请", readMessage.getSender());
                    String stringSender = readMessage.getSender();
                    Platform.runLater(() -> {
                        SimpleStringProperty name = new SimpleStringProperty();
                        name.set(stringSender);
                        newFriendDataList.add(name);
                        observableAddFriendList.addAll(newFriendDataList);
                    });
                }
                else if (readMessage.getMessageType() == ACK_FRIEND_INVITE) {
                    //接收到好友申请的结果，页面需要把结果展示在对应那条记录中
                    if (!(Boolean) readMessage.getMessage()) {
                        //如果拒绝了
                        log.info("{}拒绝了{}的好友申请", readMessage.getSender(), readMessage.getReceiver());
                    } else {
                        //接收到对方的同意
                        log.info("{}同意了{}的好友申请", readMessage.getSender(), readMessage.getReceiver());
                        //更新好友列表
                        renewFriendList();
                        //message要增添新朋友(sender)的一席之地
                        ObservableList<MessageData> messageDataList = FXCollections.observableArrayList();
                        userMessageMap.put(readMessage.getSender(), messageDataList);
                    }
                }
                else if (readMessage.getMessageType() == GROUP_CHAT) {
                    //接收到一条群聊信息
                    String messageContent = (String) readMessage.getMessage();
                    String sender = readMessage.getSender();
                    String receiver = readMessage.getReceiver();
                    log.info("用户:{} 在群:{} 发了一条信息如下:{}", readMessage.getSender(), readMessage.getReceiver(), readMessage.getMessage());
                    Platform.runLater(() -> {
                        ObservableList<MessageData> messageDataList = userMessageMap.get(receiver);
                        MessageData messageData = new MessageData(sender, messageContent, "对方");
                        messageDataList.add(messageData);
                    });
                }
                else if (readMessage.getMessageType() == IS_DELETE){
                    String messageContent = (String) readMessage.getMessage();
                    log.info(messageContent);
                    renewFriendList();
                }
                else {
                    log.error("本线程接收到未知类型的对象");
                    throw new ChatRoomException("系统出现未知错误，请联系管理员");
                }
            }
        }).start();


        //点击ALT发送消息
        messageTextArea.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ALT) {
                sendMessage();
            }
        });

        // 监听observableList的更新
        observableFriendList.addListener(new InvalidationListener() {
            @Override
            public void invalidated(Observable observable) {
                ObservableList<FriendData> data = (ObservableList) observable;
                data.forEach(new Consumer<FriendData>() {
                    @Override
                    public void accept(FriendData friendData) {
                        System.out.println(friendData.getName() + "-" + friendData.isOnline());
                    }
                });
            }
        });

        observableFriendList.addListener(new ListChangeListener<FriendData>() {
            @Override
            public void onChanged(Change<? extends FriendData> c) {
                while (c.next()) {
                    if (c.wasUpdated()) {
                        System.out.println("更新");
                    }
                    if (c.wasRemoved()) {
                        System.out.println("删除");
                    }
                }
            }
        });

    }

    @FXML
    private void exitButtonOnAction(ActionEvent event) {
        ClientService clientService = new ClientService();
        Message<String> message = new Message<>();
        message.setSender(username);
        clientService.logoutRequest(message);

        Stage stage = (Stage) exitButton.getScene().getWindow();
        stage.close();

        newLogin();
    }

    @FXML
    private void sendChatMessageButtonOnAction(ActionEvent event) {sendMessage();}

    @FXML
    private void addNewFriendButtonOnAction(ActionEvent event) {addNewFriend();}

    @FXML
    private void changeRegisterButtonOnAction(ActionEvent event) {changeRegister();}

    public void addNewFriend() {
        Parent root = null;
        AddNewFriendOrGroupController addNewFriendOrGroupController= new AddNewFriendOrGroupController();
        FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setLocation(getClass().getResource("/static/FXML/addNewFriendOrGroup.fxml"));
        addNewFriendOrGroupController.initializeWithChatController(this);
        fxmlLoader.setController(addNewFriendOrGroupController);
        try {
            root = fxmlLoader.load();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Stage addNewFriendStage = new Stage();
        addNewFriendStage.initStyle(StageStyle.UNDECORATED);
        if (root != null) {
            addNewFriendStage.setScene(new Scene(root, 358, 680));
            addNewFriendStage.show();
        } else {
            System.out.println("Root is null!");
        }
    }

    public void changeRegister() {
        Parent root = null;
        FXMLLoader loader = null;
        try {
            loader = new FXMLLoader(getClass().getResource("/static/FXML/changeData.fxml"));
            root = loader.load();
            ChangeDataController changeDataController = loader.getController();
            Stage changeRegisterStage = new Stage();
            changeRegisterStage.initStyle(StageStyle.UNDECORATED);
            changeDataController.init(username);
            changeRegisterStage.setScene(new Scene(root, 358, 680));
            changeRegisterStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void newLogin() {
        Parent root = null;
        try {
            root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/static/FXML/login.fxml")));
            Stage newLoginStage = new Stage();
            newLoginStage.initStyle(StageStyle.UNDECORATED);
            newLoginStage.setScene(new Scene(root, 600, 400));
            newLoginStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //发送消息
    private void sendMessage() {
        System.out.println(this.tmpReceiver);
        if(!StringUtils.isNullOrEmpty(this.tmpReceiver)) {
            String message = messageTextArea.getText();
            messageTextArea.clear();

            Message<String> stringMessage = new Message<>();
            stringMessage.setSender(username);
            stringMessage.setMessage(message);
            stringMessage.setReceiver(tmpReceiver);

            // 分辨是群聊消息还是个人消息
            ClientService clientService = new ClientService();
            System.out.println(friendOrGroup);
            if (friendOrGroup.equals("个人")){
                clientService.sendMessage(stringMessage);
            }else if(friendOrGroup.equals("群聊")){
                clientService.sendMessageToGroup(stringMessage);
            }

            // 更新MessageMap
            ObservableList<MessageData> messageDataList = userMessageMap.get(tmpReceiver);
            MessageData messageData = new MessageData(username, message, "本人");
            messageDataList.add(messageData);
        }
    }

    // 从friendCell里面切换至好友对应的聊天信息
    public void switchToMessageList(String friendUsername,String interFriendOrGroup) {
        this.tmpReceiver = friendUsername;
        this.friendOrGroup = interFriendOrGroup;
        chatListView.setItems(userMessageMap.get(friendUsername));
        chatListView.setCellFactory(param -> new ListCell<MessageData>() {
            @Override
            protected void updateItem(MessageData item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    if (item.getFlag().equals("对方")) {
                        setGraphic(new HeMessageListCellController(item).getRoot());
                    } else if (item.getFlag().equals("本人")) {
                        setGraphic(new MyMessageListCellController(item).getRoot());
                    }
                }
            }
        });
    }

    // 刷新好友列表
    public void renewFriendList(){
        ClientService clientService = new ClientService();
        Message<String> message = new Message<>();
        message.setSender(username);

        // 好友列表
        List<FriendDto> friendList = clientService.getFriendList(message);
        List<GroupDto> groupList = clientService.getMyGroup(message);

        List<FriendData> friendDataList = friendList.stream().map((friend) -> {
            String isOnline = null;
            if (friend.getFlag() == 1) {
                isOnline = "在线";
            } else {
                isOnline = "离线";
            }
            return new FriendData(friend.getUsername(), isOnline, "个人");
        }).collect(Collectors.toList());

        List<FriendData> groupDataList = groupList.stream().map((group) -> {
            return new FriendData(group.getName(), "xxx", "群聊");
        }).collect(Collectors.toList());

        Platform.runLater(() -> {
            observableFriendList.clear();

            observableFriendList.addAll(friendDataList);
            observableFriendList.addAll(groupDataList);
            friendListView.setItems(observableFriendList);
            friendListView.setCellFactory(param -> new FriendListCellController(this));
        });
    }

    public void renewAddFriendList(){
        Platform.runLater(() -> {
            observableAddFriendList.clear();

            observableAddFriendList.addAll(newFriendDataList);
            newFriendAskListView.setItems(observableAddFriendList);
            newFriendAskListView.setCellFactory(param -> new ReceiveFriendCellController(this));
        });
    }

    public void setUsername(String username,Socket chatClientSocket) {
        this.username = username;
        this.chatClientSocket = chatClientSocket;
    }

    public String getUsername(){
        return this.username;
    }
}
