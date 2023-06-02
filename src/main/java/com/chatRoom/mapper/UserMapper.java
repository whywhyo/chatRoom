package com.chatRoom.mapper;

import com.chatRoom.config.DruidConfig;
import com.chatRoom.domain.ChatRoomUser;
import com.chatRoom.domain.DBMessage;
import com.chatRoom.utils.EncodeUtil;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @Author Jinquan_Ou
 * @Description 跟用户相关的操作数据库的操作
 * @Date 2023-06-01 0:04
 * @Version 1.0.0
 **/

@Slf4j
public class UserMapper {

    public DBMessage<String> register(ChatRoomUser user){

        //先查数据库中是否有这个用户，不允许重复注册
        boolean exist = checkIfExist(user);
        if(exist){
            log.info("用户{}执行重复注册操作", user.getUsername());
            return new DBMessage<>(false, "用户已存在");
        }

        //注册用户
        Connection connection = null;
        try {
            connection = DruidConfig.getConnection();
            connection.setAutoCommit(false);//手动提交事务
        } catch (SQLException e) {
            log.debug(e.getMessage());
            throw new RuntimeException("系统繁忙，请稍后重试");
        }

        String sql = "insert into user values (null,?,?)";

        try(PreparedStatement preparedStatement = connection.prepareStatement(sql);) {
            preparedStatement.setString(1, user.getUsername());
            //密码在数据库MD5加密存储
            preparedStatement.setString(2, EncodeUtil.encrypt(user.getPassword()));
            int success = preparedStatement.executeUpdate();
            connection.commit();//没有异常，提交事务

            if (success>0){
                log.info("用户{}注册成功",user.getUsername());
                return new DBMessage<>(true,"注册成功");
            }else {
                log.info("用户{}注册失败",user.getUsername());
                return new DBMessage<>(false,"注册失败");
            }

        } catch (SQLException e) {
            try {
                //如果发生了错误需要回滚事务
                log.debug("注册用户时发生了异常，事务回滚");
                connection.rollback();
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
            throw new RuntimeException(e);
        }

    }

    /**
     * 检查用户是否已经存在于数据库
     */
    private boolean checkIfExist(ChatRoomUser user) {
        Connection connection = null;
        try {
            connection = DruidConfig.getConnection();
        } catch (SQLException e) {
            log.debug(e.getMessage());
            throw new RuntimeException("系统繁忙，请稍后重试");
        }
        String sql = "select * from user where username = ?";

        try(PreparedStatement preparedStatement = connection.prepareStatement(sql);) {
            preparedStatement.setString(1, user.getUsername());
            ResultSet resultSet = preparedStatement.executeQuery();
            if (!resultSet.next()) {
                return false;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return true;
    }

    public DBMessage<String> login(ChatRoomUser user){

        Connection connection = null;
        try {
            connection = DruidConfig.getConnection();
        } catch (SQLException e) {
            log.debug(e.getMessage());
            throw new RuntimeException("系统繁忙，请稍后重试");
        }

        String selectSql = "select * from user where username = ?";
        try(PreparedStatement preparedStatement = connection.prepareStatement(selectSql);) {
            preparedStatement.setString(1, user.getUsername());
            ResultSet resultSet = preparedStatement.executeQuery();
            if (!resultSet.next()){
                return new DBMessage<>(false,"用户不存在");
            }else {
                String password = resultSet.getString("password");
                //比较密码是否正确
                if (EncodeUtil.pattern(user.getPassword(),password)){
                    log.info("登录成功");
                    return new DBMessage<>(true,"登陆成功");
                }else {
                    log.info("登陆失败");
                    return new DBMessage<>(false,"密码错误");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
