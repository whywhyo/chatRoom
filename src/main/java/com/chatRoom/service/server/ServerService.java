package com.chatRoom.service.server;
import com.chatRoom.constant.ConstantKey;
import com.chatRoom.constant.MessageType;
import com.chatRoom.domain.ChatRoomUser;
import com.chatRoom.domain.DBMessage;
import com.chatRoom.domain.Message;
import com.chatRoom.exception.ChatRoomException;
import com.chatRoom.mapper.UserMapper;
import com.mysql.cj.util.StringUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import static com.chatRoom.constant.ConstantKey.LISTEN_SUFFIX;
import static com.chatRoom.constant.MessageType.SERVER_RESULT;
import static com.chatRoom.constant.SystemConstant.SERVER_PORT;


/**
 * @Author Jinquan_Ou
 * @Description 服务端功能
 * @Date 2023-05-29 23:50
 * @Version 1.0.0
 **/

@Slf4j
public class ServerService {

    private static final ConcurrentHashMap<String, Socket> clientsCollection =new ConcurrentHashMap<>();


    /**
     * 开服务器方法
     */
    public void openServer() {
        try {
            ServerSocket serverSocket  = new ServerSocket(SERVER_PORT);
            while (true){
                log.info("服务端正在监听......");
                Socket clientSocket = serverSocket.accept();
                log.info("来自{}的客户端成功连接上服务器",clientSocket.getInetAddress().getHostAddress()+clientSocket.getPort());

                //服务端开一个线程响应客户端
                Thread clientHandlerThread = new Thread(new ClientHandler(clientSocket));
                clientHandlerThread.start();
            }
        } catch (IOException e) {
            log.error("出现错误:{}",e.getMessage());
            throw new ChatRoomException("系统繁忙");
        }
    }

    /**
     * 处理客户端发过来的信息的类
     */
    @Data
    public static class ClientHandler implements Runnable{

        private Socket clientSocket;

        public ClientHandler(Socket clientSocket){
            this.clientSocket=clientSocket;
        }

        /**
         * 服务端线程执行客户端的要求方法
         */
        @Override
        public void run() {

            ObjectInputStream clientInputStream = null;
            Message message= null;

            try {
                clientInputStream = new ObjectInputStream(clientSocket.getInputStream());
                message = (Message) clientInputStream.readObject();
            } catch (IOException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }


            //判断消息类型
            MessageType messageType = message.getMessageType();
            if (messageType == MessageType.MESSAGE_SEND){
                //如果是发信息给其他用户
                log.info("服务端转发客户端信息");
                sendMessageToOther(message);
            }else if(messageType == MessageType.LOGIN){
                //如果是登录请求
                log.info("服务端处理登录请求");
                loginHandler(clientSocket,message);

            }else if(messageType == MessageType.REGISTER){
                //如果是注册请求
                log.info("服务端处理注册请求");
                registerHandler(message);
            }else if(messageType == MessageType.LOGOUT){
                //如果是登出请求
                log.info("服务端处理登出请求");
                logout(message);
            }
        }

