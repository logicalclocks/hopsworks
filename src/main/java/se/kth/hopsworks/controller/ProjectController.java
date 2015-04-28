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
import se.kth.bbc.security.ua.model.User;
import se.kth.bbc.study.Study;
import se.kth.bbc.study.StudyFacade;
import se.kth.bbc.study.StudyTeam;
import se.kth.bbc.study.StudyTeamFacade;
import se.kth.bbc.study.StudyTeamPK;
import se.kth.bbc.study.fb.Inode;
import se.kth.bbc.study.fb.InodeFacade;
import se.kth.bbc.study.privacy.StudyPrivacyManager;
import se.kth.bbc.study.services.StudyServiceEnum;
import se.kth.bbc.study.services.StudyServiceFacade;
import se.kth.hopsworks.rest.AppException;

/**
 * @author André<amore@kth.se>
 * @author Ermias<ermiasg@kth.se>
 */
@Stateless
public class ProjectController {

    private final static Logger logger = Logger.getLogger(ProjectController.class.getName());
    @EJB
    private StudyFacade studyFacade;
    @EJB
    private StudyTeamFacade studyTeamFacade;
    @EJB
    private ActivityFacade activityFacade;
    @EJB
    private se.kth.bbc.activity.ActivityController activityController;
    @EJB
    private FileOperations fileOps;
    @EJB
    private StudyServiceFacade studyServicesFacade;
    @EJB
    private InodeFacade inodes;
    @EJB
    private StudyPrivacyManager privacyManager;

    /**
     * Creates a new project(study), the related DIR, the different services in
     * the project, and the master of the project.
     *
     * @param newStudyName the name of the new project(study)
     * @param user the user that owns the project
     * @param services the services associated with the new project to be
     * created.
     * @throws AppException if the project name already exists.
     * @throws IOException if the DIR associated with the project could not be
     * created. For whatever reason.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void createStudy(String newStudyName, User user, List<StudyServiceEnum> services) throws AppException, IOException {
        if (!studyFacade.studyExists(newStudyName)) {
            //Create a new study object
            Date now = new Date();
            Study study = new Study(newStudyName, user, now);
            //create folder structure
            //mkStudyDIR(study.getName());
            logger.log(Level.FINE, "{0} - study directory created successfully.",
                    study.getName());

            //Persist study object
            studyFacade.persistStudy(study);
            //Add the desired services
            for (StudyServiceEnum se : services) {
                studyServicesFacade.addServiceForStudy(study, se);
            }
            //Add the activity information
            Activity activity = new Activity();
            activity.setFlag(ActivityFacade.NEW_STUDY);
            activity.setUser(user);
            activity.setStudy(study);
            activity.setTimestamp(now);
            
            activityFacade.persistActivity(activity);
            
            //update role information in study
            addStudyMaster(study.getId(), user.getEmail());
            logger.log(Level.FINE, "{0} - study created successfully.", study.getName());

        } else {
            logger.log(Level.SEVERE, "Study with name {0} already exists!", newStudyName);
            throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                    ResponseMessages.PROJECT_NAME_EXIST);
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
     * @param user the user performing the operation
     * @param deleteFilesOnRemove if the associated files should be deleted
     * @return true if the study and the associated files are removed
     * successfully, and false if the associated files could not be removed.
     * @throws IOException if the hole operation failed. i.e the study is not
     * removed.
     */
    public boolean removeByName(Integer studyID, User user, boolean deleteFilesOnRemove) throws IOException {
        boolean success = !deleteFilesOnRemove;
        
        Study study = studyFacade.find(studyID);
        studyFacade.remove(study);
        Date now = new Date();
        
        Activity activity = new Activity();
        activity.setFlag(ActivityFacade.REMOVED_STUDY);
        activity.setStudy(study);
        activity.setTimestamp(now);
        activity.setUser(user);
                
        activityFacade.persistActivity(activity);
        
        if (deleteFilesOnRemove) {
            String path = File.separator + Constants.DIR_ROOT + File.separator + study.getName();
            success = fileOps.rmRecursive(path);
        }
        logger.log(Level.FINE, "{0} - study removed.", study.getName());

        return success;
    }

    /**
     * Adds new team members to a project(study) - bulk persist
     *
     * @param studyID the project(study) that new members are going to be
     * added to.
     * @param user that is adding the team members.
     * @param studyTeams 
     * @return a list of user names that could not be added to the project team
     * list.
     */
    public List<String> addMembers(Integer studyID, User user, List<StudyTeam> studyTeams) {
        List<String> failedList = new ArrayList<>();
        Study study = studyFacade.find(studyID);
        
        for (StudyTeam studyTeam : studyTeams) {
            try {
                if (!studyTeam.getStudyTeamPK().getTeamMember().equals(user)) {
                    studyTeamFacade.persistStudyTeam(studyTeam);
                    logger.log(Level.FINE, "{0} - member added to study : {1}.", new Object[]{studyTeam.getStudyTeamPK().getTeamMember(), study.getName()});
                    
                    Activity activity = new Activity();
                    Date now = new Date();
                    
                    activity.setFlag(ActivityFacade.NEW_MEMBER);
                    activity.setStudy(study);
                    activity.setTimestamp(now);
                    activity.setUser(user);
                    
                    activityFacade.persistActivity(activity);
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
     *
     * @param studyID of the project
     * @return project DTO that contains team members and services
     */
    public ProjectDTO getStudyByID(Integer studyID) throws AppException {
        Study study = studyFacade.find(studyID);
        if (study == null) {
            throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                    ResponseMessages.PROJECT_NOT_FOUND);
        }
        List<StudyTeam> studyTeam = studyTeamFacade.findMembersByStudy(study);
        List<StudyServiceEnum> studyServices = studyServicesFacade.findEnabledServicesForStudy(study);
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
     * @param user
     * @param toRemoveEmail
     */
    public void deleteMemberFromTeam(Study study, User user, String toRemoveEmail) {
        studyTeamFacade.removeStudyTeam(study.getName(), toRemoveEmail);
        
        Date now = new Date();
        Activity activity = new Activity();
        activity.setFlag(ActivityFacade.REMOVED_MEMBER + toRemoveEmail);
        activity.setStudy(study);
        activity.setTimestamp(now);
        activity.setUser(user);
        
        activityFacade.persistActivity(activity);
    }
}
