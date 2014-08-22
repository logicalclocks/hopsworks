/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package se.kth.bbc.study;
import java.util.List;
 
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import se.kth.kthfsdashboard.user.UserFacade;
 
 
@ManagedBean(name="themeService", eager = true)
@ApplicationScoped
public class ThemeService {
     
     @EJB
    private UserFacade userFacade;
    
    @PostConstruct
    public void init() {
    }
     
    public List<Theme> getThemes(String studyName) {
        return userFacade.filterUsersBasedOnStudy(studyName);
    } 
    
}