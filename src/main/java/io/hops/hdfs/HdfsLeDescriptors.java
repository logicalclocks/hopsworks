/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.hops.hdfs;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author kerkinos
 */
@Entity
@Table(name = "hops.hdfs_le_descriptors")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "HdfsLeDescriptors.findEndpoint", query = "SELECT h FROM HdfsLeDescriptors h ASCENDING ORDER BY h.hdfsLeDescriptorsPK.id LIMIT 1"),
    @NamedQuery(name = "HdfsLeDescriptors.findAll", query = "SELECT h FROM HdfsLeDescriptors h"),
    @NamedQuery(name = "HdfsLeDescriptors.findById", query = "SELECT h FROM HdfsLeDescriptors h WHERE h.hdfsLeDescriptorsPK.id = :id"),
    @NamedQuery(name = "HdfsLeDescriptors.findByCounter", query = "SELECT h FROM HdfsLeDescriptors h WHERE h.counter = :counter"),
    @NamedQuery(name = "HdfsLeDescriptors.findByHostname", query = "SELECT h FROM HdfsLeDescriptors h WHERE h.hostname = :hostname"),
    @NamedQuery(name = "HdfsLeDescriptors.findByHttpAddress", query = "SELECT h FROM HdfsLeDescriptors h WHERE h.httpAddress = :httpAddress"),
    @NamedQuery(name = "HdfsLeDescriptors.findByPartitionVal", query = "SELECT h FROM HdfsLeDescriptors h WHERE h.hdfsLeDescriptorsPK.partitionVal = :partitionVal")})
public class HdfsLeDescriptors implements Serializable {
    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected HdfsLeDescriptorsPK hdfsLeDescriptorsPK;
    @Basic(optional = false)
    @NotNull
    @Column(name = "counter")
    private long counter;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 25)
    @Column(name = "hostname")
    private String hostname;
    @Size(max = 100)
    @Column(name = "httpAddress")
    private String httpAddress;

    public HdfsLeDescriptors() {
    }

    public HdfsLeDescriptors(HdfsLeDescriptorsPK hdfsLeDescriptorsPK) {
        this.hdfsLeDescriptorsPK = hdfsLeDescriptorsPK;
    }

    public HdfsLeDescriptors(HdfsLeDescriptorsPK hdfsLeDescriptorsPK, long counter, String hostname) {
        this.hdfsLeDescriptorsPK = hdfsLeDescriptorsPK;
        this.counter = counter;
        this.hostname = hostname;
    }

    public HdfsLeDescriptors(long id, int partitionVal) {
        this.hdfsLeDescriptorsPK = new HdfsLeDescriptorsPK(id, partitionVal);
    }

    public HdfsLeDescriptorsPK getHdfsLeDescriptorsPK() {
        return hdfsLeDescriptorsPK;
    }

    public void setHdfsLeDescriptorsPK(HdfsLeDescriptorsPK hdfsLeDescriptorsPK) {
        this.hdfsLeDescriptorsPK = hdfsLeDescriptorsPK;
    }

    public long getCounter() {
        return counter;
    }

    public void setCounter(long counter) {
        this.counter = counter;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getHttpAddress() {
        return httpAddress;
    }

    public void setHttpAddress(String httpAddress) {
        this.httpAddress = httpAddress;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (hdfsLeDescriptorsPK != null ? hdfsLeDescriptorsPK.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof HdfsLeDescriptors)) {
            return false;
        }
        HdfsLeDescriptors other = (HdfsLeDescriptors) object;
        if ((this.hdfsLeDescriptorsPK == null && other.hdfsLeDescriptorsPK != null) || (this.hdfsLeDescriptorsPK != null && !this.hdfsLeDescriptorsPK.equals(other.hdfsLeDescriptorsPK))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "io.hops.hdfs.HdfsLeDescriptors[ hdfsLeDescriptorsPK=" + hdfsLeDescriptorsPK + " ]";
    }
    
}
