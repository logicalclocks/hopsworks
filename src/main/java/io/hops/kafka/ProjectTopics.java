/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.hops.kafka;

import java.io.Serializable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author misdess
 */
@Entity
@Table(name = "project_topics", catalog = "hopsworks", schema = "")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "ProjectTopics.findAll", query = "SELECT p FROM ProjectTopics p"),
    @NamedQuery(name = "ProjectTopics.findByTopicName", query = "SELECT p FROM ProjectTopics p WHERE p.projectTopicsPK.topicName = :topicName"),
    @NamedQuery(name = "ProjectTopics.findByProjectId", query = "SELECT p FROM ProjectTopics p WHERE p.projectTopicsPK.projectId = :projectId")})
public class ProjectTopics implements Serializable {

    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected ProjectTopicsPK projectTopicsPK;

    public ProjectTopics() {
    }

    public ProjectTopics(ProjectTopicsPK projectTopicsPK) {
        this.projectTopicsPK = projectTopicsPK;
    }

    public ProjectTopics(String topicName, int projectId) {
        this.projectTopicsPK = new ProjectTopicsPK(topicName, projectId);
    }

    public ProjectTopicsPK getProjectTopicsPK() {
        return projectTopicsPK;
    }

    public void setProjectTopicsPK(ProjectTopicsPK projectTopicsPK) {
        this.projectTopicsPK = projectTopicsPK;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (projectTopicsPK != null ? projectTopicsPK.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof ProjectTopics)) {
            return false;
        }
        ProjectTopics other = (ProjectTopics) object;
        if ((this.projectTopicsPK == null && other.projectTopicsPK != null) || (this.projectTopicsPK != null && !this.projectTopicsPK.equals(other.projectTopicsPK))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "io.hops.kafka.ProjectTopics[ projectTopicsPK=" + projectTopicsPK + " ]";
    }
    
}
