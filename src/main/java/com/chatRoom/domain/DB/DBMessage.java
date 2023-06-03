package com.chatRoom.domain.DB;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author Jinquan_Ou
 * @Description 数据库操作返回的结果封装
 * @Date 2023-06-01 11:59
 * @Version 1.0.0
 **/

@Data
public class DBMessage<T> implements Serializable {
    private static final long serialVersionUID = 1L;
    private boolean success;
    private T content;

    public DBMessage() {
    }

    public DBMessage(boolean success, T content) {
        this.success = success;
        this.content = content;
    }
}
