/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package se.kth.bbc.activity;

import com.timgroup.jgravatar.Gravatar;
import com.timgroup.jgravatar.GravatarDefaultImage;
import com.timgroup.jgravatar.GravatarRating;
import java.io.Serializable;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.NoneScoped;
import javax.faces.bean.RequestScoped;
import javax.faces.bean.SessionScoped;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;



/**
 *
 * @author roshan
 */

@ManagedBean(name="activityBean")
@NoneScoped
public class ActivityMB implements Serializable {
    
   private static final Logger logger = Logger.getLogger(ActivityMB.class.getName());
   private static final long serialVersionUID = 1L; 
    
   @EJB
   private ActivityController activityController;
   
   private UserActivity activity;
   
   
   private String username;
   private String studyName;
   
   
   public void setEmail(String username) {
       this.username = username;
   }
   
   public String getEmail(){
       return username;
   }
   
   public void setStudyname(String studyName) {
       this.studyName = studyName;
   }
   
   public String getStudyname(){
       return studyName;
   }
   
   public UserActivity getActivity() {
        if (activity == null) {
            activity = new UserActivity();
        }
        return activity;
    }
    
    public void setActivity(UserActivity activity) {
        this.activity = activity;
    } 
   
    
    public List<UserActivity> getAcvitiyList(){
            return activityController.filterActivity();
    }
    
    
    public List<UserActivity> getActivityOnstudy(String activityOn){
        return activityController.activityOnstudy(activityOn);
    }
    
//    public long findLastActivity(String activityOn){
//        Iterator<UserActivity> itr = activityController.activityOnstudy(activityOn).listIterator();
//        while(itr.hasNext()){
//              return itr.next().getTimestamp().getTime();
//        }
//    }
    
    public void addActivity(String message, String activityAbout, String flag){
        
        activity.setId(Integer.SIZE);
        activity.setActivity(message);
        activity.setPerformedBy(getUsername());
        activity.setTimestamp(new Date());
        activity.setFlag(flag);
        activity.setActivityOn(activityAbout);
        
        try{
            activityController.persistActivity(activity);
        }catch(EJBException ejb){
            logger.log(Level.SEVERE, " Add new activity for new study failed!");
            return;
        }
            logger.log(Level.FINE, " Add new activity for new study successful: {0}", activity.getId());
    }
    
    
     private HttpServletRequest getRequest() {
        return (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
    }
    
    
    public String getUsername(){
          return getRequest().getUserPrincipal().getName();
    }
     
    public String getGravatar(String email){
    
         Gravatar gravatar = new Gravatar();
         gravatar.setRating(GravatarRating.GENERAL_AUDIENCES);
         String url = gravatar.getUrl(email);
    
         return url;
    }
   
    
}
