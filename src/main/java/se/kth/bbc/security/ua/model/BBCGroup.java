/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.bbc.security.ua.model;

import java.io.Serializable;
import java.util.Collection;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import org.codehaus.jackson.annotate.JsonIgnore;

/**
 *
 * @author Ali Gholami <gholami@pdc.kth.se>
 */
@Entity
@Table(name = "Group")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "BBCGroup.findAll", query = "SELECT b FROM BBCGroup b"),
    @NamedQuery(name = "BBCGroup.findByGid", query = "SELECT b FROM BBCGroup b WHERE b.gid = :gid"),
    @NamedQuery(name = "BBCGroup.findByGroupName", query = "SELECT b FROM BBCGroup b WHERE b.groupName = :groupName"),
    @NamedQuery(name = "BBCGroup.findByGroupDesc", query = "SELECT b FROM BBCGroup b WHERE b.groupDesc = :groupDesc")})
public class BBCGroup implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @NotNull
    @Column(name = "gid")
    private Integer gid;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 20)
    @Column(name = "group_name")
    private String groupName;
    @Size(max = 200)
    @Column(name = "group_desc")
    private String groupDesc;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "bBCGroup")
    private Collection<PeopleGroup> peopleGroupCollection;

    public BBCGroup() {
    }

    public BBCGroup(Integer gid) {
        this.gid = gid;
    }

    public BBCGroup(Integer gid, String groupName) {
        this.gid = gid;
        this.groupName = groupName;
    }

    public Integer getGid() {
        return gid;
    }

    public void setGid(Integer gid) {
        this.gid = gid;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getGroupDesc() {
        return groupDesc;
    }

    public void setGroupDesc(String groupDesc) {
        this.groupDesc = groupDesc;
    }

    @XmlTransient
    @JsonIgnore
    public Collection<PeopleGroup> getPeopleGroupCollection() {
        return peopleGroupCollection;
    }

    public void setPeopleGroupCollection(Collection<PeopleGroup> peopleGroupCollection) {
        this.peopleGroupCollection = peopleGroupCollection;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (gid != null ? gid.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof BBCGroup)) {
            return false;
        }
        BBCGroup other = (BBCGroup) object;
        if ((this.gid == null && other.gid != null) || (this.gid != null && !this.gid.equals(other.gid))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "se.kth.bbc.security.ua.model.BBCGroup[ gid=" + gid + " ]";
    }
    
}
