package com.chatRoom.service.client;

import com.chatRoom.constant.MessageType;
import com.chatRoom.domain.ChatRoomUser;
import com.chatRoom.domain.DB.DBMessage;
import com.chatRoom.domain.DB.FriendDto;
import com.chatRoom.domain.Message;
import com.chatRoom.exception.ChatRoomException;
import lombok.extern.slf4j.Slf4j;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;

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
    public void loginRequest(Message<ChatRoomUser> message) {
        Socket clientSocket = null;
        try {
            clientSocket = new Socket(SERVER_HOST, SERVER_PORT);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        message.setMessageType(LOGIN);

        //发送请求登录信息给服务端
        ObjectOutputStream clientOutputStream = null;
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
            } else {
                //登陆成功
                log.info(loginResultMessage.getMessage().getContent());
                log.info("{}登陆成功，进入信息监听状态",message.getMessage().getUsername());
                ObjectInputStream clientInputStream2 = null;
                Message readMessage = null;
                while (true) {
                    if (clientSocket.isClosed()) {
                        break;
                    }

                    try {
                        clientInputStream2 = new ObjectInputStream(clientSocket.getInputStream());
                        readMessage = (Message) clientInputStream2.readObject();//阻塞读取
                    } catch (EOFException exception) {
                        log.info("登出成功");
                        //因异常退出，也要修改数据库的状态字段
                        Message<String> logoutMessage = new Message<>();
                        logoutMessage.setSender(message.getMessage().getUsername());
                        logoutRequest(logoutMessage);
                        break;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }

                    if (readMessage.getMessageType() == MESSAGE_SEND) {

                        String messageContent = (String) readMessage.getMessage();
                        String sender = readMessage.getSender();
                        log.info("收到来自{}的信息:{}", sender, messageContent);

                        // TODO: 2023-06-02 收到信息后把记录存到本地
                    } else if (readMessage.getMessageType() == FRIEND_INVITE) {
                        //接收到一条好友申请
                        //到时页面这里把一条好友申请的信息加到对应的列表中
                        log.info("收到来自{}的好友申请", readMessage.getSender());
                    } else if (readMessage.getMessageType() == ACK_FRIEND_INVITE) {
                        //接收到好友申请的结果，页面需要把结果展示在对应那条记录中
                        if (!(Boolean) readMessage.getMessage()){
                            //如果拒绝了
                            log.info("{}拒绝了{}的好友申请",readMessage.getSender(),readMessage.getReceiver());
                        }else {
                            //如果同意了
                            log.info("{}同意了{}的好友申请",readMessage.getSender(),readMessage.getReceiver());
                        }
                    } else if(readMessage.getMessageType() == GROUP_CHAT){
                        //接收到一条群聊信息
                        log.info("用户:{} 在群:{} 发了一条信息如下:{}",readMessage.getSender(),readMessage.getReceiver(),readMessage.getMessage());
                    }
                    else {
                        log.error("本线程接收到未知类型的对象");
                        throw new ChatRoomException("系统出现未知错误，请联系管理员");
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

    public void sendMessage(Message message) {
        sendMessageToServer(message, MESSAGE_SEND);
        // TODO: 2023-06-02 发完信息之后需要将记录存到本地
    }

    /**
     * 登出请求
     */
    public void logoutRequest(Message message) {
        Message returnMessage = sendMessageToServer(message, LOGOUT);
        String result = (String) returnMessage.getMessage();
        log.info(result);
    }

    /**
     * 注册请求
     */
    public void registerRequest(Message<ChatRoomUser> message) {
        Message returnMessage = sendMessageToServer(message, REGISTER);
        log.info((String) returnMessage.getMessage());
    }

    /**
     * 拉取好友列表请求
     */
    public void getFriendList(Message message) {
        Message<DBMessage> resultMessage = sendMessageToServer(message, GET_FRIEND);

        // 判断
        DBMessage dbMessage = resultMessage.getMessage();
        if (!dbMessage.isSuccess()) {
            //如果用户本来就没有注册
            String noRegister = (String) dbMessage.getContent();
            log.info(noRegister);
        } else {
            //判断好友列表是否为空
            List<FriendDto> friendList = (List<FriendDto>) dbMessage.getContent();
            if (friendList.size() > 0) {
                for (int i = 0; i < friendList.size(); i++) {
                    //这里做一个输出操作，到时结合写界面再具体处理返回的数据吧
                    System.out.println("第" + (i + 1) + "个好友为" + friendList.get(i));
                }
            } else {
                System.out.println("您还没有添加好友");
            }
        }
    }

    /**
     * 发起好友申请
     */
    public void sendFriendInvite(Message message) {
        Message returnMessage = sendMessageToServer(message, FRIEND_INVITE);
        String result = (String) returnMessage.getMessage();
        log.info(result);
        //到时页面这里可以加一列申请好友的信息列表，展示已同意或者不同意或者等待中
    }

    /**
     * 应答好友申请
     */
    public void ackFriendInvite(Message<Boolean> message) {
        Message returnMessage = sendMessageToServer(message, ACK_FRIEND_INVITE);
        String result = (String) returnMessage.getMessage();
        log.info(result);
    }

    /**
     * 删除好友
     */
    public void deleteFriend(Message<Boolean> message){
        Message returnMessage = sendMessageToServer(message, DELETE_FRIEND);
        String result = (String) returnMessage.getMessage();
        log.info(result);
    }

    /**
     * 得到我加入的群聊
     */
    public void getMyGroup(Message<String> message){
        Message returnMessage = sendMessageToServer(message, GET_GROUP);
        String result = (String) returnMessage.getMessage();
        log.info(result);
    }

    /**
     * 新建群聊
     */
    public void createGroup(Message<String> message){
        Message returnMessage = sendMessageToServer(message, CREATE_GROUP);
        String result = (String) returnMessage.getMessage();
        log.info(result);
    }

    /**
     * 加入群聊
     */
    public void joinGroup(Message message){
        Message returnMessage = sendMessageToServer(message, JOIN_GROUP);
        String result = (String) returnMessage.getMessage();
        log.info(result);
    }

    /**
     * 群聊发信息
     */
    public void sendMessageToGroup(Message message){
        Message returnMessage = sendMessageToServer(message, GROUP_CHAT);
        String result = (String) returnMessage.getMessage();
        log.info(result);
    }

    /**
     * 请求发验证码到自己的邮箱里
     */
    public void getVerifyCode(Message message){
        Message returnMessage = sendMessageToServer(message, GET_CODE);
        String result = (String) returnMessage.getMessage();
        log.info(result);
    }

    private static Message sendMessageToServer(Message message, MessageType messageType) {
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
            Message result = (Message) clientInputStream.readObject();
            return result;
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

    }

}
