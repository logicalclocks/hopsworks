/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.hops.kafka;

/**
 *
 * @author misdess
 */
public class AclDTO {
    
    String topic_Name;
    String user_id;
    String permission_type;
    String operation_type;
    String host;
    String role;
    String shared;

    public String getHost() {
        return host;
    }

    public String getOperation_type() {
        return operation_type;
    }

    public String getPermission_type() {
        return permission_type;
    }

    public String getRole() {
        return role;
    }

    public String getShared() {
        return shared;
    }

    public String getTopic_Name() {
        return topic_Name;
    }

    public String getUser_id() {
        return user_id;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setOperation_type(String operation_type) {
        this.operation_type = operation_type;
    }

    public void setPermission_type(String permission_type) {
        this.permission_type = permission_type;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public void setShared(String shared) {
        this.shared = shared;
    }

    public void setTopic_Name(String topic_Name) {
        this.topic_Name = topic_Name;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }
}
