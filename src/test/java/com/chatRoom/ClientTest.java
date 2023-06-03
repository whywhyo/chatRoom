package com.chatRoom;

import com.alibaba.fastjson2.JSON;
import com.chatRoom.config.DruidConfig;
import com.chatRoom.constant.MessageType;
import com.chatRoom.domain.ChatRoomUser;
import com.chatRoom.domain.DB.DBMessage;
import com.chatRoom.domain.DB.FriendDto;
import com.chatRoom.domain.Message;
import com.chatRoom.mapper.FriendMapper;
import com.chatRoom.mapper.UserMapper;
import com.chatRoom.service.client.ClientService;
import com.chatRoom.service.server.ServerService;
import com.chatRoom.utils.EncodeUtil;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.net.Socket;
import java.sql.*;
import java.util.List;
import java.util.Properties;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;
import java.util.Random;

import static com.chatRoom.constant.MessageType.*;

/**
 * @Author Jinquan_Ou
 * @Description
 * @Date 2023-05-30 19:14
 * @Version 1.0.0
 **/
public class ClientTest {

    @Test
    public void testServer() {
        ServerService serverService = new ServerService();
        serverService.openServer();
    }

    @Test
    public void client1() {
        ClientService clientService = new ClientService();
        ChatRoomUser user = new ChatRoomUser("小明", "123456");
        Message<ChatRoomUser> message = new Message<>();
        message.setSender("小明");
        message.setMessage(user);
        clientService.loginRequest(message);
    }

    @Test
    public void client2() {
        ClientService clientService = new ClientService();
        ChatRoomUser user = new ChatRoomUser("小红", "123456");

        Message<ChatRoomUser> message = new Message<>();
        message.setSender("小红");
        message.setMessage(user);
        clientService.loginRequest(message);
    }

    @Test
    public void client3() {
        ClientService clientService = new ClientService();
        ChatRoomUser user = new ChatRoomUser("小强", "123456");
        Message<ChatRoomUser> message = new Message<>();
        message.setMessage(user);
        clientService.loginRequest(message);
    }

    @Test
    public void testLogout(){
        ClientService clientService = new ClientService();
        Message<Object> message = new Message<>();
        message.setMessageType(LOGOUT);
        message.setSender("小红");
        clientService.logoutRequest(message);
    }


    @Test
    public void sendMessageTest() {
        Message<String> message = new Message<>();
        message.setSender("小明");
        message.setReceiver("小强");
        message.setMessage("你好，明天一起吃饭吧");

        ClientService clientService = new ClientService();

        clientService.sendMessage(message);
    }

    @Test
    public void sendMessageTest2() {
        Message<String> message = new Message<>();
        message.setSender("小强");
        message.setReceiver("小明");
        message.setMessage("好的没问题，明天见");

        ClientService clientService = new ClientService();

        clientService.sendMessage(message);
    }

