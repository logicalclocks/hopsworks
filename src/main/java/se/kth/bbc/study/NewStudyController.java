package se.kth.bbc.study;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
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
import se.kth.bbc.activity.ActivityController;
import se.kth.bbc.activity.ActivityMB;
import se.kth.bbc.fileoperations.FileOperations;
import se.kth.bbc.fileoperations.FileSystemOperations;
import se.kth.bbc.lims.MessagesController;
import se.kth.bbc.study.services.StudyServiceEnum;
import se.kth.bbc.study.services.StudyServiceFacade;

/**
 *
 * @author stig
 */
@ManagedBean
@ViewScoped
public class NewStudyController implements Serializable {

  private static final String TEMPLATE_BBC = "Biobank";
  private static final StudyServiceEnum[] SERVICES_BBC = {
    StudyServiceEnum.CUNEIFORM, StudyServiceEnum.SAMPLES,
    StudyServiceEnum.STUDY_INFO};
  private static final String TEMPLATE_FLINK = "Flink";
  private static final StudyServiceEnum[] SERVICES_FLINK = {
    StudyServiceEnum.FLINK
  };
  private static final String TEMPLATE_CUSTOM = "Custom...";

  private StudyServiceEnum[] customServices; // Services selected by user
  private String chosenTemplate; // Chosen template: if custom, customServices is used
  private String newStudyName; //The name of the new study
  private TrackStudy study = null; //The study ultimately created

  private static final Logger logger = Logger.getLogger(
          NewStudyController.class.getName());

  @EJB
  private StudyServiceFacade studyServices;

  @EJB
  private StudyFacade studyFacade;

  @EJB
  private StudyTeamController studyTeamFacade;
  
  @EJB
  private FileOperations fileOps;

  //TODO: restructure, only use EJBs here, move methods from controller to mb to controller and rename to facade
  @ManagedProperty(value = "#{activityBean}")
  private transient ActivityMB activity;
  
  @ManagedProperty(value = "#{studyManagedBean}")
  private transient StudyMB studies;

  public StudyServiceEnum[] getAvailableServices() {
    return StudyServiceEnum.values();
  }

  public void setActivity(ActivityMB activity) {
    this.activity = activity;
  }
  
  public void setStudies(StudyMB studies){
    this.studies = studies;
  }

  public List<String> getStudyTemplates() {
    List<String> values = new ArrayList<>(4);
    values.add(TEMPLATE_BBC);
    values.add(TEMPLATE_FLINK);
    values.add(TEMPLATE_CUSTOM);
    return values;
  }

  public void setCustomServices(StudyServiceEnum[] services) {
    this.customServices = services;
  }

  public StudyServiceEnum[] getCustomServices() {
    return customServices;
  }

  public void setChosenTemplate(String template) {
    this.chosenTemplate = template;
  }

  public String getChosenTemplate() {
    return chosenTemplate;
  }

  public boolean shouldShowSelector() {
    return TEMPLATE_CUSTOM.equals(this.chosenTemplate);
  }

  public void setNewStudyName(String name) {
    this.newStudyName = name;
  }

  public String getNewStudyName() {
    return newStudyName;
  }

  /**
   * Get the current username from session and sets it as the creator of the
   * study, and also adding a record to the StudyTeam table for setting the
   * role as master for within study.
   *
   * @return
   */
  public String createStudy() {
    //TODO: make this method into one big transaction!
    try {
      if (!studyFacade.findStudy(newStudyName)) {
        //Create a new study object
        String username = getUsername();
        Date now = new Date();
        study = new TrackStudy(newStudyName, username, now);
        studyFacade.persistStudy(study);
        //Add the desired services
        persistServices();
        //Add the activity information
        activity.addActivity(ActivityController.NEW_STUDY, study.getName(),
                ActivityController.FLAG_STUDY);
        //update role information in study
        addStudyMaster(study.getName());
        //create folder structure
        mkStudyDIR(study.getName());
        logger.log(Level.INFO, "{0} - study was created successfully.", study.
                getName());

        return loadNewStudy();
      } else {
        MessagesController.addErrorMessage("A study with this name already exists!");
        logger.log(Level.SEVERE, "Study with name {0} already exists!", newStudyName);
        return null;
      }

    } catch (IOException | EJBException | URISyntaxException exp) {
      MessagesController.addErrorMessage("Study could not be created.");
      logger.log(Level.SEVERE, "Study was not created!");
      return null;
    }

  }

  private HttpServletRequest getRequest() {
    return (HttpServletRequest) FacesContext.getCurrentInstance().
            getExternalContext().getRequest();
  }

  private String getUsername() {
    return getRequest().getUserPrincipal().getName();
  }

  //Set the study owner as study master in StudyTeam table
  private void addStudyMaster(String study_name) {

    StudyTeamPK stp = new StudyTeamPK(study_name, getUsername());
    StudyTeam st = new StudyTeam(stp);
    st.setTeamRole("Master");
    st.setTimestamp(new Date());

    try {
      studyTeamFacade.persistStudyTeam(st);
      logger.log(Level.INFO, "{0} - added the study owner as a master.",
              newStudyName);
    } catch (EJBException ejb) {
      System.out.println("Add study master failed" + ejb.getMessage());
      logger.log(Level.SEVERE,
              "{0} - adding the study owner as a master failed.", ejb.
              getMessage());
    }

  }

  //create study on HDFS
  private void mkStudyDIR(String studyName) throws IOException,
          URISyntaxException {

    String rootDir = FileSystemOperations.DIR_ROOT;
    String studyPath = File.separator + rootDir + File.separator + studyName;
    String resultsPath = studyPath + File.separator
            + FileSystemOperations.DIR_RESULTS;
    String cuneiformPath = studyPath + File.separator
            + FileSystemOperations.DIR_CUNEIFORM;
    String samplesPath = studyPath + File.separator
            + FileSystemOperations.DIR_SAMPLES;

    fileOps.mkDir(studyPath);
    fileOps.mkDir(resultsPath);
    fileOps.mkDir(cuneiformPath);
    fileOps.mkDir(samplesPath);
  }
  
  
  //load the necessary information for displaying the study page
  private String loadNewStudy(){
    studies.setStudyName(newStudyName);
    studies.setCreator(getUsername());
    return studies.checkAccess();
  }
  
  private void persistServices(){
    switch(chosenTemplate){
      case TEMPLATE_BBC:
        studyServices.persistServicesForStudy(newStudyName, SERVICES_BBC);
        break;
      case TEMPLATE_FLINK:
        studyServices.persistServicesForStudy(newStudyName, SERVICES_FLINK);
        break;
      case TEMPLATE_CUSTOM:
        studyServices.persistServicesForStudy(newStudyName, customServices);
        break;
    }
  }

}
