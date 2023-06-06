package com.chatRoom.controller;

import com.chatRoom.domain.Message;
import com.chatRoom.service.client.ClientService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class AddNewFriendOrGroupController {

    @FXML
    ComboBox<String> choseAddModeComboBox;

    @FXML
    Button findButton;
    @FXML
    Button exitButton;

    @FXML
    TextField checkNameTextField;

    @FXML
    Label tipLabel;

    private ChatController chatController;

    @FXML
    public void initialize() {

        // 下拉框
        ObservableList<String> options = FXCollections.observableArrayList(
                "P2P",
                "添加群聊",
                "创建群聊"
        );
        choseAddModeComboBox.setItems(options);

        choseAddModeComboBox.setOnAction(event -> {
            String selectedOption = choseAddModeComboBox.getValue();
            System.out.println("选中的选项是：" + selectedOption);
            if (selectedOption.equals("创建群聊")) {
                findButton.setText("创   建   天   地");
            } else if (selectedOption.equals("添加群聊")){
                findButton.setText("加   入   群   聊");
            } else{
                findButton.setText("成   为   朋   友");
            }
        });

        findButton.setOnAction(event -> {
            findButtonOnAction();
        });

        exitButton.setOnAction(event -> {
            Stage stage = (Stage) exitButton.getScene().getWindow();
            stage.close();
        });
    }


    public void initializeWithChatController(ChatController chatController) {
        this.chatController = chatController;
    }

    private void findButtonOnAction() {
        if(checkNameTextField.getText().equals(chatController.getUsername())){
            tipLabel.setText("请寻找其他好友");
        }else{
            if (choseAddModeComboBox.getValue().equals("P2P")) {
                String findFriendName = checkNameTextField.getText();
                ClientService clientService = new ClientService();
                Message<String> message = new Message<>();
                message.setSender(chatController.getUsername());
                message.setReceiver(findFriendName);

                String result = clientService.sendFriendInvite(message);
                tipLabel.setText(result);
                if(result.equals("成功发送请求")){
                    findButton.setDisable(true);
                    findButton.setStyle("-fx-background-color: #CCCCCC;");
                }else if (result.equals("用户没上线")){
                    tipLabel.setText("请求失败，用户没上线");
                }
            } else if (choseAddModeComboBox.getValue().equals("添加群聊")) {
                ClientService clientService = new ClientService();
                Message<String> message = new Message<>();
                message.setSender(chatController.getUsername());
                message.setMessage(checkNameTextField.getText());
                clientService.joinGroup(message);

                chatController.renewFriendList();
                findButton.setDisable(true);
                findButton.setStyle("-fx-background-color: #CCCCCC;");

                // message要新建一席之地
                ObservableList<MessageData> messageDataList = FXCollections.observableArrayList();
                chatController.userMessageMap.put(checkNameTextField.getText(), messageDataList);

            } else if (choseAddModeComboBox.getValue().equals("创建群聊")) {
                ClientService clientService = new ClientService();
                Message<String> message = new Message<>();
                message.setSender(chatController.getUsername());
                message.setMessage(checkNameTextField.getText());
                clientService.createGroup(message);
                chatController.renewFriendList();
            }
        }
    }
}
