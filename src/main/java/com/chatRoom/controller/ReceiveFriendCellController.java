package com.chatRoom.controller;

import com.chatRoom.domain.Message;
import com.chatRoom.service.client.ClientService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.AnchorPane;

import java.io.IOException;

public class ReceiveFriendCellController extends ListCell<SimpleStringProperty> {
    @FXML
    private Button ackAsFriendButton;
    @FXML
    private Button refuseAsFriendButton;
    @FXML
    private Button addTipButton;

    @FXML
    private Label ackMessageLabel;

    private FXMLLoader mLLoader;

    @FXML
    private AnchorPane root;

    private String addFriendUsername;
    private ChatController chatController;

    public ReceiveFriendCellController(ChatController chatController) {
        this.chatController = chatController;

        mLLoader = new FXMLLoader(getClass().getResource("/static/FXML/receiveFriendCell.fxml"));
        mLLoader.setController(this);
        try {
            mLLoader.load();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Error loading receiveFriendCellController", e);
        }

        addTipButton.setText(addFriendUsername);

        ackAsFriendButton.setOnAction(event -> {
            // 同意对方好友请求
            ClientService clientService = new ClientService();
            Message<Boolean> message = new Message<>();
            message.setSender(chatController.getUsername());
            message.setReceiver(addTipButton.getText());
            message.setMessage(true);

            clientService.ackFriendInvite(message);

            //message要增添新朋友的一席之地
            ObservableList<MessageData> messageDataList = FXCollections.observableArrayList();
            chatController.userMessageMap.put(addTipButton.getText(), messageDataList);

            // UI中隐藏两个按钮，替换为通过好友请求
            ackAsFriendButton.setVisible(false);
            refuseAsFriendButton.setVisible(false);
            ackMessageLabel.setVisible(true);

            chatController.renewFriendList();
        });

        refuseAsFriendButton.setOnAction(event -> {
            // 拒绝对方好友请求
            ClientService clientService = new ClientService();
            Message<Boolean> message = new Message<>();
            message.setSender(chatController.getUsername());
            message.setReceiver(addTipButton.getText());
            message.setMessage(false);

            clientService.ackFriendInvite(message);

            // UI中隐藏两个按钮，替换为通过好友请求
            ackAsFriendButton.setVisible(false);
            refuseAsFriendButton.setVisible(false);
            ackMessageLabel.setText("已拒绝");
            ackMessageLabel.setVisible(true);
        });

    }

    @Override
    protected void updateItem(SimpleStringProperty item, boolean empty) {
        super.updateItem(item, empty);

        if(empty || item == null) {
            setText(null);
            setGraphic(null);
        } else {
            addTipButton.setText(item.getValue());

            setText(null);
            setGraphic(root);
        }
    }

}
