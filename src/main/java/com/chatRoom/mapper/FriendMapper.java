package com.chatRoom.mapper;

import com.chatRoom.config.DruidConfig;
import com.chatRoom.domain.ChatRoomUser;
import com.chatRoom.domain.DB.DBMessage;
import com.chatRoom.domain.DB.FriendDto;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author Jinquan_Ou
 * @Description
 * @Date 2023-06-02 16:56
 * @Version 1.0.0
 **/
@Slf4j
public class FriendMapper {

    /**
     * 到数据库中拉取好友列表
     */
    public DBMessage<List<FriendDto>> getFriendListByUsername(String username) {
        //先根据名字得到用户id
        UserMapper userMapper = new UserMapper();
        Integer userId = userMapper.getIdByNameFromUser(username);

        //查关系表，得到对方的信息
        Connection connection = null;
        try {
            connection = DruidConfig.getConnection();
        } catch (SQLException e) {
            log.debug(e.getMessage());
            throw new RuntimeException("系统繁忙，请稍后重试");
        }

        String sql = "select other_id, create_time from friend where uid = ?";
        ArrayList<FriendDto> friendList = new ArrayList<>();

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql);) {
            preparedStatement.setInt(1, userId);
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                int other_id = resultSet.getInt("other_id");
                ChatRoomUser otherUser = userMapper.getUserById(other_id);
                friendList.add(new FriendDto(otherUser.getUsername(),resultSet.getDate("create_time"),otherUser.getStatus()));
            }
        } catch (SQLException e) {
            throw new RuntimeException("数据库繁忙，请稍后重试");
        }finally {
            try {
                connection.close();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        return new DBMessage<>(true,friendList);

//        Connection connection = null;
//        try {
//            connection = DruidConfig.getConnection();
//        } catch (SQLException e) {
//            log.debug(e.getMessage());
//            throw new RuntimeException("系统繁忙，请稍后重试");
//        }
//
//        //先根据名字查到用户的id号
//        String selectIdSql = "select id from user where username = ?";
//        int userId;//预定义用户id号
//
//        try (PreparedStatement preparedStatement = connection.prepareStatement(selectIdSql);) {
//            preparedStatement.setString(1, username);
//            ResultSet resultSet = preparedStatement.executeQuery();
//            if (resultSet.next()) {
//                //有这个用户
//                userId = resultSet.getInt("id");
//            } else {
//                //没这个用户
//                DBMessage<String> falseMessage = new DBMessage<>();
//                falseMessage.setSuccess(false);
//                falseMessage.setContent("用户" + username + "尚未注册");
//                return falseMessage;
//            }
//        } catch (SQLException e) {
//            throw new RuntimeException("数据库繁忙，请稍后重试");
//        }
//
//        //运行到此处已经是查到了用户id
//        String selectFriendSql = "select username, create_time, status from user,friend where user.id = friend.other_id and friend.uid = ?";
//
//        try(PreparedStatement preparedStatement = connection.prepareStatement(selectFriendSql);) {
//            preparedStatement.setInt(1,userId);
//            ResultSet resultSet = preparedStatement.executeQuery();
//
//            //封装返回信息
//            ArrayList<FriendDto> friendList = new ArrayList<>();
//            DBMessage<List<FriendDto>> resultMessage = new DBMessage<>(true,friendList);
//
//            if (!resultSet.next()){
//                //如果没有数据，那么直接返回
//                return resultMessage;
//            }
//            //如果有数据:由于前面已经调用了next()方法，要先加入这个对象到集合中再再次调用next，否则会出现第一个数据读不到的情况
//
//            do {
//                FriendDto friend = new FriendDto(resultSet.getString("username"), resultSet.getDate("create_time"), resultSet.getInt("status"));
//                friendList.add(friend);
//            }
//            while (resultSet.next());
//
//            return resultMessage;
//
//        } catch (SQLException e) {
//            throw new RuntimeException(e);
//        }


    }

    /**
     * 添加好友信息到数据库
     */
    public DBMessage addFriend(String sender, String receiver) {

        //先根据名字得到id
        UserMapper userMapper = new UserMapper();
        Integer senderId = userMapper.getIdByNameFromUser(sender);
        Integer receiverId = userMapper.getIdByNameFromUser(receiver);

        Connection connection = null;
        try {
            connection = DruidConfig.getConnection();
            connection.setAutoCommit(false);
        } catch (SQLException e) {
            log.debug(e.getMessage());
            throw new RuntimeException("系统繁忙，请稍后重试");
        }

        String insertSql = "insert into friend (uid, other_id) values (?,?)";

        try (PreparedStatement preparedStatement = connection.prepareStatement(insertSql);) {
            //这里要执行两次插入操作，因为好友表是双向的
            preparedStatement.setInt(1, senderId);
            preparedStatement.setInt(2, receiverId);
            preparedStatement.executeUpdate();

            preparedStatement.setInt(1, receiverId);
            preparedStatement.setInt(2, senderId);
            preparedStatement.executeUpdate();

            //执行完了，提交事务
            connection.commit();
            //返回结果
            return new DBMessage<>(true, "操作成功");

        } catch (SQLException e) {
            try {
                //有任何异常，回滚事务
                connection.rollback();
            } catch (SQLException ex) {
                throw new RuntimeException("数据库繁忙，请稍后重试");
            }
            return new DBMessage<>(false, "不能重复添加好友");
        }finally {
            try {
                connection.close();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * 删除好友信息
     */
    public DBMessage deleteFriend(String sender, String receiver) {

        Connection connection = null;
        try {
            connection = DruidConfig.getConnection();
            connection.setAutoCommit(false);
        } catch (SQLException e) {
            log.debug(e.getMessage());
            throw new RuntimeException("系统繁忙，请稍后重试");
        }

        //先根据名字得到id
        UserMapper userMapper = new UserMapper();
        Integer senderId = userMapper.getIdByNameFromUser(sender);
        Integer receiverId = userMapper.getIdByNameFromUser(receiver);

        String deleteSql = "delete from friend where uid = ? and other_id = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(deleteSql);) {
            //这里要执行两次删除操作，因为好友表是双向的
            preparedStatement.setInt(1, senderId);
            preparedStatement.setInt(2, receiverId);
            preparedStatement.executeUpdate();

            preparedStatement.setInt(1, receiverId);
            preparedStatement.setInt(2, senderId);
            preparedStatement.executeUpdate();

            //执行完了，提交事务
            connection.commit();
            //返回结果
            return new DBMessage<>(true, "操作成功");

        } catch (SQLException e) {
            try {
                //有任何异常，回滚事务
                connection.rollback();
            } catch (SQLException ex) {
                throw new RuntimeException("数据库繁忙，请稍后重试");
            }
            return new DBMessage<>(false, "不能删除不存在的好友");
        }finally {
            try {
                connection.close();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

    }


}
