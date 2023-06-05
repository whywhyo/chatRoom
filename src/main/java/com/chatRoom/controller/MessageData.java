package com.chatRoom.controller;

import javafx.beans.property.SimpleStringProperty;

public class MessageData {
    private SimpleStringProperty name = new SimpleStringProperty();
    private SimpleStringProperty message = new SimpleStringProperty();
    private SimpleStringProperty flag = new SimpleStringProperty();

    public MessageData(String name, String isOnline, String flag) {
        this.name.set(name);
        this.message.set(isOnline);
        this.flag.set(flag);
    }

    public String getName() {
        return name.get();
    }

    public String getFlag() {
        return flag.get();
    }

    public String getMessage() {
        return message.get();
    }

    public SimpleStringProperty getNameProperty(){return this.name; }

    public SimpleStringProperty getMessageProperty(){return this.message;}

    public SimpleStringProperty getFlagProperty(){return this.flag;}

    public void setName(String name){
        this.name.set(name);
    }

    public void setMessage(String isOnline){
        this.message.set(isOnline);
    }

    public void setFlag(String flag){
        this.flag.set(flag);
    }

}
