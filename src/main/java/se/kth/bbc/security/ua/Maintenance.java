/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.bbc.security.ua;

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

@Entity
@Table(name = "hopsworks.maintenance")
@XmlRootElement
@NamedQueries({
        @NamedQuery(name = "Maintenance.findAll", query = "SELECT m FROM Maintenance m"),
        @NamedQuery(name = "Maintenance.findById", query = "SELECT m FROM Maintenance m WHERE m.id = :id"),
        @NamedQuery(name = "Maintenance.findByStatus", query = "SELECT m FROM Maintenance m WHERE m.status = :status"),
        @NamedQuery(name = "Maintenance.findByMessage", query = "SELECT m FROM Maintenance m WHERE m.message = :message")})
public class Maintenance implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @NotNull
    @Column(name = "id")
    private Short id;
    @Basic(optional = false)
    @NotNull
    @Column(name = "status")
    private short status;
    @Size(max = 254)
    @Column(name = "message")
    private String message;

    public Maintenance() {
    }

    public Maintenance(Short id) {
        this.id = id;
    }

    public Maintenance(Short id, short status) {
        this.id = id;
        this.status = status;
    }

    public Short getId() {
        return id;
    }

    public void setId(Short id) {
        this.id = id;
    }

    public short getStatus() {
        return status;
    }

    public void setStatus(short status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
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
        if (!(object instanceof Maintenance)) {
            return false;
        }
        Maintenance other = (Maintenance) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "se.kth.bbc.security.ua.Maintenance[ id=" + id + " ]";
    }

}
