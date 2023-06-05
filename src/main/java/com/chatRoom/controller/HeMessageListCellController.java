package com.chatRoom.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextArea;
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
            nameViewButton.setText(item.getName());
            messageTextArea.setText(item.getMessage());
            setText(null);
            setGraphic(root);
        }
    }

    public AnchorPane getRoot() {
        return root;
    }
}
