/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.hops.kafka;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import org.codehaus.jackson.annotate.JsonIgnore;

/**
 *
 * @author misdess
 */
@Entity
@Table(name = "schemas", catalog = "hopsworks", schema = "")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "Schemas.findAll", query = "SELECT s FROM Schemas s"),
    @NamedQuery(name = "Schemas.findByName", query = "SELECT s FROM Schemas s WHERE s.schemasPK.name = :name"),
    @NamedQuery(name = "Schemas.findByVersion", query = "SELECT s FROM Schemas s WHERE s.schemasPK.version = :version"),
    @NamedQuery(name = "Schemas.findByContents", query = "SELECT s FROM Schemas s WHERE s.contents = :contents"),
    @NamedQuery(name = "Schemas.findByCreatedOn", query = "SELECT s FROM Schemas s WHERE s.createdOn = :createdOn")})
public class Schemas implements Serializable {

    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected SchemasPK schemasPK;
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
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "schemas")
    private Collection<ProjectTopics> projectTopicsCollection;

    public Schemas() {
    }

    public Schemas(SchemasPK schemasPK) {
        this.schemasPK = schemasPK;
    }

    public Schemas(SchemasPK schemasPK, String contents, Date createdOn) {
        this.schemasPK = schemasPK;
        this.contents = contents;
        this.createdOn = createdOn;
    }

    public Schemas(String name, int version) {
        this.schemasPK = new SchemasPK(name, version);
    }

    public SchemasPK getSchemasPK() {
        return schemasPK;
    }

    public void setSchemasPK(SchemasPK schemasPK) {
        this.schemasPK = schemasPK;
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

    @XmlTransient
    @JsonIgnore
    public Collection<ProjectTopics> getProjectTopicsCollection() {
        return projectTopicsCollection;
    }

    public void setProjectTopicsCollection(Collection<ProjectTopics> projectTopicsCollection) {
        this.projectTopicsCollection = projectTopicsCollection;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (schemasPK != null ? schemasPK.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Schemas)) {
            return false;
        }
        Schemas other = (Schemas) object;
        if ((this.schemasPK == null && other.schemasPK != null) || (this.schemasPK != null && !this.schemasPK.equals(other.schemasPK))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "io.hops.kafka.Schemas[ schemasPK=" + schemasPK + " ]";
    }
    
}
