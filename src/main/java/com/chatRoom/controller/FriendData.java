package com.chatRoom.controller;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;

public class FriendData {
        private SimpleStringProperty name = new SimpleStringProperty();
        private SimpleStringProperty isOnline = new SimpleStringProperty();
        private SimpleStringProperty userOrGroup = new SimpleStringProperty();

        public FriendData(String name, String isOnline, String userOrGroup) {
            this.name.set(name);
            this.isOnline.set(isOnline);
            this.userOrGroup.set(userOrGroup);
        }

        public String getName() {return name.get();}

        public String isOnline() {return isOnline.get();}

        public String getUserOrGroup() {return userOrGroup.get();}

        public SimpleStringProperty getNameProperty(){return this.name; }

        public SimpleStringProperty getIsOnlineProperty(){return this.isOnline;}

        public SimpleStringProperty getUserOrGroupProperty(){return this.userOrGroup;}

        public void setName(String name){this.name.set(name);}

        public void setIsOnline(String isOnline){this.isOnline.set(isOnline);}

        public void setUserOrGroup(String userOrGroup){this.userOrGroup.set(userOrGroup); }
}
