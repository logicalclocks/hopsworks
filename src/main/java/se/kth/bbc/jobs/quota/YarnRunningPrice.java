/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.bbc.jobs.quota;

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
 * @author jdowling
 */
@Entity
@Table(name = "hops.yarn_running_price")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "YarnRunningPrice.findAll", query = "SELECT y FROM YarnRunningPrice y"),
  @NamedQuery(name = "YarnRunningPrice.findById", query = "SELECT y FROM YarnRunningPrice y WHERE y.id = :id"),
  @NamedQuery(name = "YarnRunningPrice.findByTime", query = "SELECT y FROM YarnRunningPrice y WHERE y.time = :time"),
  @NamedQuery(name = "YarnRunningPrice.findLatest", query = "SELECT y FROM YarnRunningPrice y ORDER BY y.time"),
  @NamedQuery(name = "YarnRunningPrice.findByPrice", query = "SELECT y FROM YarnRunningPrice y WHERE y.price = :price")})
public class YarnRunningPrice implements Serializable {

  private static final long serialVersionUID = 1L;
  @Id
  @Basic(optional = false)
  @NotNull
  @Size(min = 1, max = 255)
  @Column(name = "id")
  private String id;
  @Basic(optional = false)
  @NotNull
  @Column(name = "time")
  private long time;
  @Basic(optional = false)
  @NotNull
  @Column(name = "price")
  private float price;

  public YarnRunningPrice() {
  }

  public YarnRunningPrice(String id) {
    this.id = id;
  }

  public YarnRunningPrice(String id, long time, float price) {
    this.id = id;
    this.time = time;
    this.price = price;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public long getTime() {
    return time;
  }

  public void setTime(long time) {
    this.time = time;
  }

  public float getPrice() {
    return price;
  }

  public void setPrice(float price) {
    this.price = price;
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
    if (!(object instanceof YarnRunningPrice)) {
      return false;
    }
    YarnRunningPrice other = (YarnRunningPrice) object;
    if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "io.hops.bbc.YarnRunningPrice[ id=" + id + " ]";
  }
  
}
