package com.chatRoom.domain;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

/**
 * @Author Jinquan_Ou
 * @Description 用户类
 * @Date 2023-05-30 16:37
 * @Version 1.0.0
 **/
@Data
@ToString
public class ChatRoomUser implements Serializable {
    private static final long serialVersionUID = 1L;

    private String username;

    private String password;

    public ChatRoomUser() {
    }

    public ChatRoomUser(String username, String password) {
        this.username = username;
        this.password = password;
    }
}
