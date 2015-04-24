/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.bbc.study.fb;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 *
 * @author stig
 */
@Embeddable
public class InodePK implements Serializable {

  @Basic(optional = false)
  @NotNull
  @Column(name = "parent_id")
  private int parentId;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 3000)
  @Column(name = "name")
  private String name;

  public InodePK() {
  }

  public InodePK(int parentId, String name) {
    this.parentId = parentId;
    this.name = name;
  }

  public int getParentId() {
    return parentId;
  }

  public void setParentId(int parentId) {
    this.parentId = parentId;
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
    hash += (int) parentId;
    hash += (name != null ? name.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof InodePK)) {
      return false;
    }
    InodePK other = (InodePK) object;
    if (this.parentId != other.parentId) {
      return false;
    }
    if ((this.name == null && other.name != null) || (this.name != null
            && !this.name.equals(other.name))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "se.kth.bbc.study.fb.InodePK[ parentId=" + parentId + ", name="
            + name + " ]";
  }

}
