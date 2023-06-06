package com.chatRoom.controller;

import com.chatRoom.domain.Message;
import com.chatRoom.service.client.ClientService;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.AnchorPane;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class FriendListCellController extends ListCell<FriendData> {

    @FXML
    private Button chatButton;
    @FXML
    private Button deleteFriendButton;

    @FXML
    private Label stateLabel;

    @FXML
    private AnchorPane root;

    private FXMLLoader mLLoader;

    private ChatController chatController;
    private String friendUsername;


    public FriendListCellController(ChatController chatController) {
        this.chatController = chatController;

        mLLoader = new FXMLLoader(getClass().getResource("/static/FXML/friendListCell.fxml"));
        mLLoader.setController(this);
        try {
            mLLoader.load();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Error loading FriendListCellController", e);
        }

        chatButton.setOnAction(event -> {

            chatButton.setStyle("-fx-background-color:  #40b087;"); // 绿色

            // 在500毫秒后将按钮颜色恢复为红色
            new Thread(() -> {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                // 使用Platform.runLater来改变UI组件
                Platform.runLater(() -> {
                    if(stateLabel.getText().equals("群聊")){
                        chatButton.setStyle("-fx-background-color: #e594d7;");
                    }else{
                        chatButton.setStyle("-fx-background-color: #0095B6;");
                    }
                }); // 红色
            }).start();

            if(stateLabel.getText().equals("群聊")){
                chatController.switchToMessageList(friendUsername,"群聊");
                System.out.println("群聊聊天界面");
            }else{
                chatController.switchToMessageList(friendUsername,"个人");
                System.out.println("个人聊天界面");
            }
            System.out.println(friendUsername);
        });

        deleteFriendButton.setOnAction(event -> {
            ClientService clientService = new ClientService();
            Message<Boolean> message = new Message<>();
            message.setSender(chatController.getUsername());
            message.setReceiver(chatButton.getText());
            clientService.deleteFriend(message);
            chatController.renewFriendList();
        });
    }

    @Override
    protected void updateItem(FriendData item, boolean empty) {
        super.updateItem(item, empty);

        if(empty || item == null) {
            setText(null);
            setGraphic(null);
        } else {
            chatButton.setText(item.getName());
            stateLabel.setText(item.isOnline());
            if(item.isOnline().equals("离线")){
                stateLabel.setStyle("-fx-text-fill: #6c6c6c;");
            }
            this.friendUsername = chatButton.getText();

            if (item.getUserOrGroup().equals("群聊")) {
                chatButton.setStyle("-fx-background-color:#e594d7;");
                stateLabel.setText("群聊");
                deleteFriendButton.setText("删除群聊");
            }

            setText(null);
            setGraphic(root);
        }
    }
}
