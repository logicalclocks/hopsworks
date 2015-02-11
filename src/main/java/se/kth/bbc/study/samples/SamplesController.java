package se.kth.bbc.study.samples;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import org.primefaces.model.DualListModel;
import se.kth.bbc.fileoperations.FileOperations;
import se.kth.bbc.lims.ClientSessionState;
import se.kth.bbc.lims.Constants;
import se.kth.bbc.lims.MessagesController;
import se.kth.bbc.study.metadata.CollectionTypeStudyDesignEnum;
import se.kth.kthfsdashboard.user.UserFacade;

/**
 *
 * @author stig
 */
@ManagedBean
@ViewScoped
public class SamplesController implements Serializable{

  private static final Logger logger = Logger.getLogger(SamplesController.class.
          getName());

  @ManagedProperty(value = "#{clientSessionState}")
  private ClientSessionState sessionState;

  @EJB
  private SamplecollectionFacade samplecollectionFacade;

  @EJB
  private SampleFacade sampleFacade;

  @EJB
  private FileOperations fileOps;

  @EJB
  private UserFacade userfacade;

  private boolean collectionSelected = false;
  private boolean sampleSelected = false;

  private Samplecollection selectedCollection;
  private Sample selectedSample;

  private Sample newSample = new Sample();
  private Samplecollection newCollection = new Samplecollection();

  public List<Samplecollection> getSamplecollection() {
    return samplecollectionFacade.findByStudyname(sessionState.getActiveStudyname());
  }

  public void setSessionState(ClientSessionState sessionState) {
    this.sessionState = sessionState;
  }

  public boolean isCollectionSelected() {
    return collectionSelected;
  }

  public void setCollectionSelected(boolean collectionSelected) {
    this.collectionSelected = collectionSelected;
  }

  public boolean isSampleSelected() {
    return sampleSelected;
  }

  public void setSampleSelected(boolean sampleSelected) {
    this.sampleSelected = sampleSelected;
  }

  public Samplecollection getSelectedCollection() {
    return selectedCollection;
  }

  public void setSelectedCollection(Samplecollection selectedCollection) {
    this.selectedCollection = selectedCollection;
  }

  public Sample getSelectedSample() {
    return selectedSample;
  }

  public void setSelectedSample(Sample selectedSample) {
    this.selectedSample = selectedSample;
  }

  public Sample getNewSample() {
    return newSample;
  }

  public void setNewSample(Sample newSample) {
    this.newSample = newSample;
  }

  public Samplecollection getNewCollection() {
    return newCollection;
  }

  public void setNewCollection(Samplecollection newCollection) {
    this.newCollection = newCollection;
  }

  public void selectCollection(String id) {
    Samplecollection coll = getCollection(id);
    this.collectionSelected = (coll != null);
    this.selectedCollection = coll;
    selectedSample = null;
    sampleSelected = false;
  }

  public void selectSample(String sampleId) {
    if (selectedCollection == null) {
      //should never happen
      return;
    }
    String collectionId = selectedCollection.getId();
    Samplecollection coll = getCollection(collectionId);
    for (Sample s : coll.getSampleCollection()) {
      if (s.getId().equals(sampleId)) {
        this.selectedSample = s;
        this.sampleSelected = true;
        return;
      }
    }
    this.selectedSample = null;
    this.sampleSelected = false;
  }

  private Samplecollection getCollection(String id) {
    for (Samplecollection coll : getSamplecollection()) {
      if (coll.getId().equals(id)) {
        return coll;
      }
    }
    return null;
  }

  public DualListModel<CollectionTypeStudyDesignEnum> getCollectionTypeDualList() {
    if (selectedCollection == null) {
      return new DualListModel<>();
    }
    List<CollectionTypeStudyDesignEnum> target = selectedCollection.
            getCollectionTypeList();
    List<CollectionTypeStudyDesignEnum> source = new ArrayList<>();
    for (CollectionTypeStudyDesignEnum item : CollectionTypeStudyDesignEnum.
            values()) {
      if (!target.contains(item)) {
        source.add(item);
      }
    }
    return new DualListModel<>(source, target);
  }

  public void setCollectionTypeDualList(
          DualListModel<CollectionTypeStudyDesignEnum> duallist) {
    this.selectedCollection.setCollectionTypeList(duallist.getTarget());
  }

  public DualListModel<MaterialTypeEnum> getMaterialTypeDualList() {
    if (selectedSample == null) {
      return new DualListModel<>();
    }
    List<MaterialTypeEnum> target = selectedSample.getMaterialTypeList();
    List<MaterialTypeEnum> source = new ArrayList<>();
    for (MaterialTypeEnum item : MaterialTypeEnum.
            values()) {
      if (!target.contains(item)) {
        source.add(item);
      }
    }
    return new DualListModel<>(source, target);
  }

  public void setMaterialTypeDualList(DualListModel<MaterialTypeEnum> duallist) {
    this.selectedSample.setMaterialTypeList(duallist.getTarget());
  }
  
  public DualListModel<MaterialTypeEnum> getNewSampleMaterialTypeDualList(){
    if(newSample == null){
      //should never happen, but better safe than sorry
      newSample = new Sample();
    }
    List<MaterialTypeEnum> target = new ArrayList<>();
    List<MaterialTypeEnum> source = Arrays.asList(MaterialTypeEnum.values());
    return new DualListModel<>(source, target);
  }
  
