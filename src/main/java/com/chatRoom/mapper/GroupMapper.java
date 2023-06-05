package com.chatRoom.mapper;

import com.chatRoom.config.DruidConfig;
import com.chatRoom.domain.ChatRoomUser;
import com.chatRoom.domain.DB.DBMessage;
import com.chatRoom.domain.DB.GroupDto;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author Jinquan_Ou
 * @Description
 * @Date 2023-06-03 13:34
 * @Version 1.0.0
 **/
@Slf4j
public class GroupMapper {

    public DBMessage<String> createGroup(String creator,String groupName){
        UserMapper userMapper = new UserMapper();
        Integer creatorId = userMapper.getIdByNameFromUser(creator);


        Connection connection = null;
        try {
            connection = DruidConfig.getConnection();
            connection.setAutoCommit(false);
        } catch (SQLException e) {
            log.debug(e.getMessage());
            throw new RuntimeException("系统繁忙，请稍后重试");
        }

        String sql = "insert into chat_group (name,master_id) values (?,?)";

        try(PreparedStatement preparedStatement = connection.prepareStatement(sql);) {
            //这里要执行两次插入操作，因为好友表是双向的
            preparedStatement.setString(1,groupName);
            preparedStatement.setInt(2,creatorId);
            preparedStatement.executeUpdate();

            //执行完了，提交事务
            connection.commit();
            //返回结果
            return new DBMessage<String>(true, "新增群聊成功");

        } catch (SQLException e) {
            try {
                //有任何异常，回滚事务
                connection.rollback();
            } catch (SQLException ex) {
                throw new RuntimeException("数据库繁忙，请稍后重试");
            }
            return new DBMessage<String>(false, "数据库繁忙，新增群聊失败");
        }finally {
            try {
                connection.close();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

    }

    public DBMessage<String> joinGroup(String joiner,String groupName){

        //判断用户是否已经在群里了
        List<GroupDto> content = getGroupListByName(joiner).getContent();
        for (GroupDto group : content) {
            if (group.getName().equals(groupName)){
                return new DBMessage<>(false,"你已经在群里了，不能重复添加");
            }
        }

        //得到群聊id
        Integer groupId = getIdByNameFromGroup(groupName);
        //得到加入者的id
        UserMapper userMapper = new UserMapper();
        Integer joinerId = userMapper.getIdByNameFromUser(joiner);

        //开始加入

        Connection connection = null;
        try {
            connection = DruidConfig.getConnection();
            connection.setAutoCommit(false);
        } catch (SQLException e) {
            log.debug(e.getMessage());
            throw new RuntimeException("系统繁忙，请稍后重试");
        }

        String sql = "insert into group_user (uid,gid) values (?,?)";

        try(PreparedStatement preparedStatement = connection.prepareStatement(sql);) {
            preparedStatement.setInt(1,joinerId);
            preparedStatement.setInt(2,groupId);
            preparedStatement.executeUpdate();
            connection.commit();
            return new DBMessage<>(true,"添加群聊成功");
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
            throw new RuntimeException(e);
        }finally {
            try {
                connection.close();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

    }

    /**
     * 通过用户名字得到用户所加入的群聊
     */
    public DBMessage<List<GroupDto>> getGroupListByName(String name){
        //先得到用户的id
        UserMapper userMapper = new UserMapper();
        Integer userId = userMapper.getIdByNameFromUser(name);

        //查中间表
        Connection connection = null;
        try {
            connection = DruidConfig.getConnection();
        } catch (SQLException e) {
            log.debug(e.getMessage());
            throw new RuntimeException("系统繁忙，请稍后重试");
        }
        String sql = "select gid from group_user where uid = ?";

        ArrayList<Integer> groupIdList = new ArrayList<>();
        try(PreparedStatement preparedStatement = connection.prepareStatement(sql);) {
            preparedStatement.setInt(1,userId);
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()){
                groupIdList.add(resultSet.getInt("gid"));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }finally {
            try {
                connection.close();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        if (groupIdList.size()>0) {

            ArrayList<GroupDto> groupList = new ArrayList<>();
            //如果用户有群聊
//            List<GroupDto> groupList = groupIdList.stream().map(this::getGroupById).collect(Collectors.toList());
            for (Integer id : groupIdList) {
                GroupDto group = getGroupById(id);
                groupList.add(group);
            }

            return new DBMessage<>(true, groupList);
        }else {
            //如果用户没有群聊
            return new DBMessage<>(true, new ArrayList<>());
        }

    }

    /**
     * 通过群聊名字得到群里的所有用户
     */
    public DBMessage<List<ChatRoomUser>> getUserByGroupName(String groupName){
        //先查到群id
        Integer groupId = getIdByNameFromGroup(groupName);

        //查中间表
        Connection connection = null;
        try {
            connection = DruidConfig.getConnection();
        } catch (SQLException e) {
            log.debug(e.getMessage());
            throw new RuntimeException("系统繁忙，请稍后重试");
        }
        String sql = "select uid from group_user where gid = ?";
        ArrayList<Integer> userIdList = new ArrayList<>();//用户id列表
        try(PreparedStatement preparedStatement = connection.prepareStatement(sql);) {
            preparedStatement.setInt(1,groupId);
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                userIdList.add(resultSet.getInt("uid"));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }finally {
            try {
                connection.close();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        UserMapper userMapper = new UserMapper();
        List<ChatRoomUser> userList = userIdList.stream().map(userMapper::getUserById).collect(Collectors.toList());

        return new DBMessage<>(true, userList);
    }

    public Integer getIdByNameFromGroup(String groupName){
        Connection connection = null;
        try {
            connection = DruidConfig.getConnection();
        } catch (SQLException e) {
            log.debug(e.getMessage());
            throw new RuntimeException("系统繁忙，请稍后重试");
        }

        String selectIdSql = "select id from chat_group where name = ?";
        try( PreparedStatement preparedStatement = connection.prepareStatement(selectIdSql);) {
            preparedStatement.setString(1,groupName);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()){
                return resultSet.getInt("id");
            }else {
                return null;
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
    }


    public GroupDto getGroupById(Integer groupId){
        Connection connection = null;
        try {
            connection = DruidConfig.getConnection();
        } catch (SQLException e) {
            log.debug(e.getMessage());
            throw new RuntimeException("系统繁忙，请稍后重试");
        }
        String sql = "select * from chat_group where id = ?";

        try( PreparedStatement preparedStatement = connection.prepareStatement(sql);) {
            preparedStatement.setInt(1,groupId);
            ResultSet resultSet = preparedStatement.executeQuery();
            resultSet.next();

            //查到群主的名字
            int masterId = resultSet.getInt("master_id");
            UserMapper userMapper = new UserMapper();
            ChatRoomUser master = userMapper.getUserById(masterId);

            return new GroupDto(resultSet.getString("name"), resultSet.getDate("create_time"), master.getUsername());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }finally {
            try {
                connection.close();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

    }
}
