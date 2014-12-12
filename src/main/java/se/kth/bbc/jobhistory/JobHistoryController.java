package se.kth.bbc.jobhistory;

import java.io.Serializable;
import java.util.List;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import se.kth.bbc.study.StudyMB;

/**
 *
 * @author stig
 */
@ManagedBean
@RequestScoped
public class JobHistoryController implements Serializable{
  
  @EJB
  private JobHistoryFacade history;
  
  @ManagedProperty(value="#{studyManagedBean}")
  private StudyMB studies;
  
  public void setStudies(StudyMB studies){
    this.studies = studies;
  }
  
  public List<JobHistory> getHistoryForType(String type){
    return history.findForStudyByType(studies.getStudyName(), type);
  }
}
