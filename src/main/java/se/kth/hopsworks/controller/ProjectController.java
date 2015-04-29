/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.hopsworks.controller;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.Response;
import se.kth.bbc.activity.Activity;
import se.kth.bbc.activity.ActivityFacade;
import se.kth.bbc.fileoperations.FileOperations;
import se.kth.bbc.lims.Constants;
import se.kth.bbc.security.ua.UserManager;
import se.kth.bbc.security.ua.model.User;
import se.kth.bbc.study.Study;
import se.kth.bbc.study.StudyFacade;
import se.kth.bbc.study.StudyRoleTypes;
import se.kth.bbc.study.StudyTeam;
import se.kth.bbc.study.StudyTeamFacade;
import se.kth.bbc.study.StudyTeamPK;
import se.kth.bbc.study.fb.Inode;
import se.kth.bbc.study.fb.InodeFacade;
import se.kth.bbc.study.services.StudyServiceEnum;
import se.kth.bbc.study.services.StudyServiceFacade;
import se.kth.hopsworks.rest.AppException;

/**
 * @author André<amore@kth.se>
 * @author Ermias<ermiasg@kth.se>
 */
@Stateless
public class ProjectController {

  private final static Logger logger = Logger.getLogger(ProjectController.class.
          getName());
  @EJB
  private StudyFacade studyFacade;
  @EJB
  private StudyTeamFacade studyTeamFacade;
  @EJB
  private UserManager userBean;
  @EJB
  private ActivityFacade activityFacade;
  @EJB
  private FileOperations fileOps;
  @EJB
  private StudyServiceFacade studyServicesFacade;
  @EJB
  private InodeFacade inodes;

