/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.hops.kafka;

import java.io.Serializable;
import java.util.Collection;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import org.codehaus.jackson.annotate.JsonIgnore;
import se.kth.bbc.project.Project;

/**
 *
 * @author misdess
 */
@Entity
@Table(name = "project_topics", catalog = "hopsworks", schema = "")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "ProjectTopics.findAll", query = "SELECT p FROM ProjectTopics p"),
    @NamedQuery(name = "ProjectTopics.findById", query = "SELECT p FROM ProjectTopics p WHERE p.id = :id"),
    @NamedQuery(name = "ProjectTopics.findByTopicName", query = "SELECT p FROM ProjectTopics p WHERE p.topicName = :topicName"),
    @NamedQuery(name = "ProjectTopics.findByOwner", query = "SELECT p FROM ProjectTopics p WHERE p.owner = :owner"),
    @NamedQuery(name = "ProjectTopics.findByProject", query = "SELECT p FROM ProjectTopics p WHERE p.project_id = :project_id")})
public class ProjectTopics implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "topic_name")
    private String topicName;
    @Basic(optional = false)
    @NotNull
    @Column(name = "owner")
    private boolean owner;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "projectTopics")
    private Collection<TopicAcls> topicAclsCollection;
    @JoinColumn(name = "project_id", referencedColumnName = "id")
    @ManyToOne(optional = false)
    private Project projectId;

    public ProjectTopics() {
    }

    public ProjectTopics(Integer id) {
        this.id = id;
    }
 
    public ProjectTopics(String topicName, Integer id, boolean owner) {
        this.topicName = topicName;
        this.id = id;
        this.owner = owner;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTopicName() {
        return topicName;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    public boolean getOwner() {
        return owner;
    }

    public void setOwner(boolean owner) {
        this.owner = owner;
    }

    @XmlTransient
    @JsonIgnore
    public Collection<TopicAcls> getTopicAclsCollection() {
        return topicAclsCollection;
    }

    public void setTopicAclsCollection(Collection<TopicAcls> topicAclsCollection) {
        this.topicAclsCollection = topicAclsCollection;
    }

    public Project getProjectId() {
        return projectId;
    }

    public void setProjectId(Project projectId) {
        this.projectId = projectId;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof ProjectTopics)) {
            return false;
        }
        ProjectTopics other = (ProjectTopics) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "io.hops.kafka.ProjectTopics[ id=" + id + " ]";
    }
    
}
