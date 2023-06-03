package com.chatRoom.mapper;

import com.chatRoom.config.DruidConfig;
import com.chatRoom.domain.ChatRoomUser;
import com.chatRoom.domain.DB.DBMessage;
import com.chatRoom.domain.DB.FriendDto;
import com.chatRoom.domain.DB.GroupDto;
import com.chatRoom.utils.EncodeUtil;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author Jinquan_Ou
 * @Description 跟用户相关的操作数据库的操作
 * @Date 2023-06-01 0:04
 * @Version 1.0.0
 **/

@Slf4j
public class UserMapper {

    public DBMessage<String> register(ChatRoomUser user) {

        //先查数据库中是否有这个用户，不允许重复注册
        boolean exist = checkIfExist(user);
        if (exist) {
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

        String sql = "insert into user (username,password,email) values (?,?,?)";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql);) {
            preparedStatement.setString(1, user.getUsername());
            //密码在数据库MD5加密存储
            preparedStatement.setString(2, EncodeUtil.encrypt(user.getPassword()));
            preparedStatement.setString(3, user.getEmail());
            int success = preparedStatement.executeUpdate();
            connection.commit();//没有异常，提交事务

            if (success > 0) {
                log.info("用户{}注册成功", user.getUsername());
                return new DBMessage<>(true, "注册成功");
            } else {
                log.info("用户{}注册失败", user.getUsername());
                return new DBMessage<>(false, "注册失败");
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
        }finally {
            try {
                connection.close();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
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

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql);) {
            preparedStatement.setString(1, user.getUsername());
            ResultSet resultSet = preparedStatement.executeQuery();
            if (!resultSet.next()) {
                return false;
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
        return true;
    }

    public DBMessage<String> login(ChatRoomUser user) {

        Connection connection = null;
        try {
            connection = DruidConfig.getConnection();
        } catch (SQLException e) {
            log.debug(e.getMessage());
            throw new RuntimeException("系统繁忙，请稍后重试");
        }

        String selectSql = "select * from user where username = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(selectSql);) {
            preparedStatement.setString(1, user.getUsername());
            ResultSet resultSet = preparedStatement.executeQuery();
            if (!resultSet.next()) {
                return new DBMessage<>(false, "用户不存在");
            } else {
                String password = resultSet.getString("password");
                //比较密码是否正确
                if (EncodeUtil.pattern(user.getPassword(), password)) {
                    log.info("登录成功");
                    //登录成功之后，将登录状态从0改成1
                    updateStatus(user,1);
                    return new DBMessage<>(true, "登陆成功");
                } else {
                    log.info("登陆失败");
                    return new DBMessage<>(false, "密码错误");
                }
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
    }

    public DBMessage<String> logout(ChatRoomUser user){
        updateStatus(user,0);
        return new DBMessage<>(true,"登出成功");
    }


    /**
     * 修改用户的登录状态
     */
    private void updateStatus(ChatRoomUser user, int status) {
        Connection connection = null;
        try {
            connection = DruidConfig.getConnection();
        } catch (SQLException e) {
            log.debug(e.getMessage());
            throw new RuntimeException("系统繁忙，请稍后重试");
        }

        String updateSql = "update user set status = ? where username = ?";

        try(PreparedStatement preparedStatement = connection.prepareStatement(updateSql);) {
            preparedStatement.setInt(1,status);
            preparedStatement.setString(2,user.getUsername());
            preparedStatement.executeUpdate();
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

    /**
     * 通过名字得到Id
     */
    public Integer getIdByNameFromUser(String name){
        Connection connection = null;
        try {
            connection = DruidConfig.getConnection();
        } catch (SQLException e) {
            log.debug(e.getMessage());
            throw new RuntimeException("系统繁忙，请稍后重试");
        }

        String selectIdSql = "select id from user where username = ?";
        try( PreparedStatement preparedStatement = connection.prepareStatement(selectIdSql);) {
            preparedStatement.setString(1,name);
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

    /**
     * 通过id得到用户对象
     */
    public ChatRoomUser getUserById(Integer userId){
        Connection connection = null;
        try {
            connection = DruidConfig.getConnection();
        } catch (SQLException e) {
            log.debug(e.getMessage());
            throw new RuntimeException("系统繁忙，请稍后重试");
        }

        String sql = "select * from user where id = ?";

        try( PreparedStatement preparedStatement = connection.prepareStatement(sql);) {
            preparedStatement.setInt(1,userId);
            ResultSet resultSet = preparedStatement.executeQuery();
            resultSet.next();
            ChatRoomUser user = new ChatRoomUser();
            user.setUsername(resultSet.getString("username"));
            user.setEmail(resultSet.getString("email"));
            user.setStatus(resultSet.getInt("status"));
            return user;
        } catch (SQLException e) {
            log.debug(e.getMessage());
            throw new RuntimeException("数据库繁忙，请稍后重试");
        }finally {
            try {
                connection.close();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
