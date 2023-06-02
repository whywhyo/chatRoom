package com.chatRoom.service.client;

import com.chatRoom.constant.MessageType;
import com.chatRoom.domain.ChatRoomUser;
import com.chatRoom.domain.DBMessage;
import com.chatRoom.domain.Message;
import com.chatRoom.exception.ChatRoomException;
import lombok.extern.slf4j.Slf4j;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;

import static com.chatRoom.constant.ConstantKey.USER_INFO;
import static com.chatRoom.constant.MessageType.*;
import static com.chatRoom.constant.SystemConstant.SERVER_HOST;
import static com.chatRoom.constant.SystemConstant.SERVER_PORT;

/**
 * @Author Jinquan_Ou
 * @Description 客户端功能
 * @Date 2023-05-29 23:50
 * @Version 1.0.0
 **/
@Slf4j
public class ClientService {
    /**
     * 登录请求
     */
    public void loginRequest(Message<ChatRoomUser> message){
        Socket clientSocket = null;
        try {
            clientSocket = new Socket(SERVER_HOST, SERVER_PORT);
            log.info("客户端连接上了服务器");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        message.setMessageType(LOGIN);

        //发送请求登录信息给服务端
        ObjectOutputStream clientOutputStream =null;
        try {
            clientOutputStream = new ObjectOutputStream(clientSocket.getOutputStream());
            clientOutputStream.writeObject(message);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        ObjectInputStream clientInputStream = null;
        try {
            //接收服务端发回来的登录结果
            clientInputStream = new ObjectInputStream(clientSocket.getInputStream());
            Message<DBMessage<String>> loginResultMessage = (Message<DBMessage<String>>) clientInputStream.readObject();

            if (!loginResultMessage.getMessage().isSuccess()) {
                //登陆失败
                log.info(loginResultMessage.getMessage().getContent());
            }else {
                //登陆成功
                log.info(loginResultMessage.getMessage().getContent());
                log.info("进入信息监听状态");
                ObjectInputStream clientInputStream2 = null;
                Message<String> message2= null;
                while(true) {

                    if(clientSocket.isClosed()){
                        break;
                    }

                    try {
                        clientInputStream2 = new ObjectInputStream(clientSocket.getInputStream());
                        message2 = (Message<String>) clientInputStream2.readObject();//阻塞读取
                    }catch (EOFException exception){
                        log.info("登出成功");
                        break;
                    }
                    catch (Exception e) {
                        throw new RuntimeException(e);
                    }

                    if (!(message2.getMessageType() == MessageType.MESSAGE_SEND)) {
                        log.error("本线程接收到未知类型的对象");
                        throw new ChatRoomException("系统出现未知错误，请联系管理员");
                    } else {
                        String messageContent = message2.getMessage();
                        String sender = message2.getSender();
                        log.info("收到来自{}的信息:{}", sender, messageContent);
                    }
                }
            }

        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 发送信息的请求
     */

    public void sendMessage(Message message){
        sendMessageToServer(message, MESSAGE_SEND);
    }

    /**
     * 登出请求
     */
    public void logoutRequest(Message message){
       sendMessageToServer(message, LOGOUT);
    }

    /**
     * 注册请求
     */
    public void registerRequest(Message<ChatRoomUser> message){
        sendMessageToServer(message,REGISTER);
    }

    private static void sendMessageToServer(Message message, MessageType messageType) {
        //重新开一个socket用来发信息，因为登录的socket已经用来监听信息了
        Socket senderSocket = null;
        try {
            senderSocket = new Socket(SERVER_HOST, SERVER_PORT);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        //设置一下message的类型
        message.setMessageType(messageType);

        //直接把message数据发给服务端，让服务端来转发
        ObjectOutputStream senderOutputStream = null;
        try {
            senderOutputStream = new ObjectOutputStream(senderSocket.getOutputStream());
            senderOutputStream.writeObject(message);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        log.info("成功将请求发送给服务端");


        //接收服务端的反馈
        try {
            ObjectInputStream clientInputStream = new ObjectInputStream(senderSocket.getInputStream());
            Message<String> result = (Message<String>) clientInputStream.readObject();
            log.info(result.getMessage());
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

    }

}
