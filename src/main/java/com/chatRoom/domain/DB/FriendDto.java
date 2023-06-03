package com.chatRoom.domain.DB;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;
import java.util.Date;

/**
 * @Author Jinquan_Ou
 * @Description 接收好友信息的类
 * @Date 2023-06-02 16:10
 * @Version 1.0.0
 **/
@Data
@ToString
public class FriendDto implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 好友的名字
     */
    private String username;

    /**
     * 成为好友的时间
     */
    private Date createTime;

    /**
     * 该好友是否在线
     */
    private Integer flag;

    public FriendDto(String username, Date createTime, Integer flag) {
        this.username = username;
        this.createTime = createTime;
        this.flag = flag;
    }
}