  /**
   * Creates a new project(study), the related DIR, the different services in
   * the project, and the master of the project.
   *
   * @param newStudyName the name of the new project(study)
   * @param email
   * @return
   * @throws AppException if the project name already exists.
   * @throws IOException if the DIR associated with the project could not be
   * created. For whatever reason.
   */
  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  //this needs to be an atomic operation (all or nothing) REQUIRES_NEW 
  //will make sure a new transaction is created even if this method is
  //called from within a transaction.
  public Study createStudy(String newStudyName, String email) throws
          AppException, IOException {
    User user = userBean.getUserByEmail(email);
    //if there is no project by the same name for this user
    if (!studyFacade.studyExistsForOwner(newStudyName, user)) {
      //Create a new study object
      Date now = new Date();
      Study study = new Study(newStudyName, user, now);
      //create folder structure
      //mkStudyDIR(study.getName());
      logger.log(Level.FINE, "{0} - study directory created successfully.",
              study.getName());

      //Persist study object
      studyFacade.persistStudy(study);
      studyFacade.flushEm();//flushing it to get study id
      //Add the activity information     
      logActivity(ActivityFacade.NEW_STUDY,
            ActivityFacade.FLAG_STUDY, user, study);
      //update role information in study
      addStudyMaster(study.getId(), user.getEmail());
      logger.log(Level.FINE, "{0} - study created successfully.", study.
              getName());
      return study;
    } else {
      logger.log(Level.SEVERE, "Study with name {0} already exists!",
              newStudyName);
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.PROJECT_NAME_EXIST);
    }

  }
  
  /**
   *
   * @param id study id
   * @return Study
   */
  public Study findStudyById(Integer id) throws AppException {
    
    Study study = studyFacade.find(id);
     if( study != null){
       return study;
     } else {
     throw new AppException(Response.Status.NOT_FOUND.getStatusCode(),
              ResponseMessages.PROJECT_NOT_FOUND);
     }
  }

  public void addServices(Study study, List<StudyServiceEnum> services) {
    //Add the desired services
    for (StudyServiceEnum se : services) {
      studyServicesFacade.addServiceForStudy(study, se);
    }
  }

  //Set the study owner as study master in StudyTeam table
  private void addStudyMaster(Integer study_id, String userName) {
    StudyTeamPK stp = new StudyTeamPK(study_id, userName);
    StudyTeam st = new StudyTeam(stp);
    st.setTeamRole("Master");
    st.setTimestamp(new Date());
    studyTeamFacade.persistStudyTeam(st);
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

  /**
   * Remove a study and optionally all associated files.
   *
   * @param studyID to be removed
   * @param email
   * @param deleteFilesOnRemove if the associated files should be deleted
   * @return true if the study and the associated files are removed
   * successfully, and false if the associated files could not be removed.
   * @throws IOException if the hole operation failed. i.e the study is not
   * removed.
   * @throws AppException if the project could not be found.
   */
  public boolean removeByName(Integer studyID, String email,
          boolean deleteFilesOnRemove) throws IOException, AppException {
    boolean success = !deleteFilesOnRemove;
    User user = userBean.getUserByEmail(email);
    Study study = studyFacade.find(studyID);
    if (study == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.PROJECT_NOT_FOUND);
    }
    studyFacade.remove(study);

    logActivity(ActivityFacade.REMOVED_STUDY,
            ActivityFacade.FLAG_STUDY, user, study);

    if (deleteFilesOnRemove) {
      String path = File.separator + Constants.DIR_ROOT + File.separator
              + study.getName();
      success = fileOps.rmRecursive(path);
    }
    logger.log(Level.FINE, "{0} - study removed.", study.getName());

    return success;
  }

  /**
   * Adds new team members as researcher to a project(study) - bulk persist
   *
   * @param study
   * @param email
   * @param studyTeams
   * @return a list of user names that could not be added to the project team
   * list.
   */
  public List<String> addMembers(Study study, String email,
          List<StudyTeam> studyTeams) {
    List<String> failedList = new ArrayList<>();
    User user = userBean.getUserByEmail(email);
    for (StudyTeam studyTeam : studyTeams) {
      try {
        if (!studyTeam.getStudyTeamPK().getTeamMember().equals(user.getEmail())) {
          studyTeam.getStudyTeamPK().setStudyId(study.getId());
          studyTeam.setTeamRole(StudyRoleTypes.RESEARCHER.getTeam());
          studyTeam.setTimestamp(new Date());
          studyTeamFacade.persistStudyTeam(studyTeam);
          logger.log(Level.FINE, "{0} - member added to study : {1}.",
                  new Object[]{studyTeam.getStudyTeamPK().getTeamMember(),
                    study.getName()});

          logActivity(ActivityFacade.NEW_MEMBER,
            ActivityFacade.FLAG_STUDY, user, study);
        }
      } catch (EJBException ejb) {
        failedList.add(studyTeam.getStudyTeamPK().getTeamMember());
        logger.log(Level.SEVERE, "Adding  team member {0} to study failed",
                studyTeam.getStudyTeamPK().getTeamMember());
        return null;
      }
    }
    return failedList;
  }

  private boolean isStudyPresentInHdfs(String studyname) {
    Inode root = inodes.getStudyRoot(studyname);
    if (root == null) {
      logger.log(Level.INFO, "Study folder not found in HDFS for study {0} .",
              studyname);
      return false;
    }
    return true;
  }

  /**
   * Project info as data transfer object that can be sent to the user.
   * <p>
   * @param studyID of the project
   * @return project DTO that contains team members and services
   * @throws se.kth.hopsworks.rest.AppException
   */
  public ProjectDTO getStudyByID(Integer studyID) throws AppException {
    Study study = studyFacade.find(studyID);
    if (study == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.PROJECT_NOT_FOUND);
    }
    List<StudyTeam> studyTeam = studyTeamFacade.findMembersByStudy(study);
    List<StudyServiceEnum> studyServices = studyServicesFacade.
            findEnabledServicesForStudy(study);
    List<String> services = new ArrayList<>();
    for (StudyServiceEnum s : studyServices) {
      services.add(s.toString());
    }
    return new ProjectDTO(study, services, studyTeam);
  }

  /**
   * Deletes a member from a project
   *
   * @param study
   * @param email
   * @param toRemoveEmail
   */
  public void deleteMemberFromTeam(Study study, String email,
          String toRemoveEmail) {
    studyTeamFacade.removeStudyTeam(study, toRemoveEmail);
    User user = userBean.getUserByEmail(email);
    logActivity(ActivityFacade.REMOVED_MEMBER + toRemoveEmail,
            ActivityFacade.FLAG_STUDY, user, study);
  }

  /**
   * Retrieves all the study teams that a user have a role
   * <p>
   * @param email of the user
   * @return a list of study team
   */
  public List<StudyTeam> findStudyByUser(String email) {
    User user = userBean.getUserByEmail(email);
    return studyTeamFacade.findByMember(user);
  }

  /**
   * Logs activity
   * <p>
   * @param activityPerformed the description of the operation performed
   * @param flag on what the operation was performed(FLAG_STUDY, FLAG_USER)
   * @param performedBy the user that performed the operation
   * @param performedOn the project the operation was performed on.
   */
  public void logActivity(String activityPerformed, String flag,
          User performedBy, Study performedOn) {
    Date now = new Date();
    Activity activity = new Activity();
    activity.setActivity(activityPerformed);
    activity.setFlag(flag);
    activity.setStudy(performedOn);
    activity.setTimestamp(now);
    activity.setUser(performedBy);

    activityFacade.persistActivity(activity);
  }
}
