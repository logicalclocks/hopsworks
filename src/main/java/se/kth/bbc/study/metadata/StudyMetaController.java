package se.kth.bbc.study.metadata;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import org.primefaces.model.DualListModel;
import se.kth.bbc.lims.MessagesController;
import se.kth.bbc.study.StudyMB;

/**
 *
 * @author stig
 */
@ManagedBean
@ViewScoped
public class StudyMetaController implements Serializable {

  @EJB
  private StudyMetaFacade studyMetaFacade;

  //TODO: replace with session bean
  @ManagedProperty(value = "#{studyManagedBean}")
  private StudyMB study;

  private StudyMeta metadata;
  private DualListModel<CollectionTypeStudyDesignEnum> studyDesignDual;
  private DualListModel<InclusionCriteriumEnum> inclusionCriteriaDual;

  public StudyMB getStudy() {
    return study;
  }

  public void setStudy(StudyMB study) {
    this.study = study;
  }

  public StudyMetaController() {
  }

  public void setMetadata(StudyMeta metadata) {
    this.metadata = metadata;
  }

  public StudyMeta getMetadata() {
    return metadata;
  }

  public void updateMetadata() {
    try {
      metadata.setStudyDesignList(studyDesignDual.getTarget());
      metadata.setInclusionCriteriaList(inclusionCriteriaDual.getTarget());
      studyMetaFacade.update(metadata);
    } catch (EJBException ejb) {
      MessagesController.addErrorMessage("Failed","Update failed.","updateMessage");
      return;
    }
    MessagesController.
            addInfoMessage("Success", "Metadata has been updated.","updateMessage");
  }

  @PostConstruct
  public void init() {
    metadata = studyMetaFacade.findByStudyname(study.getStudyName());
    if (metadata == null) {
      metadata = new StudyMeta();
      metadata.setStudyname(study.getStudyName());
    }

    //create study design model
    List<CollectionTypeStudyDesignEnum> availableDesign;
    List<CollectionTypeStudyDesignEnum> usedDesign;
    if (metadata.getStudyDesignList() == null || metadata.getStudyDesignList().
            isEmpty()) {
      availableDesign = Arrays.asList(CollectionTypeStudyDesignEnum.values());
      usedDesign = new ArrayList<>();
    } else {
      availableDesign = new ArrayList<>();
      usedDesign = new ArrayList<>();
      for (CollectionTypeStudyDesignEnum item : CollectionTypeStudyDesignEnum.
              values()) {
        if (metadata.getStudyDesignList().contains(item)) {
          usedDesign.add(item);
        } else {
          availableDesign.add(item);
        }
      }
    }
    studyDesignDual = new DualListModel<>(availableDesign, usedDesign);

    //create inclusion criteria model
    List<InclusionCriteriumEnum> availableCriteria;
    List<InclusionCriteriumEnum> usedCriteria;
    if (metadata.getInclusionCriteriaList() == null || metadata.
            getInclusionCriteriaList().isEmpty()) {
      availableCriteria = Arrays.asList(InclusionCriteriumEnum.values());
      usedCriteria = new ArrayList<>();
    } else {
      availableCriteria = new ArrayList<>();
      usedCriteria = new ArrayList<>();
      for (InclusionCriteriumEnum item : InclusionCriteriumEnum.
              values()) {
        if (metadata.getInclusionCriteriaList().contains(item)) {
          usedCriteria.add(item);
        } else {
          availableCriteria.add(item);
        }
      }
    }
    inclusionCriteriaDual = new DualListModel<>(availableCriteria, usedCriteria);
  }

  public void setStudyDesignDual(
          DualListModel<CollectionTypeStudyDesignEnum> studyDesignDual) {
    this.studyDesignDual = studyDesignDual;
  }

  public void setInclusionCriteriaDual(
          DualListModel<InclusionCriteriumEnum> inclusionCriteriaDual) {
    this.inclusionCriteriaDual = inclusionCriteriaDual;
  }

  public DualListModel<CollectionTypeStudyDesignEnum> getStudyDesignDual() {
    return this.studyDesignDual;
  }

  public DualListModel<InclusionCriteriumEnum> getInclusionCriteriaDual() {
    return this.inclusionCriteriaDual;
  }

  public void process() {
    //method to call to send data over
  }

}
