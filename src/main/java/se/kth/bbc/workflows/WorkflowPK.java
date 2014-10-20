/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.bbc.workflows;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 *
 * @author stig
 */
@Embeddable
public class WorkflowPK implements Serializable {
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 128)
    private String title;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 128)
    private String study;

    public WorkflowPK() {
    }

    public WorkflowPK(String title, String study) {
        this.title = title;
        this.study = study;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getStudy() {
        return study;
    }

    public void setStudy(String study) {
        this.study = study;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (title != null ? title.hashCode() : 0);
        hash += (study != null ? study.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof WorkflowPK)) {
            return false;
        }
        WorkflowPK other = (WorkflowPK) object;
        if ((this.title == null && other.title != null) || (this.title != null && !this.title.equals(other.title))) {
            return false;
        }
        if ((this.study == null && other.study != null) || (this.study != null && !this.study.equals(other.study))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "se.kth.bbc.workflows.WorkflowPK[ title=" + title + ", study=" + study + " ]";
    }
    
}
