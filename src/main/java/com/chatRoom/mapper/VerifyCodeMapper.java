package com.chatRoom.mapper;

import com.chatRoom.config.DruidConfig;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @Author Jinquan_Ou
 * @Description
 * @Date 2023-06-03 19:37
 * @Version 1.0.0
 **/
@Slf4j
public class VerifyCodeMapper {

    /**
     * 插入用户-验证码对应信息
     */
    public void saveInfo(String username, String code){
        Connection connection = null;
        try {
            connection = DruidConfig.getConnection();
            connection.setAutoCommit(false);
        } catch (SQLException e) {
            log.debug(e.getMessage());
            throw new RuntimeException("系统繁忙，请稍后重试");
        }

        String sql ="insert into verify (username,code) values (?,?)";
        try(PreparedStatement preparedStatement = connection.prepareStatement(sql);) {
            preparedStatement.setString(1,username);
            preparedStatement.setString(2,code);
            preparedStatement.executeUpdate();
            connection.commit();
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
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
     * 删除用户-验证码对应信息
     */
    public void deleteInfo(String username){

        Connection connection = null;
        try {
            connection = DruidConfig.getConnection();
            connection.setAutoCommit(false);
        } catch (SQLException e) {
            log.debug(e.getMessage());
            throw new RuntimeException("系统繁忙，请稍后重试");
        }

        String sql ="delete from verify where username = ?";
        try(PreparedStatement preparedStatement = connection.prepareStatement(sql);) {
            preparedStatement.setString(1,username);
            preparedStatement.executeUpdate();
            connection.commit();
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
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
     * 查询用户-验证码对应信息
     */
    public String getRealCode(String username){
        Connection connection = null;
        try {
            connection = DruidConfig.getConnection();
        } catch (SQLException e) {
            log.debug(e.getMessage());
            throw new RuntimeException("系统繁忙，请稍后重试");
        }

        String sql ="select code from verify where username = ?";
        try(PreparedStatement preparedStatement = connection.prepareStatement(sql);) {
            preparedStatement.setString(1,username);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()){
                return resultSet.getString("code");
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
}
