/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.hops.kafka;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author misdess
 */
@Entity
@Table(name = "topic_acls", catalog = "hopsworks", schema = "")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "TopicAcls.findAll", query = "SELECT t FROM TopicAcls t"),
    @NamedQuery(name = "TopicAcls.findByTopicId", query = "SELECT t FROM TopicAcls t WHERE t.topicAclsPK.topicId = :topicId"),
    @NamedQuery(name = "TopicAcls.findByUserId", query = "SELECT t FROM TopicAcls t WHERE t.topicAclsPK.userId = :userId"),
    @NamedQuery(name = "TopicAcls.findByPermissionType", query = "SELECT t FROM TopicAcls t WHERE t.topicAclsPK.permissionType = :permissionType"),
    @NamedQuery(name = "TopicAcls.findByOperationType", query = "SELECT t FROM TopicAcls t WHERE t.topicAclsPK.operationType = :operationType"),
    @NamedQuery(name = "TopicAcls.findByHost", query = "SELECT t FROM TopicAcls t WHERE t.topicAclsPK.host = :host"),
    @NamedQuery(name = "TopicAcls.findByRole", query = "SELECT t FROM TopicAcls t WHERE t.topicAclsPK.role = :role"),
    @NamedQuery(name = "TopicAcls.findByNotShared", query = "SELECT t FROM TopicAcls t WHERE t.shared != :shared")})
public class TopicAcls implements Serializable {

    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected TopicAclsPK topicAclsPK;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 25)
    @Column(name = "shared")
    private String shared;
    @JoinColumn(name = "topic_id", referencedColumnName = "id", insertable = false, updatable = false)
    @ManyToOne(optional = false)
    private ProjectTopics projectTopics;

    public TopicAcls() {
    }

    public TopicAcls(TopicAclsPK topicAclsPK) {
        this.topicAclsPK = topicAclsPK;
    }

    public TopicAcls(TopicAclsPK topicAclsPK, String shared) {
        this.topicAclsPK = topicAclsPK;
        this.shared = shared;
    }

    public TopicAcls(int topicId, String userId, String permissionType,
            String operationType, String host, String role, String shared) {
        this.topicAclsPK = new TopicAclsPK(topicId, userId, permissionType,
                operationType, host, role);
        this.shared =shared;
    }

    public TopicAclsPK getTopicAclsPK() {
        return topicAclsPK;
    }

    public void setTopicAclsPK(TopicAclsPK topicAclsPK) {
        this.topicAclsPK = topicAclsPK;
    }

    public String getShared() {
        return shared;
    }

    public void setShared(String shared) {
        this.shared = shared;
    }

    public ProjectTopics getProjectTopics() {
        return projectTopics;
    }

    public void setProjectTopics(ProjectTopics projectTopics) {
        this.projectTopics = projectTopics;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (topicAclsPK != null ? topicAclsPK.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof TopicAcls)) {
            return false;
        }
        TopicAcls other = (TopicAcls) object;
        if ((this.topicAclsPK == null && other.topicAclsPK != null) || (this.topicAclsPK != null && !this.topicAclsPK.equals(other.topicAclsPK))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "io.hops.kafka.TopicAcls[ topicAclsPK=" + topicAclsPK + " ]";
    }
    
}
