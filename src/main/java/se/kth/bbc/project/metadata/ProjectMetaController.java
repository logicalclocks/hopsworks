package se.kth.bbc.project.metadata;

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
import se.kth.bbc.lims.ClientSessionState;
import se.kth.bbc.lims.MessagesController;

/**
 *
 * @author stig
 */
@ManagedBean
@ViewScoped
public class ProjectMetaController implements Serializable {

  @EJB
  private ProjectMetaFacade projectMetaFacade;

  @ManagedProperty(value = "#{clientSessionState}")
  private ClientSessionState sessionState;

  private ProjectMeta metadata;
  private DualListModel<CollectionTypeProjectDesignEnum> projectDesignDual;
  private DualListModel<InclusionCriteriumEnum> inclusionCriteriaDual;

  public void setSessionState(ClientSessionState sessionState) {
    this.sessionState = sessionState;
  }

  public ProjectMetaController() {
  }

  public void setMetadata(ProjectMeta metadata) {
    this.metadata = metadata;
  }

  public ProjectMeta getMetadata() {
    return metadata;
  }

  public void updateMetadata() {
    try {
      metadata.setProjectDesignList(projectDesignDual.getTarget());
      metadata.setInclusionCriteriaList(inclusionCriteriaDual.getTarget());
      projectMetaFacade.update(metadata);
    } catch (EJBException ejb) {
      MessagesController.addErrorMessage("Failed", "Update failed.",
              "updateMessage");
      return;
    }
    MessagesController.
            addInfoMessage("Success", "Metadata has been updated.",
                    "updateMessage");
  }

  @PostConstruct
  public void init() {
    metadata = projectMetaFacade.
            findByProject(sessionState.getActiveProject());
    if (metadata == null) {
      metadata = new ProjectMeta(sessionState.getActiveProject().getId());
      metadata.setProject(sessionState.getActiveProject());
    }

    //create project design model
    List<CollectionTypeProjectDesignEnum> availableDesign;
    List<CollectionTypeProjectDesignEnum> usedDesign;
    if (metadata.getProjectDesignList() == null || metadata.getProjectDesignList().
            isEmpty()) {
      availableDesign = Arrays.asList(CollectionTypeProjectDesignEnum.values());
      usedDesign = new ArrayList<>();
    } else {
      availableDesign = new ArrayList<>();
      usedDesign = new ArrayList<>();
      for (CollectionTypeProjectDesignEnum item : CollectionTypeProjectDesignEnum.
              values()) {
        if (metadata.getProjectDesignList().contains(item)) {
          usedDesign.add(item);
        } else {
          availableDesign.add(item);
        }
      }
    }
    projectDesignDual = new DualListModel<>(availableDesign, usedDesign);

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

  public void setProjectDesignDual(
          DualListModel<CollectionTypeProjectDesignEnum> projectDesignDual) {
    this.projectDesignDual = projectDesignDual;
  }

  public void setInclusionCriteriaDual(
          DualListModel<InclusionCriteriumEnum> inclusionCriteriaDual) {
    this.inclusionCriteriaDual = inclusionCriteriaDual;
  }

  public DualListModel<CollectionTypeProjectDesignEnum> getProjectDesignDual() {
    return this.projectDesignDual;
  }

  public DualListModel<InclusionCriteriumEnum> getInclusionCriteriaDual() {
    return this.inclusionCriteriaDual;
  }

  public void process() {
    //method to call to send data over
  }

}
