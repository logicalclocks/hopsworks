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

/**kafkaCtrl.topicIsSharedTo(row.name);kafkaCtrl.getAclsForTopic(row.name);
 *
 * @author misdess
 */
@Embeddable
public class SharedTopicsPK implements Serializable {

    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "topic_name")
    private String topicName;
    @Basic(optional = false)
    @NotNull
    @Column(name = "owner_id")
    private int projectId;

    public SharedTopicsPK() {
    }

    public SharedTopicsPK(String topicName, int projectId) {
        this.topicName = topicName;
        this.projectId = projectId;
    }

    public String getTopicName() {
        return topicName;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    public int getProjectId() {
        return projectId;
    }

    public void setProjectId(int projectId) {
        this.projectId = projectId;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (topicName != null ? topicName.hashCode() : 0);
        hash += (int) projectId;
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof SharedTopicsPK)) {
            return false;
        }
        SharedTopicsPK other = (SharedTopicsPK) object;
        if ((this.topicName == null && other.topicName != null) || (this.topicName != null && !this.topicName.equals(other.topicName))) {
            return false;
        }
        if (this.projectId != other.projectId) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "io.hops.kafka.SharedTopicsPK[ topicName=" + topicName + ", projectId=" + projectId + " ]";
    }
    
}
