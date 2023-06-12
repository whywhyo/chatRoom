package com.chatRoom.controller;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * @Author Mei_Deng
 * @Description
 * @Date 2023-06-1 9:14
 * @Version 1.0.0
 **/

public class ClientControllerTest extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/static/FXML/login.fxml"));
        Parent root = loader.load();
        LoginController controller = loader.getController();
        controller.setPrimaryStage(primaryStage);
        primaryStage.setScene(new Scene(root, 600,400));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
