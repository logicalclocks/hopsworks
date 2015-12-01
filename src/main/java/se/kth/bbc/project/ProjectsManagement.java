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
    @NamedQuery(name = "ProjectsManagement.findByQuotaRemaining", query = "SELECT p FROM ProjectsManagement p WHERE p.quotaRemaining = :quotaRemaining"),
    @NamedQuery(name = "ProjectsManagement.findByTotal", query = "SELECT p FROM ProjectsManagement p WHERE p.total = :total"),
    @NamedQuery(name = "ProjectsManagement.findByUsername", query = "SELECT p FROM ProjectsManagement p WHERE p.username = :username"),
    @NamedQuery(name = "ProjectsManagement.findByRetentionPeriod", query = "SELECT p FROM ProjectsManagement p WHERE p.retentionPeriod = :retentionPeriod"),
    @NamedQuery(name = "ProjectsManagement.findByArchived", query = "SELECT p FROM ProjectsManagement p WHERE p.archived = :archived"),
    @NamedQuery(name = "ProjectsManagement.findByDate", query = "SELECT p FROM ProjectsManagement p WHERE p.date = :date")})
public class ProjectsManagement implements Serializable {
    private static final long serialVersionUID = 1L;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 100)
    @Column(name = "projectname")
    private String projectname;
    @Column(name = "quota_remaining")
    private Integer quotaRemaining;
    @Column(name = "total")
    private Integer total;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 150)
    @Column(name = "username")
    private String username;
    @Column(name = "retention_period")
    @Temporal(TemporalType.DATE)
    private Date retentionPeriod;
    @Column(name = "archived")
    private Boolean archived;
    @Basic(optional = false)
    @NotNull
    @Column(name = "date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date date;

    public ProjectsManagement() {
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

    public Boolean getArchived() {
        return archived;
    }

    public void setArchived(Boolean archived) {
        this.archived = archived;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
    
}
