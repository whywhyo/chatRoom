package com.chatRoom;

import com.alibaba.fastjson2.JSON;
import com.chatRoom.config.DruidConfig;
import com.chatRoom.constant.MessageType;
import com.chatRoom.domain.ChatRoomUser;
import com.chatRoom.domain.Message;
import com.chatRoom.mapper.UserMapper;
import com.chatRoom.service.client.ClientService;
import com.chatRoom.service.server.ServerService;
import com.chatRoom.utils.EncodeUtil;
import org.junit.Test;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.net.Socket;
import java.sql.*;

import static com.chatRoom.constant.MessageType.LOGIN;
import static com.chatRoom.constant.MessageType.LOGOUT;

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
        ChatRoomUser user = new ChatRoomUser("小明", "123456");

        Message<ChatRoomUser> message = new Message<>();
        message.setSender("小明");
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
        message.setSender("小强");
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
        ChatRoomUser user = new ChatRoomUser("小红", "123456");
        Message<ChatRoomUser> message = new Message<>();

    }

}
