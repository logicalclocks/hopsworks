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
    private String username;
    private String permissionType;
    private String operationType;
    private String host;
    private String role;

    public AclDTO() {
    }

    AclDTO(Integer id, String username, String permissionType,
            String operationType, String host, String role) {
        this.id = id;
        this.username = username;
        this.permissionType = permissionType;
        this.operationType = operationType;
        this.host = host;
        this.role = role;
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

    public void setUsername(String username) {
        this.username = username;
    }
}
