/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.bbc.project;

import io.hops.bbc.ProjectPaymentAction;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@Table(name = "hopsworks.project_payments_history")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "ProjectPaymentsHistory.findAll", query = "SELECT p FROM ProjectPaymentsHistory p"),
    @NamedQuery(name = "ProjectPaymentsHistory.findByProjectname", query = "SELECT p FROM ProjectPaymentsHistory p WHERE p.projectPaymentsHistoryPK.projectname = :projectname"),
    @NamedQuery(name = "ProjectPaymentsHistory.findByUsername", query = "SELECT p FROM ProjectPaymentsHistory p WHERE p.username = :username"),
    @NamedQuery(name = "ProjectPaymentsHistory.findByAction", query = "SELECT p FROM ProjectPaymentsHistory p WHERE p.action = :action"),
    @NamedQuery(name = "ProjectPaymentsHistory.findByTransactionDate", query = "SELECT p FROM ProjectPaymentsHistory p WHERE p.projectPaymentsHistoryPK.transactionDate = :transactionDate"),
    @NamedQuery(name = "ProjectPaymentsHistory.findByAmount", query = "SELECT p FROM ProjectPaymentsHistory p WHERE p.amount = :amount")})
public class ProjectPaymentsHistory implements Serializable {
    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected ProjectPaymentsHistoryPK projectPaymentsHistoryPK;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 150)
    private String username;
    @Basic(optional = false)
    @NotNull
    @Enumerated(EnumType.STRING)
    private ProjectPaymentAction action;
    @Basic(optional = false)
    @NotNull
    private int amount;

    public ProjectPaymentsHistory() {
    }

    public ProjectPaymentsHistory(ProjectPaymentsHistoryPK projectPaymentsHistoryPK) {
        this.projectPaymentsHistoryPK = projectPaymentsHistoryPK;
    }

    public ProjectPaymentsHistory(ProjectPaymentsHistoryPK projectPaymentsHistoryPK, String username, ProjectPaymentAction action, int amount) {
        this.projectPaymentsHistoryPK = projectPaymentsHistoryPK;
        this.username = username;
        this.action = action;
        this.amount = amount;
    }

    public ProjectPaymentsHistory(String projectname, Date transactionDate) {
        this.projectPaymentsHistoryPK = new ProjectPaymentsHistoryPK(projectname, transactionDate);
    }

    public ProjectPaymentsHistoryPK getProjectPaymentsHistoryPK() {
        return projectPaymentsHistoryPK;
    }

    public void setProjectPaymentsHistoryPK(ProjectPaymentsHistoryPK projectPaymentsHistoryPK) {
        this.projectPaymentsHistoryPK = projectPaymentsHistoryPK;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public ProjectPaymentAction getAction() {
        return action;
    }

    public void setAction(ProjectPaymentAction action) {
        this.action = action;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (projectPaymentsHistoryPK != null ? projectPaymentsHistoryPK.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof ProjectPaymentsHistory)) {
            return false;
        }
        ProjectPaymentsHistory other = (ProjectPaymentsHistory) object;
        if ((this.projectPaymentsHistoryPK == null && other.projectPaymentsHistoryPK != null) || (this.projectPaymentsHistoryPK != null && !this.projectPaymentsHistoryPK.equals(other.projectPaymentsHistoryPK))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "se.kth.bbc.project.ProjectPaymentsHistory[ projectPaymentsHistoryPK=" + projectPaymentsHistoryPK + " ]";
    }
    
}
