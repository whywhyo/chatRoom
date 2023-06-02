package com.chatRoom.config;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.pool.DruidDataSourceFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * @Author Jinquan_Ou
 * @Description
 * @Date 2023-05-31 18:37
 * @Version 1.0.0
 **/
public class DruidConfig {
    private static DataSource dataSource;

    static {
        Properties properties = new Properties();
        try {
            properties.load(DruidConfig.class.getClassLoader().getResourceAsStream("druid.properties"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            dataSource = DruidDataSourceFactory.createDataSource(properties);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

}
