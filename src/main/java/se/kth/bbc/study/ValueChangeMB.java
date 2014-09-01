/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package se.kth.bbc.study;

import java.io.Serializable;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.NoneScoped;
import javax.faces.bean.SessionScoped;
import javax.faces.component.UIOutput;
import javax.faces.event.AjaxBehaviorEvent;
import javax.faces.event.ValueChangeEvent;


/**
 *
 * @author roshan
 */

@ManagedBean(name="valueChangeMB", eager = true)
@SessionScoped
public class ValueChangeMB implements Serializable{
    
    private static final long serialVersionUID = 1L;
  
    @ManagedProperty(value = "#{studyManagedBean}")
    private StudyMB studyMB;
    
    
    private String newTeamRole;
    
    public void setStudyMB(StudyMB studyMB){
        this.studyMB = studyMB;
    }
        
    public String getNewTeamRole(){
        return newTeamRole;
    }
    
    public void setNewTeamRole(String newTeamRole){
        this.newTeamRole = newTeamRole;
    }
    
     public void teamRoleChanged(ValueChangeEvent e){

        System.out.println(" new value for team role - "+ newTeamRole);
      
    
//      if(!selectedTeamRole.equals(e.getOldValue().toString()))
//            updateStudyTeamRole();
   }
    
}