        /**
         * 登出方法
         */
        private void logout(Message message){
            String sender = message.getSender();

            if (!clientsCollection.containsKey(sender+LISTEN_SUFFIX)) {
                //如果用户本身就没有登录，就执行登出
                Message<String> returnMessage = new Message<>();
                returnMessage.setMessageType(SERVER_RESULT);
                returnMessage.setMessage("请先登录");

                sendMessageMethod(clientSocket,returnMessage);
            }else {
                //先把监听信息socket关闭掉
                Socket targetSocket = clientsCollection.get(sender + LISTEN_SUFFIX);
                try {
                    targetSocket.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                //从集合中取出相应的socket，把它关闭
                clientsCollection.remove(sender+LISTEN_SUFFIX);

                //返回信息给客户端
                Message<String> returnMessage = new Message<>();
                returnMessage.setMessageType(SERVER_RESULT);
                returnMessage.setMessage("登出成功");
                sendMessageMethod(clientSocket,returnMessage);
            }
        }


        /**
         * 发送信息的方法
         * @param message 信息对象
         */
        private void sendMessageToOther(Message<String> message) {

            boolean isOnline = clientsCollection.containsKey(message.getReceiver()+LISTEN_SUFFIX);
            if (!isOnline){
                log.error("用户给一个没上线的用户发了信息");
                Message<String> returnMessage = new Message<>();
                returnMessage.setMessageType(SERVER_RESULT);
                returnMessage.setMessage("对方没上线，暂不支持给用户离线发信息");
                sendMessageMethod(clientSocket,returnMessage);
            }else {
                //先从集合中拿到接收方socket
                Socket receiverSocket = clientsCollection.get(message.getReceiver()+LISTEN_SUFFIX);


                //开始发信息
                ObjectOutputStream receiverOutputStream = null;
                try {
                    receiverOutputStream = new ObjectOutputStream(receiverSocket.getOutputStream());
                    receiverOutputStream.writeObject(message);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                //转发完信息后给发送方做一个回复
                Message<String> returnMessage = new Message<>();
                returnMessage.setMessageType(SERVER_RESULT);
                returnMessage.setMessage("服务端已将信息转发出去");

                sendMessageMethod(clientSocket,returnMessage);
            }
        }

        /**
         * 注册的方法
         * @param message 信息对象
         */
        private void registerHandler(Message<HashMap<String,Object>> message) {
            HashMap<String, Object> messageMap = message.getMessage();
            ChatRoomUser userInfo = (ChatRoomUser)messageMap.get(ConstantKey.USER_INFO);
            // TODO: 2023-05-30 操作数据库
            UserMapper userMapper = new UserMapper();
            userMapper.register(userInfo);

            log.info("用户名为{},密码为{} 的用户注册成功了",userInfo.getUsername(),userInfo.getPassword());
        }

        /**
         * 登录的方法
         * @param clientSocket 自己的客户端socket
         * @param message 信息对象
         */
        private void loginHandler(Socket clientSocket,Message<ChatRoomUser> message) {
            ChatRoomUser userInfo = message.getMessage();

            //先检验是否为空
            checkUser(userInfo);

            Message<DBMessage<String>> loginResult = new Message<>();

            //不能允许重复登录
            if (clientsCollection.containsKey(message.getSender()+LISTEN_SUFFIX)) {

                DBMessage<String> result = new DBMessage<>();
                result.setSuccess(false);
                result.setContent("不能重复登录");

                loginResult.setMessageType(SERVER_RESULT);
                loginResult.setMessage(result);

            }else {
                log.info("8787878787用户还没登录");
                // TODO: 2023-05-30 操作数据库
                UserMapper userMapper = new UserMapper();
                DBMessage<String> result = userMapper.login(userInfo);
                if (!result.isSuccess()) {
                    //登录失败
                    loginResult.setMessageType(SERVER_RESULT);
                    loginResult.setMessage(result);
                } else {
                    //登录成功
                    loginResult.setMessageType(SERVER_RESULT);
                    loginResult.setMessage(result);
                    log.info("用户名为{},密码为{} 的用户上线了",userInfo.getUsername(),userInfo.getPassword());
                    //将当前socket加入到集合中
                    clientsCollection.put(userInfo.getUsername()+":listen",clientSocket);
                }
            }
            //发送登录结果给客户端
            sendMessageMethod(clientSocket,loginResult);

        }


        /**
         * 发信息封装的方法
         * @param socket 目标socket
         * @param message 信息对象
         */
        private void sendMessageMethod(Socket socket,Message message){

            ObjectOutputStream receiverOutputStream = null;
            try {
                receiverOutputStream = new ObjectOutputStream(socket.getOutputStream());
                receiverOutputStream.writeObject(message);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

//        try {
//            receiverOutputStream.close();
//            socket.shutdownOutput();
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
        }

        private void checkUser(ChatRoomUser user){
            if(StringUtils.isNullOrEmpty(user.getUsername())){
                throw new RuntimeException("用户名不能为空");
            }

            if(StringUtils.isNullOrEmpty(user.getPassword())){
                throw new RuntimeException("密码不能为空");
            }
        }

    }

}
