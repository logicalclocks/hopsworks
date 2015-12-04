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
import javax.persistence.Embeddable;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 *
 * @author kerkinos
 */
@Embeddable
public class ProjectPaymentsHistoryPK implements Serializable {
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 100)
    private String projectname;
    @Basic(optional = false)
    @NotNull
    @Column(name = "transaction_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date transactionDate;

    public ProjectPaymentsHistoryPK() {
    }

    public ProjectPaymentsHistoryPK(String projectname, Date transactionDate) {
        this.projectname = projectname;
        this.transactionDate = transactionDate;
    }

    public String getProjectname() {
        return projectname;
    }

    public void setProjectname(String projectname) {
        this.projectname = projectname;
    }

    public Date getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(Date transactionDate) {
        this.transactionDate = transactionDate;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (projectname != null ? projectname.hashCode() : 0);
        hash += (transactionDate != null ? transactionDate.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof ProjectPaymentsHistoryPK)) {
            return false;
        }
        ProjectPaymentsHistoryPK other = (ProjectPaymentsHistoryPK) object;
        if ((this.projectname == null && other.projectname != null) || (this.projectname != null && !this.projectname.equals(other.projectname))) {
            return false;
        }
        if ((this.transactionDate == null && other.transactionDate != null) || (this.transactionDate != null && !this.transactionDate.equals(other.transactionDate))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "se.kth.bbc.project.ProjectPaymentsHistoryPK[ projectname=" + projectname + ", transactionDate=" + transactionDate + " ]";
    }
    
}
