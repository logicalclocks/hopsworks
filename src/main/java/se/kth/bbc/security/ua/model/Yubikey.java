/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.bbc.security.ua.model;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
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
 * @author gholami
 */
@Entity
@Table(name = "Yubikey")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "Yubikey.findAll", query = "SELECT y FROM Yubikey y"),
    @NamedQuery(name = "Yubikey.findByYubidnum", query = "SELECT y FROM Yubikey y WHERE y.yubidnum = :yubidnum"),
    @NamedQuery(name = "Yubikey.findBySerial", query = "SELECT y FROM Yubikey y WHERE y.serial = :serial"),
    @NamedQuery(name = "Yubikey.findByVersion", query = "SELECT y FROM Yubikey y WHERE y.version = :version"),
    @NamedQuery(name = "Yubikey.findByActive", query = "SELECT y FROM Yubikey y WHERE y.active = :active"),
    @NamedQuery(name = "Yubikey.findByNotes", query = "SELECT y FROM Yubikey y WHERE y.notes = :notes"),
    @NamedQuery(name = "Yubikey.findByCounter", query = "SELECT y FROM Yubikey y WHERE y.counter = :counter"),
    @NamedQuery(name = "Yubikey.findByLow", query = "SELECT y FROM Yubikey y WHERE y.low = :low"),
    @NamedQuery(name = "Yubikey.findByHigh", query = "SELECT y FROM Yubikey y WHERE y.high = :high"),
    @NamedQuery(name = "Yubikey.findBySessionUse", query = "SELECT y FROM Yubikey y WHERE y.sessionUse = :sessionUse"),
    @NamedQuery(name = "Yubikey.findByCreated", query = "SELECT y FROM Yubikey y WHERE y.created = :created"),
    @NamedQuery(name = "Yubikey.findByAesSecret", query = "SELECT y FROM Yubikey y WHERE y.aesSecret = :aesSecret"),
    @NamedQuery(name = "Yubikey.findByPublicId", query = "SELECT y FROM Yubikey y WHERE y.publicId = :publicId")})
public class Yubikey implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 45)
    @Column(name = "yubidnum")
    private String yubidnum;
    @Size(max = 10)
    @Column(name = "serial")
    private String serial;
    @Size(max = 15)
    @Column(name = "version")
    private String version;
    @Column(name = "active")
    private Boolean active;
    @Size(max = 100)
    @Column(name = "notes")
    private String notes;
    @Column(name = "counter")
    private Integer counter;
    @Column(name = "low")
    private Integer low;
    @Column(name = "high")
    private Integer high;
    @Column(name = "session_use")
    private Integer sessionUse;
    @Basic(optional = false)
    @NotNull
    @Column(name = "created")
    @Temporal(TemporalType.TIMESTAMP)
    private Date created;
    @Size(max = 100)
    @Column(name = "aes_secret")
    private String aesSecret;
    @Size(max = 40)
    @Column(name = "public_id")
    private String publicId;
    @JoinColumn(name = "People_uid", referencedColumnName = "uid")
    @ManyToOne
    private People peopleuid;

    public Yubikey() {
    }

    public Yubikey(String yubidnum) {
        this.yubidnum = yubidnum;
    }

    public Yubikey(String yubidnum, Date created) {
        this.yubidnum = yubidnum;
        this.created = created;
    }

    public String getYubidnum() {
        return yubidnum;
    }

    public void setYubidnum(String yubidnum) {
        this.yubidnum = yubidnum;
    }

    public String getSerial() {
        return serial;
    }

    public void setSerial(String serial) {
        this.serial = serial;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Integer getCounter() {
        return counter;
    }

    public void setCounter(Integer counter) {
        this.counter = counter;
    }

    public Integer getLow() {
        return low;
    }

    public void setLow(Integer low) {
        this.low = low;
    }

    public Integer getHigh() {
        return high;
    }

    public void setHigh(Integer high) {
        this.high = high;
    }

    public Integer getSessionUse() {
        return sessionUse;
    }

    public void setSessionUse(Integer sessionUse) {
        this.sessionUse = sessionUse;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public String getAesSecret() {
        return aesSecret;
    }

    public void setAesSecret(String aesSecret) {
        this.aesSecret = aesSecret;
    }

    public String getPublicId() {
        return publicId;
    }

    public void setPublicId(String publicId) {
        this.publicId = publicId;
    }

    public People getPeopleuid() {
        return peopleuid;
    }

    public void setPeopleuid(People peopleuid) {
        this.peopleuid = peopleuid;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (yubidnum != null ? yubidnum.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Yubikey)) {
            return false;
        }
        Yubikey other = (Yubikey) object;
        if ((this.yubidnum == null && other.yubidnum != null) || (this.yubidnum != null && !this.yubidnum.equals(other.yubidnum))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "se.kth.bbc.security.ua.model.Yubikey[ yubidnum=" + yubidnum + " ]";
    }
    
}
