package se.kth.bbc.activity;

import java.io.Serializable;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import org.primefaces.model.LazyDataModel;
import se.kth.bbc.study.Study;
import se.kth.kthfsdashboard.user.Gravatar;

/**
 *
 * @author roshan
 */
@ManagedBean(name = "activityBean")
@SessionScoped
public class ActivityMB implements Serializable {

  private static final Logger logger = Logger.getLogger(ActivityMB.class.
          getName());
  private static final long serialVersionUID = 1L;

  @EJB
  private ActivityFacade activityFacade;

  @EJB
  private ActivityDetailFacade activityDetailFacade;

  private LazyDataModel<ActivityDetail> allLazyModel;

  @PostConstruct
  public void init() {
    try {
      this.allLazyModel = new LazyActivityModel(activityDetailFacade);
      int cnt = (int) activityFacade.getTotalCount();
      allLazyModel.setRowCount(cnt);
    } catch (IllegalArgumentException e) {
      logger.log(Level.SEVERE, "Failed to initialize LazyActivityModel.", e);
      this.allLazyModel = null;
    }
  }

  public LazyDataModel<ActivityDetail> getAllLazyModel() {
    return allLazyModel;
  }

  public List<ActivityDetail> getActivityDetailList() {
    return activityDetailFacade.getAllActivityDetail();
  }

  public List<ActivityDetail> getActivityDetailOnStudy(String studyName) {
    return activityDetailFacade.activityDetailOnStudy(studyName);
  }

  public String findLastActivity(int id) {

    Iterator<Activity> itr = activityFacade.activityOnID(id).
            listIterator();
    long currentTime = new Date().getTime();
    while (itr.hasNext()) {
      long fetchedTime = itr.next().getTimestamp().getTime();
      if ((currentTime - fetchedTime) / 1000 >= 0 && (currentTime - fetchedTime)
              / 1000 <= 20) {
        return String.format("less than a minute ago.");
      } else if ((currentTime - fetchedTime) / 1000 > 20 && (currentTime
              - fetchedTime) / 1000 <= 118) {
        return String.format("about %s minute ago.", 1);
      } else if ((currentTime - fetchedTime) / 1000 > 118 && (currentTime
              - fetchedTime) / 1000 < 1800) {
        return String.format("%s minutes ago.", (currentTime - fetchedTime)
                / 60000);
      } else if ((currentTime - fetchedTime) / 1000 > 1800 && (currentTime
              - fetchedTime) / 1000 <= 7056) {
        return String.format("about %s hour ago.", 1);
      } else if ((currentTime - fetchedTime) / 1000 > 7056 && (currentTime
              - fetchedTime) / 1000 <= 45400) {
        return String.format("%s hours ago.", (currentTime - fetchedTime)
                / 3600000);
      } else if ((currentTime - fetchedTime) / 1000 > 45400 && (currentTime
              - fetchedTime) / 1000 <= 170000) {
        return String.format("about %s day ago.", 1);
      } else if ((currentTime - fetchedTime) / 1000 > 170000 && (currentTime
              - fetchedTime) / 1000 <= 1300000) {
        return String.format("%s days ago.", (currentTime - fetchedTime)
                / 86400000);
      } else if ((currentTime - fetchedTime) / 1000 > 1300000 && (currentTime
              - fetchedTime) / 1000 <= 2500000) {
        return String.format("about %s month ago.", 1);
      } else if ((currentTime - fetchedTime) / 1000 > 2500000 && (currentTime
              - fetchedTime) / 1000 < 25000000) {
        return String.format("%s months ago.", (currentTime - fetchedTime)
                / 1000 / 2600000);
      } else {
        return String.format("about %s year ago.", 1);
      }
    }
    return "more than a year ago"; // dummy
  }

  public String findLastActivityOnStudy(Study study) {

    Activity itr = activityFacade.lastActivityOnStudy(study);
    long currentTime = new Date().getTime();

    long getLastUpdate = itr.getTimestamp().getTime();
    if ((currentTime - getLastUpdate) / 1000 >= 0 && (currentTime
            - getLastUpdate) / 1000 <= 20) {
      return String.format("less than a minute ago.");
    } else if ((currentTime - getLastUpdate) / 1000 > 20 && (currentTime
            - getLastUpdate) / 1000 <= 118) {
      return String.format("about %s minute ago.", 1);
    } else if ((currentTime - getLastUpdate) / 1000 > 118 && (currentTime
            - getLastUpdate) / 1000 < 1800) {
      return String.format("%s minutes ago.", (currentTime - getLastUpdate)
              / 60000);
    } else if ((currentTime - getLastUpdate) / 1000 > 1800 && (currentTime
            - getLastUpdate) / 1000 <= 7056) {
      return String.format("about %s hour ago.", 1);
    } else if ((currentTime - getLastUpdate) / 1000 > 7056 && (currentTime
            - getLastUpdate) / 1000 <= 45400) {
      return String.format("%s hours ago.", (currentTime - getLastUpdate)
              / 3600000);
    } else if ((currentTime - getLastUpdate) / 1000 > 45400 && (currentTime
            - getLastUpdate) / 1000 <= 170000) {
      return String.format("about %s day ago.", 1);
    } else if ((currentTime - getLastUpdate) / 1000 > 170000 && (currentTime
            - getLastUpdate) / 1000 <= 1300000) {
      return String.format("%s days ago.", (currentTime - getLastUpdate)
              / 86400000);
    } else if ((currentTime - getLastUpdate) / 1000 > 1300000 && (currentTime
            - getLastUpdate) / 1000 <= 2500000) {
      return String.format("about %s month ago.", 1);
    } else if ((currentTime - getLastUpdate) / 1000 > 2500000 && (currentTime
            - getLastUpdate) / 1000 < 25000000) {
      return String.format("%s months ago.", (currentTime - getLastUpdate)
              / 1000 / 2600000);
    } else {
      return String.format("about %s year ago.", 1);
    }
  }

  public String getGravatar(String email, int size) {
    return Gravatar.getUrl(email, size);
  }

}
