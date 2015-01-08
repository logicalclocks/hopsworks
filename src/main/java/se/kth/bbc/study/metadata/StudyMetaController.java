package se.kth.bbc.study.metadata;

import java.io.Serializable;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.inject.Named;
import javax.faces.bean.ViewScoped;
import se.kth.bbc.lims.MessagesController;
import se.kth.bbc.study.StudyMB;

/**
 *
 * @author stig
 */
@ManagedBean
@ViewScoped
public class StudyMetaController implements Serializable{

  @EJB
  private StudyMetaFacade studyMetaFacade;

  //TODO: replace with session bean
  @ManagedProperty(value = "#{studyManagedBean}")
  private StudyMB study;

  private StudyMeta metadata;

  public StudyMB getStudy() {
    return study;
  }

  public void setStudy(StudyMB study) {
    this.study = study;
  }

  public StudyMetaController() {
  }

  public boolean metaDataAvailable() {
    return studyMetaFacade.findByStudyname(study.getStudyName()) != null;
  }

  public void setMetadata(StudyMeta metadata) {
    this.metadata = metadata;
  }

  public StudyMeta getMetadata() {
    if(metadata == null){
      metadata = studyMetaFacade.findByStudyname(study.getStudyName());
      if(metadata== null){
        metadata = new StudyMeta();
        metadata.setStudyname(study.getStudyName());
      }
    }
    return metadata;
  }

  public void updateMetadata() {
    try {
      studyMetaFacade.update(metadata);
    } catch (EJBException ejb) {
      MessagesController.addErrorMessage("Update failed.");
      return;
    }
    MessagesController.
            addInfoMessage("Success", "Metadata has been updated.");
  }

}
