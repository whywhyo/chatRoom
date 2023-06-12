package com.chatRoom.controller;

import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;

import java.io.IOException;

public class HeMessageListCellController extends ListCell<MessageData> {

    @FXML
    private Button nameViewButton;

    @FXML
    private TextArea messageTextArea;

    @FXML
    private AnchorPane root;

    private FXMLLoader mLLoader;

    @FXML
    private ImageView messageImageView;

    public HeMessageListCellController(MessageData item) {
        mLLoader = new FXMLLoader(getClass().getResource("/static/FXML/heMessageListCell.fxml"));
        mLLoader.setController(this);
        try {
            mLLoader.load();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Error loading heMessageCellController", e);
        }
        if(item == null) {
            setText(null);
            setGraphic(null);
        } else {
            if (item.getName() != null) {
                nameViewButton.setText(item.getName());
            }
            if (item.getFlag().equals("对方_图片")) {
                if(item.getMessage()!=null){
                    messageTextArea.setVisible(false);
                    messageImageView.setVisible(true);
                    messageImageView.setImage(new Image("file:/" + item.getMessage()));

                    // 根据图片的宽高比计算高度
                    messageImageView.setPreserveRatio(true);
                    double ratio = messageImageView.getImage().getHeight() / messageImageView.getImage().getWidth();
                    double height = 200 * ratio + 40;

                    // 设定ImageView和Cell的高度
                    messageImageView.setFitHeight(height); // 这里设定ImageView的高度
                    root.setMinHeight(height);
                    root.setMaxHeight(height);
                    root.setPrefHeight(height);
                }
            }else{
                messageImageView.setVisible(false);
                messageTextArea.setVisible(true);
                messageTextArea.setText(item.getMessage());
                // 恢复默认的高度
                double height = 85;
                root.setMinHeight(height);
                root.setMaxHeight(height);
                root.setPrefHeight(height);
            }
            setText(null);
            setGraphic(root);

        }
    }

    public AnchorPane getRoot() {
        return root;
    }
}
