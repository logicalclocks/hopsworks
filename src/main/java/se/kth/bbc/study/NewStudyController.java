package se.kth.bbc.study;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
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
import se.kth.bbc.activity.ActivityFacade;
import se.kth.bbc.fileoperations.FileOperations;
import se.kth.bbc.lims.ClientSessionState;
import se.kth.bbc.lims.Constants;
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
    StudyServiceEnum.STUDY_INFO, StudyServiceEnum.ADAM};
  private static final String TEMPLATE_CUSTOM = "Custom...";
  private static final String TEMPLATE_SPARK = "Spark";
  private static final StudyServiceEnum[] SERVICES_SPARK = {
    StudyServiceEnum.SPARK};

  private StudyServiceEnum[] customServices; // Services selected by user
  private String chosenTemplate; // Chosen template: if custom, customServices is used
  private String newStudyName; //The name of the new study
  private Study study = null; //The study ultimately created

  private static final Logger logger = Logger.getLogger(
          NewStudyController.class.getName());

  @EJB
  private StudyServiceFacade studyServices;

  @EJB
  private StudyFacade studyFacade;

  @EJB
  private StudyTeamFacade studyTeamFacade;

  @EJB
  private FileOperations fileOps;

  @EJB
  private ActivityFacade activityFacade;

  @ManagedProperty(value = "#{studyManagedBean}")
  private transient StudyMB studies;

  @ManagedProperty(value = "#{clientSessionState}")
  private ClientSessionState sessionState;

  public StudyServiceEnum[] getAvailableServices() {
    return StudyServiceEnum.values();
  }

  public void setStudies(StudyMB studies) {
    this.studies = studies;
  }

  public void setSessionState(ClientSessionState sessionState) {
    this.sessionState = sessionState;
  }

  public List<String> getStudyTemplates() {
    List<String> values = new ArrayList<>(4);
    values.add(TEMPLATE_BBC);
    values.add(TEMPLATE_SPARK);
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
        String username = sessionState.getLoggedInUsername();
        Date now = new Date();
        study = new Study(newStudyName, username, now);
        //create folder structure
        mkStudyDIR(study.getName());
        logger.log(Level.FINE, "{0} - study directory created successfully.",
                study.
                getName());

        //Persist study object
        studyFacade.persistStudy(study);
        //Add the desired services
        persistServices();
        //Add the activity information
        activityFacade.
                persistActivity(ActivityFacade.NEW_STUDY, study.getName(),
                        sessionState.getLoggedInUsername());
        //update role information in study
        addStudyMaster(study.getName());
        logger.log(Level.FINE, "{0} - study created successfully.", study.
                getName());

        return loadNewStudy();
      } else {
        MessagesController.addErrorMessage(
                "A study with this name already exists!");
        logger.log(Level.SEVERE, "Study with name {0} already exists!",
                newStudyName);
        return null;
      }

    } catch (IOException e) {
      MessagesController.addErrorMessage(
              "Study folder could not be created in HDFS.");
      logger.log(Level.SEVERE, "Error creating study folder in HDFS.", e);
      return null;
    } catch (EJBException ex) {
      MessagesController.addErrorMessage(
              "Study Inode could not be created in DB.");
      logger.log(Level.SEVERE, "Error creating study Inode in DB.", ex);
      return null;
    }

  }

  //Set the study owner as study master in StudyTeam table
  private void addStudyMaster(String study_name) {

    StudyTeamPK stp = new StudyTeamPK(study_name, sessionState.
            getLoggedInUsername());
    StudyTeam st = new StudyTeam(stp);
    st.setTeamRole("Master");
    st.setTimestamp(new Date());

    try {
      studyTeamFacade.persistStudyTeam(st);
      logger.log(Level.FINE, "{0} - added the study owner as a master.",
              newStudyName);
    } catch (EJBException ejb) {
      System.out.println("Add study master failed" + ejb.getMessage());
      logger.log(Level.SEVERE,
              "{0} - adding the study owner as a master failed.", ejb.
              getMessage());
    }

  }

  //create study on HDFS
  private void mkStudyDIR(String studyName) throws IOException {

    String rootDir = Constants.DIR_ROOT;
    String studyPath = File.separator + rootDir + File.separator + studyName;
    String resultsPath = studyPath + File.separator
            + Constants.DIR_RESULTS;
    String cuneiformPath = studyPath + File.separator
            + Constants.DIR_CUNEIFORM;
    String samplesPath = studyPath + File.separator
            + Constants.DIR_SAMPLES;

    fileOps.mkDir(studyPath);
    fileOps.mkDir(resultsPath);
    fileOps.mkDir(cuneiformPath);
    fileOps.mkDir(samplesPath);
  }

  //load the necessary information for displaying the study page
  private String loadNewStudy() {
    return studies.fetchStudy(newStudyName);
  }

  private void persistServices() {
    switch (chosenTemplate) {
      case TEMPLATE_BBC:
        studyServices.persistServicesForStudy(newStudyName, SERVICES_BBC);
        break;
      case TEMPLATE_SPARK:
        studyServices.persistServicesForStudy(newStudyName, SERVICES_SPARK);
        break;
      case TEMPLATE_CUSTOM:
        studyServices.persistServicesForStudy(newStudyName, customServices);
        break;
    }
  }

}
