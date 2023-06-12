package com.chatRoom.controller;

import com.chatRoom.domain.ChatRoomUser;
import com.chatRoom.domain.DB.DBMessage;
import com.chatRoom.domain.Message;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import javafx.stage.StageStyle;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import static com.chatRoom.constant.MessageType.*;
import static com.chatRoom.constant.SystemConstant.SERVER_HOST;
import static com.chatRoom.constant.SystemConstant.SERVER_PORT;

@Slf4j
public class LoginController {

    private String data;

    @FXML
    private Label loginMessageLabel;

    @FXML
    private Button loginButton;
    @FXML
    private Button cancelButton;
    @FXML
    private Button registerButton;

    @FXML
    private TextField userNameTextField;
    @FXML
    private PasswordField passwordTextField;

    private Socket loginSocket;

    private Stage primaryStage;

    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    @FXML
    public void loginButtonOnAction(ActionEvent event) {

        ChatRoomUser chatRoomUser = new ChatRoomUser();
        chatRoomUser.setUsername(userNameTextField.getText());
        chatRoomUser.setPassword(passwordTextField.getText());

        Message<ChatRoomUser> message = new Message<>();
        message.setMessage(chatRoomUser);
        message.setMessageType(LOGIN);


        new Thread(() -> {


            try {
                loginSocket = new Socket(SERVER_HOST, SERVER_PORT);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            //发送请求登录信息给服务端
            ObjectOutputStream clientOutputStream = null;
            try {
                clientOutputStream = new ObjectOutputStream(loginSocket.getOutputStream());
                clientOutputStream.writeObject(message);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            ObjectInputStream clientInputStream = null;
            try {
                //接收服务端发回来的登录结果
                clientInputStream = new ObjectInputStream(loginSocket.getInputStream());
                Message<DBMessage<String>> loginResultMessage = (Message<DBMessage<String>>) clientInputStream.readObject();

                if (!loginResultMessage.getMessage().isSuccess()) {
                    //登陆失败,在界面显示登录失败
                    log.info(loginResultMessage.getMessage().getContent());
                    Platform.runLater(() -> {
                        loginMessageLabel.setText("登陆失败，请检查账号密码");
                    });
                } else {
                    log.info(loginResultMessage.getMessage().getContent());


                    new Thread(()->{

                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        Platform.runLater(() -> {
                            // 关闭当前界面
                            Stage stage = (Stage) loginButton.getScene().getWindow();
                            stage.close();

                            //打开聊天界面
                            startChat(chatRoomUser.getUsername());
                        });
                    }).start();
                }


            } catch (IOException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }

        }).start();

    }

    @FXML
    private void registerButtonOnAction(ActionEvent event) {
        Stage stage = (Stage) registerButton.getScene().getWindow();
        stage.close();
        createAccountForm();
    }

    @FXML
    private void cancelButtonOnAction(ActionEvent event) {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }


    public void createAccountForm() {
        Parent root = null;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/static/FXML/register.fxml"));
            root = loader.load();
            Stage registerStage = new Stage();
            registerStage.initStyle(StageStyle.UNDECORATED);
            registerStage.setScene(new Scene(root, 440, 710));
            registerStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void startChat(String username) {
        Parent root = null;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/static/FXML/chat.fxml"));
            root = loader.load();
            Stage registerStage = new Stage();

            ChatController chatController = loader.getController();
            log.info("login传给chat的socket："+loginSocket);
            chatController.setUsername(username,loginSocket);
            chatController.setPrimaryStage(this.primaryStage);
            chatController.init();

            registerStage.setScene(new Scene(root, 1000, 600));
            registerStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setData(String data) {
        this.data = data;
        userNameTextField.setText(this.data);
    }
}
