/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.hops.kafka;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 *
 * @author misdess
 */
@Embeddable
public class TopicAclsPK implements Serializable {

    @Basic(optional = false)
    @NotNull
    @Column(name = "topic_id")
    private int topicId;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 1000)
    @Column(name = "user_id")
    private String userId;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "permission_type")
    private String permissionType;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "operation_type")
    private String operationType;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "host")
    private String host;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "role")
    private String role;

    public TopicAclsPK() {
    }

    public TopicAclsPK(int topicId, String userId, String permissionType, String operationType, String host, String role) {
        this.topicId = topicId;
        this.userId = userId;
        this.permissionType = permissionType;
        this.operationType = operationType;
        this.host = host;
        this.role = role;
    }

    public int getTopicId() {
        return topicId;
    }

    public void setTopicId(int topicId) {
        this.topicId = topicId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getPermissionType() {
        return permissionType;
    }

    public void setPermissionType(String permissionType) {
        this.permissionType = permissionType;
    }

    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (int) topicId;
        hash += (userId != null ? userId.hashCode() : 0);
        hash += (permissionType != null ? permissionType.hashCode() : 0);
        hash += (operationType != null ? operationType.hashCode() : 0);
        hash += (host != null ? host.hashCode() : 0);
        hash += (role != null ? role.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof TopicAclsPK)) {
            return false;
        }
        TopicAclsPK other = (TopicAclsPK) object;
        if (this.topicId != other.topicId) {
            return false;
        }
        if ((this.userId == null && other.userId != null) || (this.userId != null && !this.userId.equals(other.userId))) {
            return false;
        }
        if ((this.permissionType == null && other.permissionType != null) || (this.permissionType != null && !this.permissionType.equals(other.permissionType))) {
            return false;
        }
        if ((this.operationType == null && other.operationType != null) || (this.operationType != null && !this.operationType.equals(other.operationType))) {
            return false;
        }
        if ((this.host == null && other.host != null) || (this.host != null && !this.host.equals(other.host))) {
            return false;
        }
        if ((this.role == null && other.role != null) || (this.role != null && !this.role.equals(other.role))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "io.hops.kafka.TopicAclsPK[ topicId=" + topicId + ", userId=" + userId + ", permissionType=" + permissionType + ", operationType=" + operationType + ", host=" + host + ", role=" + role + " ]";
    }
    
}