    @Test
    public void testOnline(){
        Connection connection = null;
        try {
            connection = DruidConfig.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        String sql = "select socket from online where name = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql);){
            preparedStatement.setString(1,"小王"+":listen");
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()){
                String socketJson = resultSet.getString("socket");
                System.out.println("从数据库中读到了json："+socketJson);
            }
            //转换成socket对象
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testJDBC() {
        Connection connection = null;
        try {
            connection = DruidConfig.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        String sql = "select * from user";

        try (Statement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery(sql);
        ) {
            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                String username = resultSet.getString("username");
                String password = resultSet.getString("password");
                System.out.println("id:" + id + "  username:" + username + "  password" + password);
            }

        } catch (SQLException e) {
            System.out.println("处理数据库异常");
        }
    }

    @Test
    public void getNowThread(){
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        ThreadInfo[] threadInfos = threadMXBean.dumpAllThreads(true, true);

        for (ThreadInfo threadInfo : threadInfos) {
            System.out.println("Thread ID: " + threadInfo.getThreadId());
            System.out.println("Thread Name: " + threadInfo.getThreadName());
            System.out.println("Thread State: " + threadInfo.getThreadState());
            // 其他线程信息
            System.out.println("-------------------------");
        }
    }

    @Test
    public void testEncodeMD5(){
        System.out.println(EncodeUtil.encrypt("123456"));
        System.out.println(EncodeUtil.pattern("123456", "e10adc3949ba59abbe56e057f20f883e"));
    }

    @Test
    public void testRegister(){
        ClientService clientService = new ClientService();
        ChatRoomUser user = new ChatRoomUser("小唐", "123456","2565483668@qq.com","456812");
        Message<ChatRoomUser> message = new Message<>();
        message.setMessageType(REGISTER);
        message.setMessage(user);
        clientService.registerRequest(message);
    }

    @Test
    public void testGetVerifyCode(){
        ClientService clientService = new ClientService();
        Message<String> message = new Message<>();
        message.setSender("小唐");
        message.setMessage("2565483668@qq.com");
        clientService.getVerifyCode(message);
    }

    @Test
    public void testFriendList(){
        ClientService clientService = new ClientService();
        Message<Object> message = new Message<>();
        message.setSender("小明");
        message.setMessageType(GET_FRIEND);
        clientService.getFriendList(message);
    }

    @Test
    public void testSendFriendInvite(){
        ClientService clientService = new ClientService();
        Message<Object> message = new Message<>();
        message.setSender("小明");
        message.setReceiver("小强");
        message.setMessageType(FRIEND_INVITE);
        clientService.sendFriendInvite(message);
    }

    @Test
    public void testAddFriend(){
        ClientService clientService = new ClientService();
        Message<Boolean> message = new Message<>();
        message.setMessage(true);
        message.setSender("小强");
        message.setReceiver("小明");
        clientService.ackFriendInvite(message);
    }

    @Test
    public void testDeleteFriend(){
        ClientService clientService = new ClientService();
        Message<Boolean> message = new Message<>();
        message.setSender("小强");
        message.setReceiver("小明");
        clientService.deleteFriend(message);
    }

    @Test
    public void testSendEmail(){
        // 收件人电子邮件地址
        String to = "2565483668@qq.com";

        // 发件人电子邮件地址
        String from = "1511349576@qq.com";

        // 发件人电子邮件密码
        String password = "cjjijhdwvjyviefc";

        // SMTP服务器地址
        String host = "smtp.qq.com";

        // 创建Properties对象，用于配置邮件服务器
        Properties properties = new Properties();
        properties.put("mail.smtp.host", host);
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.port", "465");
        properties.put("mail.smtp.socketFactory.port", "465");
        properties.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");

        // 创建Session对象，用于与邮件服务器进行通信
        Session session = Session.getInstance(properties, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(from, password);
            }
        });

        try {
            // 创建MimeMessage对象
            MimeMessage message = new MimeMessage(session);

            // 设置发件人
            message.setFrom(new InternetAddress(from,"小小聊天室管理员"));

            // 设置收件人
            message.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(to));

            // 设置邮件主题
            message.setSubject("聊天室登录验证");

            //设置一个随机数作为验证码
            Random random = new Random();
            int code = random.nextInt(900000) + 100000; // 生成100000到999999之间的随机数
            System.out.println("验证码: " + code);

            // 设置邮件正文
            message.setText("你的验证码为: "+code+" 请注意查收");

            // 发送邮件
            Transport.send(message);

            System.out.println("Email sent successfully!");
        } catch (MessagingException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testCreateGroup(){
        ClientService clientService = new ClientService();
        Message<String> message = new Message<>();
        message.setSender("小明");
        message.setMessage("小明创建的群聊");
        clientService.createGroup(message);
    }

    @Test
    public void testJoinGroup(){
        ClientService clientService = new ClientService();
        Message<String> message = new Message<>();
        message.setSender("小强");
        message.setMessage("小明创建的群聊");
        clientService.joinGroup(message);
    }

    @Test
    public void testSendMessageToGroup(){
        ClientService clientService = new ClientService();
        Message<String> message = new Message<>();
        message.setSender("小明");
        message.setMessage("今晚大家一起去吃饭吧");
        message.setReceiver("小明创建的群聊");
        clientService.sendMessageToGroup(message);
    }




}
