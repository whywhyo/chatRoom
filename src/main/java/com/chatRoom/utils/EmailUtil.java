package com.chatRoom.utils;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.util.Properties;
import java.util.Random;

/**
 * @Author Jinquan_Ou
 * @Description
 * @Date 2023-06-03 12:08
 * @Version 1.0.0
 **/
public class EmailUtil {
    public static String sendEmail(String to){
        // 收件人电子邮件地址
//        String to = "2565483668@qq.com";
//
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

            System.out.println("成功发送邮件");
            return String.valueOf(code);
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
