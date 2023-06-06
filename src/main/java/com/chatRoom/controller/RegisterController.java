package com.chatRoom.controller;

import com.chatRoom.domain.ChatRoomUser;
import com.chatRoom.domain.Message;
import com.chatRoom.service.client.ClientService;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.chatRoom.constant.MessageType.REGISTER;

public class RegisterController {

    @FXML
    private Button registerButton;
    @FXML
    private Button cancelButton;
    @FXML
    private Button sendConfirmCodeButton;

    @FXML
    private TextField userNameTextField;
    @FXML
    private PasswordField passwordTextField;
    @FXML
    private PasswordField confirmPasswordTextField;
    @FXML
    private TextField emailTextField;
    @FXML
    private TextField confirmMailTextField;

    @FXML
    private Label successTipLabel;
    @FXML
    private Label userNameFalseLabel;
    @FXML
    private Label passwordFalseLabel;
    @FXML
    private Label confirmPasswordFalseLabel;
    @FXML
    private Label emailFalseLabel;
    @FXML
    private Label confirmMailFalseLabel;


    @FXML
    public void registerButtonOnAction(ActionEvent event){
        String userName = userNameTextField.getText();
        String password = passwordTextField.getText();
        String confirmPassword = confirmPasswordTextField.getText();
        String email = emailTextField.getText();
        String confirmMail = confirmMailTextField.getText();

        userNameFalseLabel.setVisible(userName.equals(""));
        passwordFalseLabel.setVisible(password.equals(""));
        confirmPasswordFalseLabel.setVisible(confirmPassword.equals("") || !confirmPassword.equals(password));
        emailFalseLabel.setVisible(email.equals("") || !validate(email));
        confirmMailFalseLabel.setVisible(confirmMail.equals(""));

        ClientService clientService = new ClientService();
        ChatRoomUser user = new ChatRoomUser(userName, password,email,confirmMail);
        Message<ChatRoomUser> message = new Message<>();
        message.setMessageType(REGISTER);
        message.setMessage(user);

        String tip = clientService.registerRequest(message);

        if (tip.equals("成功将请求发送给服务端")){
            successTipLabel.setText("注册中...");
        }

        // 确认密码等于密码
        if(tip.equals("注册成功")){

            // 注册成功 TextField设为不可更改 label设为注册成功
            successTipLabel.setText("注册成功，点击取消退回至登录界面");
            userNameTextField.setEditable(false);
            passwordTextField.setEditable(false);
            confirmPasswordTextField.setEditable(false);
            emailTextField.setEditable(false);
            confirmMailTextField.setEditable(false);
            registerButton.setDisable(true);
            sendConfirmCodeButton.setDisable(true);
        }else if(tip.equals("用户已存在")){
            userNameFalseLabel.setVisible(true);
            successTipLabel.setText("注册失败，用户已存在");
        }
    }

    @FXML
    private void cancelButtonOnAction(ActionEvent event){
        // 如果注册成功 传username进login界面
        // 关闭注册(当前)界面 打开login界面
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
        Parent root = null;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/static/FXML/login.fxml"));
            root = loader.load();

            LoginController loginController = loader.getController();
            loginController.setData(userNameTextField.getText());

            Stage loginStage = new Stage();
            loginStage.initStyle(StageStyle.UNDECORATED);
            loginStage.setScene(new Scene(root,600,400));
            loginStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 验证码
    @FXML
    private void sendConfirmCodeButtonOnAction(){
        ClientService clientService = new ClientService();
        Message<String> message = new Message<>();
        message.setSender(userNameTextField.getText());
        message.setMessage(emailTextField.getText());

        clientService.getVerifyCode(message);
        new Thread(()-> {
            sendConfirmCodeButton.setDisable(true);
        }).start();
        new Thread(()->{
            int time = 20;
            while(time-->0){
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                int finalTime = time;
                Platform.runLater(() -> {
                    sendConfirmCodeButton.setText(finalTime + "");
                });
            }
            Platform.runLater(() -> {
                sendConfirmCodeButton.setDisable(false);
                sendConfirmCodeButton.setText("发送验证码");
            });
        }).start();
    }

    private static final String EMAIL_PATTERN =
            "^[\\w\\.-]+@[\\w\\.-]+\\.[a-zA-Z]{2,}$";

    private static final Pattern pattern = Pattern.compile(EMAIL_PATTERN);

    // 邮箱格式验证
    public static boolean validate(final String email) {
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
    }

}
