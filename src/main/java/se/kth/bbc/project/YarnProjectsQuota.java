/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.bbc.project;

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
@Table(name = "hops.yarn_projects_quota")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "YarnProjectsQuota.findAll", query = "SELECT y FROM YarnProjectsQuota y"),
    @NamedQuery(name = "YarnProjectsQuota.findByProjectname", query = "SELECT y FROM YarnProjectsQuota y WHERE y.projectname = :projectname"),
    @NamedQuery(name = "YarnProjectsQuota.findByQuotaRemaining", query = "SELECT y FROM YarnProjectsQuota y WHERE y.quotaRemaining = :quotaRemaining"),
    @NamedQuery(name = "YarnProjectsQuota.findByTotal", query = "SELECT y FROM YarnProjectsQuota y WHERE y.total = :total")})
public class YarnProjectsQuota implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 100)
    private String projectname;
    @Column(name = "quota_remaining")
    private Integer quotaRemaining;
    private Integer total;

    public YarnProjectsQuota() {
    }

    public YarnProjectsQuota(String projectname) {
        this.projectname = projectname;
    }

    public YarnProjectsQuota(String projectname, int quotaRemaining, int total) {
        this.projectname = projectname;
        this.quotaRemaining = quotaRemaining;
        this.total = total;
    }

    public String getProjectname() {
        return projectname;
    }

    public void setProjectname(String projectname) {
        this.projectname = projectname;
    }

    public Integer getQuotaRemaining() {
        return quotaRemaining;
    }

    public void setQuotaRemaining(Integer quotaRemaining) {
        this.quotaRemaining = quotaRemaining;
    }

    public Integer getTotal() {
        return total;
    }

    public void setTotal(Integer total) {
        this.total = total;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (projectname != null ? projectname.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof YarnProjectsQuota)) {
            return false;
        }
        YarnProjectsQuota other = (YarnProjectsQuota) object;
        if ((this.projectname == null && other.projectname != null) || (this.projectname != null && !this.projectname.equals(other.projectname))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "se.kth.bbc.project.YarnProjectsQuota[ projectname=" + projectname + " ]";
    }
    
}
