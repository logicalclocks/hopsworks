/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.bbc.project;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@Table(name = "hopsworks.projects_management")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "ProjectsManagement.findAll", query = "SELECT p FROM ProjectsManagement p"),
    @NamedQuery(name = "ProjectsManagement.findByProjectname", query = "SELECT p FROM ProjectsManagement p WHERE p.projectname = :projectname"),
    @NamedQuery(name = "ProjectsManagement.findByYarnQuotaRemaining", query = "SELECT p FROM ProjectsManagement p WHERE p.yarnQuotaRemaining = :yarnQuotaRemaining"),
    @NamedQuery(name = "ProjectsManagement.findByYarnQuotaTotal", query = "SELECT p FROM ProjectsManagement p WHERE p.yarnQuotaTotal = :yarnQuotaTotal"),
    @NamedQuery(name = "ProjectsManagement.findByUsername", query = "SELECT p FROM ProjectsManagement p WHERE p.username = :username"),
    @NamedQuery(name = "ProjectsManagement.findByRetentionPeriod", query = "SELECT p FROM ProjectsManagement p WHERE p.retentionPeriod = :retentionPeriod"),
    @NamedQuery(name = "ProjectsManagement.findByDisabled", query = "SELECT p FROM ProjectsManagement p WHERE p.disabled = :disabled"),
    @NamedQuery(name = "ProjectsManagement.findByLastPaidAt", query = "SELECT p FROM ProjectsManagement p WHERE p.lastPaidAt = :lastPaidAt")})
public class ProjectsManagement implements Serializable {
    private static final long serialVersionUID = 1L;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 100)
    @Id
    private String projectname;
    @Column(name = "yarn_quota_remaining")
    private Integer yarnQuotaRemaining;
    @Column(name = "yarn_quota_total")
    private Integer yarnQuotaTotal;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 150)
    private String username;
    @Column(name = "retention_period")
    @Temporal(TemporalType.DATE)
    private Date retentionPeriod;
    private Boolean disabled;
    @Column(name = "last_paid_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastPaidAt;

    public ProjectsManagement() {
    }

    public String getProjectname() {
        return projectname;
    }

    public void setProjectname(String projectname) {
        this.projectname = projectname;
    }

    public Integer getYarnQuotaRemaining() {
        return yarnQuotaRemaining;
    }

    public void setYarnQuotaRemaining(Integer yarnQuotaRemaining) {
        this.yarnQuotaRemaining = yarnQuotaRemaining;
    }

    public Integer getYarnQuotaTotal() {
        return yarnQuotaTotal;
    }

    public void setYarnQuotaTotal(Integer yarnQuotaTotal) {
        this.yarnQuotaTotal = yarnQuotaTotal;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Date getRetentionPeriod() {
        return retentionPeriod;
    }

    public void setRetentionPeriod(Date retentionPeriod) {
        this.retentionPeriod = retentionPeriod;
    }

    public Boolean getDisabled() {
        return disabled;
    }

    public void setDisabled(Boolean disabled) {
        this.disabled = disabled;
    }

    public Date getLastPaidAt() {
        return lastPaidAt;
    }

    public void setLastPaidAt(Date lastPaidAt) {
        this.lastPaidAt = lastPaidAt;
    }
    
}
