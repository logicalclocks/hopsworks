/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package se.kth.kthfsdashboard.user;

import java.io.Serializable;
import java.util.List;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.NoneScoped;
import se.kth.bbc.study.StudyMB;

/**
 *
 * @author roshan
 */
@ManagedBean(name="filterUserBean", eager = true)
@NoneScoped
public class FilterUserBean implements Serializable{
    
    @EJB
    private UserFacade userFacade;
   
    
    @ManagedProperty("#{studyManagedBean}")
    private StudyMB study;
    
    public void setStudy(StudyMB study) {
        this.study = study;
    }
    
    public List<Username> findUsersToAddStudyTeam(){
        return userFacade.filterUsersBasedOnStudy(study.getStudyName());
    }
    
}
