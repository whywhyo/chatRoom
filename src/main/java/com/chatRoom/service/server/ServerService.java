package com.chatRoom.service.server;

import com.chatRoom.constant.ConstantKey;
import com.chatRoom.constant.MessageType;
import com.chatRoom.domain.ChatRoomUser;
import com.chatRoom.domain.DB.DBMessage;
import com.chatRoom.domain.DB.FriendDto;
import com.chatRoom.domain.DB.GroupDto;
import com.chatRoom.domain.Message;
import com.chatRoom.exception.ChatRoomException;
import com.chatRoom.mapper.FriendMapper;
import com.chatRoom.mapper.GroupMapper;
import com.chatRoom.mapper.UserMapper;
import com.chatRoom.mapper.VerifyCodeMapper;
import com.chatRoom.utils.EmailUtil;
import com.mysql.cj.util.StringUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.chatRoom.constant.ConstantKey.LISTEN_SUFFIX;
import static com.chatRoom.constant.MessageType.*;
import static com.chatRoom.constant.SystemConstant.SERVER_PORT;


/**
 * @Author Jinquan_Ou
 * @Description 服务端功能
 * @Date 2023-05-29 23:50
 * @Version 1.0.0
 **/

@Slf4j
public class ServerService {

    private static final ConcurrentHashMap<String, Socket> clientsCollection = new ConcurrentHashMap<>();


