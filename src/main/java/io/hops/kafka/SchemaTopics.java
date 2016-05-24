/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.hops.kafka;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author misdess
 */
@Entity
@Table(name = "schema_topics", catalog = "hopsworks", schema = "")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "SchemaTopics.findAll", query = "SELECT s FROM SchemaTopics s"),
    @NamedQuery(name = "SchemaTopics.findByName", query = "SELECT s FROM SchemaTopics s WHERE s.schemaTopicsPK.name = :name"),
    @NamedQuery(name = "SchemaTopics.findByVersion", query = "SELECT s FROM SchemaTopics s WHERE s.schemaTopicsPK.version = :version"),
    @NamedQuery(name = "SchemaTopics.findByContents", query = "SELECT s FROM SchemaTopics s WHERE s.contents = :contents"),
    @NamedQuery(name = "SchemaTopics.findByCreatedOn", query = "SELECT s FROM SchemaTopics s WHERE s.createdOn = :createdOn")})
public class SchemaTopics implements Serializable {

    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected SchemaTopicsPK schemaTopicsPK;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 10000)
    @Column(name = "contents")
    private String contents;
    @Basic(optional = false)
    @NotNull
    @Column(name = "created_on")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdOn;

    public SchemaTopics() {
    }

    public SchemaTopics(String name, int version, String contents, Date createdOn) {
        this.schemaTopicsPK = new SchemaTopicsPK(name, version);
        this.contents = contents;
        this.createdOn = createdOn;
    }

    public SchemaTopics(String name, int version) {
        this.schemaTopicsPK = new SchemaTopicsPK(name, version);
    }

    public SchemaTopicsPK getSchemaTopicsPK() {
        return schemaTopicsPK;
    }

    public void setSchemaTopicsPK(SchemaTopicsPK schemaTopicsPK) {
        this.schemaTopicsPK = schemaTopicsPK;
    }

    public String getContents() {
        return contents;
    }

    public void setContents(String contents) {
        this.contents = contents;
    }

    public Date getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(Date createdOn) {
        this.createdOn = createdOn;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (schemaTopicsPK != null ? schemaTopicsPK.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof SchemaTopics)) {
            return false;
        }
        SchemaTopics other = (SchemaTopics) object;
        if ((this.schemaTopicsPK == null && other.schemaTopicsPK != null) || (this.schemaTopicsPK != null && !this.schemaTopicsPK.equals(other.schemaTopicsPK))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "io.hops.kafka.SchemaTopics[ schemaTopicsPK=" + schemaTopicsPK + " ]";
    }
    
}