  public void setNewSampleMaterialTypeDualList(DualListModel<MaterialTypeEnum> duallist){
    this.newSample.setMaterialTypeList(duallist.getTarget());
  }

  public void updateSampleCollection() {
    try {
      samplecollectionFacade.update(selectedCollection);
      MessagesController.addInfoMessage(MessagesController.SUCCESS,
              "Samplecollection data updated.", "updateSuccess");
    } catch (EJBException e) {
      logger.log(Level.SEVERE, "Failed to update samplecollection metadata", e);
      MessagesController.addErrorMessage(MessagesController.ERROR,
              "Failed to update data.", "updateFail");
    }
  }

  public void updateSample() {
    try {
      sampleFacade.update(selectedSample);
      MessagesController.addInfoMessage(MessagesController.SUCCESS,
              "Sample data updated.", "updateSuccess");
    } catch (EJBException e) {
      logger.log(Level.SEVERE, "Failed to update sample metadata", e);
      MessagesController.addErrorMessage(MessagesController.ERROR,
              "Failed to update data.", "updateFail");
    }
  }

  public void updateAll() {
    if (selectedCollection != null) {
      updateSampleCollection();
      if (selectedSample != null) {
        updateSample();
      }
    }
  }

  public void createNewSample() {
    if (newSample == null) {
      //should never happen
      MessagesController.addErrorMessage("Error",
              "An error occurred while trying to create a new sample. Please try again.",
              "message");
      return;
    }
    newSample.setSamplecollectionId(selectedCollection);
    try {
      sampleFacade.persist(newSample);
      //To refresh the selectedCollection
      selectedCollection = samplecollectionFacade.findById(selectedCollection.
              getId());
    } catch (EJBException e) {
      MessagesController.addErrorMessage("Adding sample failed",
              "Failed to create sample", "message");
      logger.log(Level.SEVERE, "Error while persisting new sample.", e);
      return;
    }
    try {
      createSampleDir(newSample.getId());
    } catch (IOException e) {
      MessagesController.addErrorMessage("Failed to create directory structure",
              "An error occurred while creating the directory structure for this sample. A database record has been added however.",
              "message");
      logger.log(Level.SEVERE,
              "Error while creating sample directory structure.", e);
    }
    MessagesController.addInfoMessage("Success", "Sample has been added",
            "message");
    selectSample(newSample.getId());
    newSample = new Sample();
  }

  /**
     * Create a sample folder for the current study. Creates a sample folder and
     * subfolders for various common file types.
     */
  private void createSampleDir(String sampleId) throws IOException {
    //Construct path
    String path = File.separator + Constants.DIR_ROOT
            + File.separator + sessionState.getActiveStudyname()
            + File.separator + Constants.DIR_SAMPLES
            + File.separator + selectedCollection.getAcronym()
            + File.separator + sampleId;

    //create dirs in fs
    boolean success;
    //add all (sub)directories
    String[] folders = {path,
      path + File.separator + Constants.DIR_BAM,
      path + File.separator + Constants.DIR_FASTQ,
      path + File.separator + Constants.DIR_VCF};

    for (String s : folders) {
      success = fileOps.mkDir(s);
      if (!success) {
        MessagesController.addErrorMessage(MessagesController.ERROR,
                "Failed to create folder " + s + ".");
        return;
      }
    }
  }

  private void createSampleCollectionDir(String folderName) throws IOException {
    //Construct path
    String path = File.separator + Constants.DIR_ROOT
            + File.separator + sessionState.getActiveStudyname()
            + File.separator + Constants.DIR_SAMPLES
            + File.separator + folderName;

    //create dir in fs
    boolean success = fileOps.mkDir(path);
    if (!success) {
      MessagesController.addErrorMessage(MessagesController.ERROR,
              "Failed to create folder " + path + ".");
    }
  }

  public void createNewCollection() {
    if (newCollection == null) {
      //should never happen
      MessagesController.addErrorMessage("Error",
              "An error occurred while trying to create a new sample collection. Please try again.",
              "message");
      return;
    }
    try {
      newCollection.setContact(userfacade.findByEmail(getUsername()));
      newCollection.setStudy(sessionState.getActiveStudy());
      samplecollectionFacade.persist(newCollection);
    } catch (EJBException e) {
      MessagesController.addErrorMessage("Adding collection failed",
              "Failed to create collection", "message");
      logger.log(Level.SEVERE, "Error while persisting new collection.", e);
      return;
    }
    try {
      createSampleCollectionDir(newCollection.getAcronym());
    } catch (IOException e) {
      MessagesController.addErrorMessage("Failed to create directory structure",
              "An error occurred while creating the directory structure for this sample collection. A database record has been added however.",
              "message");
      logger.log(Level.SEVERE,
              "Error while creating sample collection directory.", e);
    }
    MessagesController.addInfoMessage("Success",
            "Sample collection has been added", "message");
    selectCollection(newCollection.getId());
    newCollection = new Samplecollection();
  }

  //TODO: move these to a common class
  private HttpServletRequest getRequest() {
    return (HttpServletRequest) FacesContext.getCurrentInstance().
            getExternalContext().getRequest();
  }

  private String getUsername() {
    return getRequest().getUserPrincipal().getName();
  }

}