    /**
     * 开服务器方法
     */
    public void openServer() {
        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            while (true) {
                log.info("服务端正在监听......");
                Socket clientSocket = serverSocket.accept();
                log.info("来自{}的客户端成功连接上服务器", clientSocket.getInetAddress().getHostAddress() + clientSocket.getPort());

                //服务端开一个线程响应客户端
                Thread clientHandlerThread = new Thread(new ClientHandler(clientSocket));
                clientHandlerThread.start();
            }
        } catch (IOException e) {
            log.error("出现错误:{}", e.getMessage());
            throw new ChatRoomException("系统繁忙");
        }
    }

    /**
     * 处理客户端发过来的信息的类
     */
    @Data
    public static class ClientHandler implements Runnable {

        private Socket clientSocket;

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        /**
         * 服务端线程执行客户端的要求方法
         */
        @Override
        public void run() {

            ObjectInputStream clientInputStream = null;
            Message message = null;

            try {
                clientInputStream = new ObjectInputStream(clientSocket.getInputStream());
                message = (Message) clientInputStream.readObject();
            } catch (IOException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }

            //判断消息类型
            MessageType messageType = message.getMessageType();
            if (messageType == MESSAGE_SEND) {
                //如果是发信息给其他用户
                log.info("服务端转发客户端信息");
                sendMessageToOther(message);
            } else if (messageType == LOGIN) {
                //如果是登录请求
                log.info("服务端处理登录请求");
                loginHandler(clientSocket, message);
            } else if (messageType == REGISTER) {
                //如果是注册请求
                log.info("服务端处理注册请求");
                registerHandler(message);
            } else if (messageType == LOGOUT) {
                //如果是登出请求
                log.info("服务端处理登出请求");
                logout(message);
            } else if (messageType == GET_FRIEND) {
                //如果是获取好友列表请求
                log.info("服务器处理获取好友列表请求");
                getFriendList(message);
            } else if (messageType == FRIEND_INVITE) {
                //如果是请求添加好友的请求
                log.info("服务器处理请求添加好友的请求");
                forwardFriendInvite(message);
            } else if (messageType == ACK_FRIEND_INVITE) {
                //如果是应答好友邀请请求的请求
                log.info("服务器处理应答好友邀请请求的请求");
                ackFriendInvite(message);
            } else if(messageType == DELETE_FRIEND){
                //如果是请求删除好友的请求
                log.info("服务器处理删除好友的请求");
                deleteFriend(message);
            }else if(messageType == CREATE_GROUP){
                //如果是新建群聊的请求
                log.info("服务器处理新建群聊的请求");
                createGroup(message);
            }else if (messageType == JOIN_GROUP){
                //如果是加入群聊的请求
                log.info("服务器处理加入群聊的请求");
                joinGroup(message);
            }else if (messageType == GET_GROUP){
                //如果是请求获取群聊列表的请求
                log.info("服务器处理请求获取群聊列表的请求");
                getGroup(message);
            }else if (messageType == GROUP_CHAT){
                //如果是群聊发信息请求
                log.info("服务器处理群聊发信息的请求");
                sendMessageToGroup(message);
            }else if (messageType == GET_CODE){
                //如果是请求发送验证码到邮箱中
                log.info("服务器处理请求发送验证码的请求");
                getVerifyCode(message);
            }
        }

        private void getVerifyCode(Message<String> message) {
            String username = message.getSender();
            String email = message.getMessage();

            //调用方法发送验证码到邮箱
            String code = EmailUtil.sendEmail(email);

            //将验证码信息保存到数据库，方便验证
            VerifyCodeMapper verifyCodeMapper = new VerifyCodeMapper();
            verifyCodeMapper.saveInfo(username,code);

            //封装发送验证码成功的信息
            Message<String> returnMessage = new Message<>();
            returnMessage.setMessage("发送验证码成功");
            sendMessageMethod(clientSocket,returnMessage);
        }

        private void sendMessageToGroup(Message<String> message){
            String sender = message.getSender();
            String groupName = message.getReceiver();

            //得到群聊里面所有的在线用户
            GroupMapper groupMapper = new GroupMapper();
            DBMessage<List<ChatRoomUser>> dbMessage = groupMapper.getUserByGroupName(groupName);
            List<ChatRoomUser> allUserList = dbMessage.getContent();
            List<ChatRoomUser> onlineUserList = allUserList.stream().filter(user -> user.getStatus() == 1).collect(Collectors.toList());

            //通过这些用户的监听socket发信息
            for (ChatRoomUser user : onlineUserList) {
                Socket receiverSocket = clientsCollection.get(user.getUsername()+LISTEN_SUFFIX);
                sendMessageMethod(receiverSocket,message);
            }

            //返回结果反馈给发送方
            Message<String> returnMessage = new Message<>();
            returnMessage.setMessage("发送群聊信息成功");
            sendMessageMethod(clientSocket,returnMessage);

        }

        private void getGroup(Message<String> message){
            String sender = message.getSender();
            GroupMapper groupMapper = new GroupMapper();
            DBMessage<List<GroupDto>> groupListByName = groupMapper.getGroupListByName(sender);


            //返回给客户端
            Message<List<GroupDto>> returnMessage = new Message<>();
            returnMessage.setMessage(groupListByName.getContent());
            sendMessageMethod(clientSocket,returnMessage);
        }

        private void joinGroup(Message<String> message) {
            //得到申请者名字
            String applicant = message.getSender();
            //得到群聊名字
            String groupName = message.getMessage();

            GroupMapper groupMapper = new GroupMapper();
            DBMessage<String> dbMessage = groupMapper.joinGroup(applicant, groupName);

            //封装返回给客户端的信息
            Message<String> returnMessage = new Message<>();
            returnMessage.setMessage(dbMessage.getContent());

            //返回给客户端
            sendMessageMethod(clientSocket,returnMessage);
        }

        private void createGroup(Message<String> message){

            //得到群聊发起者
            String creator = message.getSender();
            //得到群聊名字
            String groupName = message.getMessage();
            //操作数据库加入
            GroupMapper groupMapper = new GroupMapper();
            DBMessage<String> dbMessage = groupMapper.createGroup(creator, groupName);

            //创建群聊之后要把自己加入到用户群聊关系表中
            groupMapper.joinGroup(creator,groupName);

            //封装返回给客户端的信息
            Message<String> returnMessage = new Message<>();
            returnMessage.setMessage(dbMessage.getContent());

            //返回给客户端
            sendMessageMethod(clientSocket,returnMessage);

        }


        private void deleteFriend(Message message) {
            String sender = message.getSender();
            String receiver = message.getReceiver();

            //发起数据库操作
            FriendMapper friendMapper = new FriendMapper();
            DBMessage dbMessage = friendMapper.deleteFriend(message.getSender(), message.getReceiver());

            //发信息通知客户端
            Message<String> returnMessage = new Message<>();
            returnMessage.setMessage((String) dbMessage.getContent());

            sendMessageMethod(clientSocket,returnMessage);
        }

        private void ackFriendInvite(Message<Boolean> message) {
            //得到接收结果方的线程
            Socket receiverSocket = clientsCollection.get(message.getReceiver()+LISTEN_SUFFIX);

            Message<String> returnMessage = new Message<>();//返回给应答方的数据

            if (message.getMessage()) {
                //如果同意好友申请，要发起数据库操作
                FriendMapper friendMapper = new FriendMapper();
                DBMessage dbMessage = friendMapper.addFriend(message.getSender(), message.getReceiver());
                if (dbMessage.isSuccess()) {
                    returnMessage.setMessage("已同意对方邀请");
                } else {
                    returnMessage.setMessage((String) dbMessage.getContent());
                }
            } else {
                returnMessage.setMessage("已拒绝对方邀请");
            }

            //发信息通知好友邀请申请者
            sendMessageMethod(receiverSocket, message);
            //发信息通知好友申请应答者
            sendMessageMethod(clientSocket, returnMessage);
        }

        /**
         * 转发请求添加好友信息的方法
         */
        private void forwardFriendInvite(Message message) {
            String sender = message.getSender();
            String receiver = message.getReceiver();

            //从集合中找到接收者线程
            Socket receiverSocket = clientsCollection.get(receiver + LISTEN_SUFFIX);
            sendMessageMethod(receiverSocket, message);

            //给发起邀请的客户一个回复
            Message<String> returnMessage = new Message<>();
            returnMessage.setMessage("加好友请求已成功发出，等待对方应答");
            sendMessageMethod(clientSocket, returnMessage);
        }


        /**
         * 获取好友列表请求方法
         */
        private void getFriendList(Message message) {
            //先得到要获取好友列表请求的客户名字
            String clientName = message.getSender();

            //查数据库，获得客户的好友列表
            FriendMapper friendMapper = new FriendMapper();
            DBMessage<List<FriendDto>> dbMessage = friendMapper.getFriendListByUsername(clientName);

            //封装返回给客户端的信息
            Message<List<FriendDto>> resultMessage = new Message<>();
            resultMessage.setMessageType(SERVER_RESULT);
            resultMessage.setMessage(dbMessage.getContent());
            sendMessageMethod(clientSocket, resultMessage);
        }

        /**
         * 登出方法
         */
        private void logout(Message message) {
            String sender = message.getSender();

            if (!clientsCollection.containsKey(sender + LISTEN_SUFFIX)) {
                //如果用户本身就没有登录，就执行登出
                Message<String> returnMessage = new Message<>();
                returnMessage.setMessageType(SERVER_RESULT);
                returnMessage.setMessage("请先登录");

                sendMessageMethod(clientSocket, returnMessage);
            } else {
                //先把监听信息socket关闭掉
                Socket targetSocket = clientsCollection.get(sender + LISTEN_SUFFIX);
                try {
                    targetSocket.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                //从集合中取出相应的socket，把它关闭
                clientsCollection.remove(sender + LISTEN_SUFFIX);

                //将数据库中的状态改掉
                ChatRoomUser user = new ChatRoomUser();
                user.setUsername(sender);
                UserMapper userMapper = new UserMapper();
                userMapper.logout(user);

                //返回信息给客户端
                Message<String> returnMessage = new Message<>();
                returnMessage.setMessageType(SERVER_RESULT);
                returnMessage.setMessage("登出成功");
                sendMessageMethod(clientSocket, returnMessage);
            }
        }


        /**
         * 发送信息的方法
         *
         * @param message 信息对象
         */
        private void sendMessageToOther(Message<String> message) {

            boolean isOnline = clientsCollection.containsKey(message.getReceiver() + LISTEN_SUFFIX);
            if (!isOnline) {
                log.error("用户给一个没上线的用户发了信息");
                Message<String> returnMessage = new Message<>();
                returnMessage.setMessageType(SERVER_RESULT);
                returnMessage.setMessage("对方没上线，暂不支持给用户离线发信息");
                sendMessageMethod(clientSocket, returnMessage);
            } else {
                //先从集合中拿到接收方socket
                Socket receiverSocket = clientsCollection.get(message.getReceiver() + LISTEN_SUFFIX);

                //开始发信息
                ObjectOutputStream receiverOutputStream = null;
                try {
                    receiverOutputStream = new ObjectOutputStream(receiverSocket.getOutputStream());
                    receiverOutputStream.writeObject(message);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                // TODO: 2023-06-02 给接收方的socket发完数据之后把路径存在数据库中


                //转发完信息后给发送方做一个回复
                Message<String> returnMessage = new Message<>();
                returnMessage.setMessageType(SERVER_RESULT);
                returnMessage.setMessage("服务端已将信息转发出去");

                sendMessageMethod(clientSocket, returnMessage);
            }
        }

        /**
         * 注册的方法
         *
         * @param message 信息对象
         */
        private void registerHandler(Message<ChatRoomUser> message) {

            ChatRoomUser userInfo = (ChatRoomUser) message.getMessage();

            //封装返回信息的对象
            Message<String> resultMessage = new Message<>();
            resultMessage.setMessageType(SERVER_RESULT);
//            resultMessage.setMessage(dbMessage.getContent());

            if(StringUtils.isNullOrEmpty(userInfo.getCode())){
                resultMessage.setMessage("请先获取验证码并输入");
            }else {

                //先从数据库中读取验证码
                VerifyCodeMapper verifyCodeMapper = new VerifyCodeMapper();
                String realCode = verifyCodeMapper.getRealCode(userInfo.getUsername());

                if (StringUtils.isNullOrEmpty(realCode) || !(realCode.equals(userInfo.getCode()))){
                    //如果数据库中没有存放对应的数据，或者验证码不匹配，那就返回信息要求用户重新获取验证码
                    resultMessage.setMessage("验证码校验不通过");
                }else {
                    //操作数据库新增用户信息
                    UserMapper userMapper = new UserMapper();
                    DBMessage<String> dbMessage = userMapper.register(userInfo);
                    resultMessage.setMessage(dbMessage.getContent());
                }
                //每次读取完验证码都要删除数据库中对应的数据
                verifyCodeMapper.deleteInfo(userInfo.getUsername());
            }
            sendMessageMethod(clientSocket, resultMessage);
        }

        /**
         * 登录的方法
         *
         * @param clientSocket 自己的客户端socket
         * @param message      信息对象
         */
        private void loginHandler(Socket clientSocket, Message<ChatRoomUser> message) {
            ChatRoomUser userInfo = message.getMessage();

            //先检验是否为空
            checkUser(userInfo);

            Message<DBMessage<String>> loginResult = new Message<>();

            //不能允许重复登录
            if (clientsCollection.containsKey(message.getSender() + LISTEN_SUFFIX)) {

                DBMessage<String> result = new DBMessage<>();
                result.setSuccess(false);
                result.setContent("不能重复登录");

                loginResult.setMessageType(SERVER_RESULT);
                loginResult.setMessage(result);

            } else {
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
                    log.info("用户名为{},密码为{} 的用户上线了", userInfo.getUsername(), userInfo.getPassword());
                    //将当前socket加入到集合中
                    clientsCollection.put(userInfo.getUsername() + ":listen", clientSocket);
                }
            }
            //发送登录结果给客户端
            sendMessageMethod(clientSocket, loginResult);

        }


        /**
         * 发信息封装的方法
         *
         * @param socket  目标socket
         * @param message 信息对象
         */
        private void sendMessageMethod(Socket socket, Message message) {

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

        private void checkUser(ChatRoomUser user) {
            if (StringUtils.isNullOrEmpty(user.getUsername())) {
                throw new RuntimeException("用户名不能为空");
            }

            if (StringUtils.isNullOrEmpty(user.getPassword())) {
                throw new RuntimeException("密码不能为空");
            }
        }

    }

}
