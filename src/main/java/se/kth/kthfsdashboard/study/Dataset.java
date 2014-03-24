/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package se.kth.kthfsdashboard.study;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author roshan
 */
@Entity
@Table(name = "dataset")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "Dataset.findAll", query = "SELECT d FROM Dataset d"),
    @NamedQuery(name = "Dataset.findById", query = "SELECT d FROM Dataset d WHERE d.id = :id"),
    @NamedQuery(name = "Dataset.findByOwner", query = "SELECT d FROM Dataset d WHERE d.owner = :owner"),
    @NamedQuery(name = "Dataset.findByName", query = "SELECT d FROM Dataset d WHERE d.name = :name")})
public class Dataset implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @NotNull
    @Column(name = "id")
    private Integer id;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 128)
    @Column(name = "owner")
    private String owner;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 128)
    @Column(name = "name")
    private String name;

    public Dataset() {
    }

    public Dataset(Integer id) {
        this.id = id;
    }

    public Dataset(Integer id, String owner, String name) {
        this.id = id;
        this.owner = owner;
        this.name = name;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
        if (!(object instanceof Dataset)) {
            return false;
        }
        Dataset other = (Dataset) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "se.kth.kthfsdashboard.study.Dataset[ id=" + id + " ]";
    }
    
}
