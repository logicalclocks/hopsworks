/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package se.kth.bbc.study;
import java.util.ArrayList;
import java.util.List;
 
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import se.kth.bbc.activity.ActivityMB;
import se.kth.kthfsdashboard.user.UserFacade;
import se.kth.kthfsdashboard.user.Username;
 
 
@ManagedBean(name="themeService", eager = true)
@ApplicationScoped
public class ThemeService {
     
     @EJB
    private UserFacade userFacade;
    private List<Username> username;
    
    
    private List<Theme> themes;
//     
//    @ManagedProperty(value = "#{studyManagedBean}")
//    private StudyMB study;
    
    
    @PostConstruct
    public void init() {
         themes = new ArrayList<Theme>();
         //username = userFacade.filterUsersBasedOnStudy(study.getStudyName());
           username = userFacade.findAll();
        for(int i=0;i<username.size();i++){
            Username un = username.get(i);
                themes.add(new Theme(i, un.getName(), un.getEmail()));
        }
    }
     
    public List<Theme> getThemes() {
        return themes;
    } 
   
//    public void setStudy(StudyMB study) {
//        this.study = study;
//    }
    
}