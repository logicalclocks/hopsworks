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
public class ProjectTopicsPK implements Serializable {

    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "project_id")
    private Integer id;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "topic_name")
    private String topicName;

    public ProjectTopicsPK() {
    }

    public ProjectTopicsPK(String topicName, Integer id) {
        this.topicName = topicName;
        this.id =id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getId() {
        return id;
    }

    public String getTopicName() {
        return topicName;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

     
    
    @Override
    public int hashCode() {
        int hash = 0;
        hash += (int) id;
        hash += (topicName != null ? topicName.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof ProjectTopicsPK)) {
            return false;
        }
        ProjectTopicsPK other = (ProjectTopicsPK) object;
        if (this.id != other.id) {
            return false;
        }
        if ((this.topicName == null && other.topicName != null) || (this.topicName != null && !this.topicName.equals(other.topicName))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "io.hops.kafka.TopicAclsPK[ topicName=" + topicName + ", projectId=" + id  + " ]";
    }
    
}
