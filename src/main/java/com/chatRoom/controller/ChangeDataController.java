package com.chatRoom.controller;

import com.chatRoom.domain.Message;
import com.chatRoom.service.client.ClientService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;


public class ChangeDataController {

    @FXML
    private Button changeButton;
    @FXML
    private Button exitButton;

    @FXML
    ComboBox<String> choseChangeModeComboBox;

    @FXML
    private TextField newChangeTextField;
    @FXML
    private TextField confirmTextField;

    @FXML
    private Label tipLabel;
    @FXML
    private Label label2;

    private String userName;

    public void init(String userName){
        this.userName = userName;
    }

    @FXML
    private void exitButtonOnAction(ActionEvent event){
        Stage stage = (Stage) exitButton.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void changeButtonOnAction(ActionEvent event){
        ClientService clientService = new ClientService();
        Message<String> message = new Message<>();
        String passWord = newChangeTextField.getText();
        message.setMessage(passWord);
        message.setSender(this.userName);
        if(clientService.changeDataRequest(message).equals("成功将请求发送给服务端")) {
            tipLabel.setText("修改成功！");
            changeButton.setDisable(true);
        };
    }

    public void initialize() {
        // 下拉框
        ObservableList<String> options = FXCollections.observableArrayList(
                "更改密码",
                "更改邮箱"
        );
        choseChangeModeComboBox.setItems(options);

        choseChangeModeComboBox.setOnAction(event -> {
            String selectedOption = choseChangeModeComboBox.getValue();
            System.out.println("选中的选项是：" + selectedOption);
            if(selectedOption.equals("更改密码")){
                label2.setText("新 密 码:");
                confirmTextField.setVisible(false);

            }else if(selectedOption.equals("更改邮箱")){
                label2.setText("新 邮 箱:");
                confirmTextField.setVisible(true);
            }
        });
    }
}
