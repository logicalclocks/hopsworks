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
    
    private int id;
    private String topicName;
    private String username;
    private String permissionType;
    private String operationType;
    private String host;
    private String role;
    private String shared;

    public AclDTO() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
    
    public String getHost() {
        return host;
    }

    public String getOperationType() {
        return operationType;
    }

    public String getPermissionType() {
        return permissionType;
    }

    public String getRole() {
        return role;
    }

    public String getShared() {
        return shared;
    }

    public String getTopicName() {
        return topicName;
    }

    public String getUsername() {
        return username;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    public void setPermissionType(String permissionType) {
        this.permissionType = permissionType;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public void setShared(String shared) {
        this.shared = shared;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
