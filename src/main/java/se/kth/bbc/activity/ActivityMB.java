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
import java.util.Map;
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
@ManagedBean(name = "activityBean")
@NoneScoped
public class ActivityMB implements Serializable {

    private static final Logger logger = Logger.getLogger(ActivityMB.class.getName());
    private static final long serialVersionUID = 1L;

    @EJB
    private ActivityController activityController;

    private UserActivity activity;

    private String flag;

    public String getFlag() {
        return flag;
    }

    public void setFlag(String flag) {
        this.flag = flag;
    }

    public void fetchFlag() {

        FacesContext fc = FacesContext.getCurrentInstance();
        Map<String, String> params = fc.getExternalContext().getRequestParameterMap();
        this.flag = params.get("teamFlag");
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

    public List<UserActivity> getActivityList() {
        return activityController.filterActivity();
    }
    
    public List<ActivityDetail> getActivityDetailList() {
        return activityController.filterActivityDetail();
    }

    public List<UserActivity> getActivityOnstudy(String activityOn) {
        return activityController.activityOnstudy(activityOn);
    }

    public String findLastActivity(int id) {

        Iterator<UserActivity> itr = activityController.activityOnID(id).listIterator();
        long currentTime = new Date().getTime();
        while (itr.hasNext()) {
            long fetchedTime = itr.next().getTimestamp().getTime();
            if ((currentTime - fetchedTime) / 1000 >= 0 && (currentTime - fetchedTime) / 1000 <= 118) {
                return String.format("about %s minute ago.", 1);
            } else if ((currentTime - fetchedTime) / 1000 > 118 && (currentTime - fetchedTime) / 1000 < 1800) {
                return String.format("%s minutes ago.", (currentTime - fetchedTime) / 60000);
            } else if ((currentTime - fetchedTime) / 1000 > 1800 && (currentTime - fetchedTime) / 1000 <= 7056) {
                return String.format("about %s hour ago.", 1);
            } else if ((currentTime - fetchedTime) / 1000 > 7056 && (currentTime - fetchedTime) / 1000 <= 45400) {
                return String.format("%s hours ago.", (currentTime - fetchedTime) / 3600000);
            } else if ((currentTime - fetchedTime) / 1000 > 45400 && (currentTime - fetchedTime) / 1000 <= 170000) {
                return String.format("about %s day ago.", 1);
            } else if ((currentTime - fetchedTime) / 1000 > 170000 && (currentTime - fetchedTime) / 1000 <= 1300000) {
                return String.format("%s days ago.", (currentTime - fetchedTime) / 86400000);
            } else if ((currentTime - fetchedTime) / 1000 > 1300000 && (currentTime - fetchedTime) / 1000 <= 2500000) {
                return String.format("about %s month ago.", 1);
            } else if ((currentTime - fetchedTime) / 1000 > 2500000 && (currentTime - fetchedTime) / 1000 < 25000000) {
                return String.format("%s months ago.", (currentTime - fetchedTime) / (1000 * 2600000));
            } else {
                return String.format("about %s year ago.", 1);
            }
        }
        return "more than a year ago"; // dummy
    }

    public String findLastActivityOnStudy(String name) {

        Iterator<UserActivity> itr = activityController.lastActivityOnStudy(name).listIterator();
        long currentTime = new Date().getTime();
        while (itr.hasNext()) {
            long getLastUpdate = itr.next().getTimestamp().getTime();
            if ((currentTime - getLastUpdate) / 1000 >= 0 && (currentTime - getLastUpdate) / 1000 <= 118) {
                return String.format("about %s minute ago.", 1);
            } else if ((currentTime - getLastUpdate) / 1000 > 118 && (currentTime - getLastUpdate) / 1000 < 1800) {
                return String.format("%s minutes ago.", (currentTime - getLastUpdate) / 60000);
            } else if ((currentTime - getLastUpdate) / 1000 > 1800 && (currentTime - getLastUpdate) / 1000 <= 7056) {
                return String.format("about %s hour ago.", 1);
            } else if ((currentTime - getLastUpdate) / 1000 > 7056 && (currentTime - getLastUpdate) / 1000 <= 45400) {
                return String.format("%s hours ago.", (currentTime - getLastUpdate) / 3600000);
            } else if ((currentTime - getLastUpdate) / 1000 > 45400 && (currentTime - getLastUpdate) / 1000 <= 170000) {
                return String.format("about %s day ago.", 1);
            } else if ((currentTime - getLastUpdate) / 1000 > 170000 && (currentTime - getLastUpdate) / 1000 <= 1300000) {
                return String.format("%s days ago.", (currentTime - getLastUpdate) / 86400000);
            } else if ((currentTime - getLastUpdate) / 1000 > 1300000 && (currentTime - getLastUpdate) / 1000 <= 2500000) {
                return String.format("about %s month ago.", 1);
            } else if ((currentTime - getLastUpdate) / 1000 > 2500000 && (currentTime - getLastUpdate) / 1000 < 25000000) {
                return String.format("%s months ago.", (currentTime - getLastUpdate) / (1000 * 2600000));
            } else {
                return String.format("about %s year ago.", 1);
            }
        }
        return "more than a year ago"; // dummy
    }

    public void addActivity(String message, String activityAbout, String flag) {

        activity.setId(Integer.SIZE);
        activity.setActivity(message);
        activity.setPerformedBy(getUsername());
        activity.setTimestamp(new Date());
        activity.setFlag(flag);
        activity.setActivityOn(activityAbout);

        try {
            activityController.persistActivity(activity);
        } catch (EJBException ejb) {
            logger.log(Level.SEVERE, " Add new activity for new study failed!");
            return;
        }
            logger.log(Level.FINE, " Add new activity for new study successful: {0}", activity.getId());
    }
    
    public void addSampleActivity(String message, String activityAbout, String flag, String username) {

        activity.setId(Integer.SIZE);
        activity.setActivity(message);
        activity.setPerformedBy(username);
        activity.setTimestamp(new Date());
        activity.setFlag(flag);
        activity.setActivityOn(activityAbout);

        try {
            activityController.persistActivity(activity);
        } catch (EJBException ejb) {
            logger.log(Level.SEVERE, " Add new activity for new study failed!");
            return;
        }
            logger.log(Level.FINE, " Add new activity for new study successful: {0}", activity.getId());
    }
    

    public List<UserActivity> getAllTeamActivity() {
        return activityController.findAllTeamActivity(getFlag());
    }

    private HttpServletRequest getRequest() {
        return (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
    }

    public String getUsername() {
        return getRequest().getUserPrincipal().getName();
    }

    public String getGravatar(String email) {

        Gravatar gravatar = new Gravatar();
        gravatar.setRating(GravatarRating.GENERAL_AUDIENCES);
        String url = gravatar.getUrl(email);

        return url;
    }

}
