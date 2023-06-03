package com.chatRoom.domain.DB;

import lombok.Data;

import java.util.Date;

/**
 * @Author Jinquan_Ou
 * @Description
 * @Date 2023-06-03 14:02
 * @Version 1.0.0
 **/
@Data
public class GroupDto {
    private String name;
    private Date create_time;
    private String creator;

    public GroupDto() {
    }

    public GroupDto(String name, Date create_time, String creator) {
        this.name = name;
        this.create_time = create_time;
        this.creator = creator;
    }
}
