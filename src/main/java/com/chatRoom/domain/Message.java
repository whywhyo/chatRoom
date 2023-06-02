package com.chatRoom.domain;

import com.chatRoom.constant.MessageType;
import lombok.Data;

import java.io.Serializable;

/**
 * @Author Jinquan_Ou
 * @Description C/S传输的消息对象
 * @Date 2023-05-30 16:13
 * @Version 1.0.0
 **/
@Data
public class Message <T> implements Serializable {
    private static final long serialVersionUID = 1L;

    //发送端
    private String sender;
    //接收端
    private String receiver;
    //发送的信息
    private T message;
    //消息的类型
    private MessageType messageType;


}
