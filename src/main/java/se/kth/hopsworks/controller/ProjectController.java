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
import se.kth.bbc.activity.ActivityDetailFacade;
import se.kth.bbc.activity.ActivityFacade;
import se.kth.bbc.fileoperations.FileOperations;
import se.kth.bbc.lims.Constants;
import se.kth.bbc.lims.MessagesController;
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
import se.kth.bbc.study.services.StudyServices;
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
    private ActivityDetailFacade activityDetailFacade;
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
     * @param username the user that owns the project
     * @param services the services associated with the new project to be
     * created.
     * @throws AppException if the project name already exists.
     * @throws IOException if the DIR associated with the project could not be
     * created. For whatever reason.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void createStudy(String newStudyName, String username, List<StudyServiceEnum> services) throws AppException, IOException {
        if (!studyFacade.studyExists(newStudyName)) {
            //Create a new study object
            Date now = new Date();
            Study study = new Study(newStudyName, username, now);
            //create folder structure
            //mkStudyDIR(study.getName());
            logger.log(Level.FINE, "{0} - study directory created successfully.",
                    study.getName());

            //Persist study object
            studyFacade.persistStudy(study);
            //Add the desired services
            for (StudyServiceEnum se : services) {
                studyServicesFacade.addServiceForStudy(newStudyName, se);
            }
            //Add the activity information
            activityFacade.persistActivity(ActivityFacade.NEW_STUDY, study.getName(),
                    username);
            //update role information in study
            addStudyMaster(study.getName(), study.getUsername());
            logger.log(Level.FINE, "{0} - study created successfully.", study.getName());

        } else {
            logger.log(Level.SEVERE, "Study with name {0} already exists!", newStudyName);
            throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                    ResponseMessages.PROJECT_NAME_EXIST);
        }

    }

    //Set the study owner as study master in StudyTeam table
    private void addStudyMaster(String study_name, String userName) {
        StudyTeamPK stp = new StudyTeamPK(study_name, userName);
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
     * @param studyName to be removed
     * @param userName the user performing the operation
     * @param deleteFilesOnRemove if the associated files should be deleted
     * @return true if the study and the associated files are removed
     * successfully, and false if the associated files could not be removed.
     * @throws IOException if the hole operation failed. i.e the study is not
     * removed.
     */
    public boolean removeByName(String studyName, String userName, boolean deleteFilesOnRemove) throws IOException {
        boolean success = !deleteFilesOnRemove;
        studyFacade.removeByName(studyName);
        activityFacade.persistActivity(ActivityFacade.REMOVED_STUDY, studyName, userName);
        if (deleteFilesOnRemove) {
            String path = File.separator + Constants.DIR_ROOT + File.separator + studyName;
            success = fileOps.rmRecursive(path);
        }
        logger.log(Level.FINE, "{0} - study removed.", studyName);

        return success;
    }

    /**
     * Adds new team members to a project(study) - bulk persist
     *
     * @param studyName the project(study) that new members are going to be
     * added to.
     * @param user that is adding the team members.
     * @param studyTeams
     * @return a list of user names that could not be added to the project team
     * list.
     */
    public List<String> addMembers(String studyName, String user, List<StudyTeam> studyTeams) {
        List<String> failedList = new ArrayList<>();
        for (StudyTeam studyTeam : studyTeams) {
            try {
                if (!studyTeam.getStudyTeamPK().getTeamMember().equals(user)) {
                    studyTeamFacade.persistStudyTeam(studyTeam);
                    logger.log(Level.FINE, "{0} - member added to study : {1}.",
                            new Object[]{studyTeam.getStudyTeamPK().getTeamMember(), studyName});
                    activityFacade.persistActivity(ActivityFacade.NEW_MEMBER + studyTeam.getStudyTeamPK().getTeamMember()
                            + " ", studyName, user);
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
     * @param name of the project
     * @return project DTO that contains team members and services
     */
    public ProjectDTO getProjectByName(String name) throws AppException {
        Study study = studyFacade.findByName(name);
        if (study == null) {
            throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                    ResponseMessages.PROJECT_NOT_FOUND);
        }
        List<StudyTeam> studyTeam = studyTeamFacade.findByStudyName(name);
        List<StudyServiceEnum> studyServices = studyServicesFacade.findEnabledServicesForStudy(name);
        List<String> services = new ArrayList<>();
        for (StudyServiceEnum s : studyServices) {
            services.add(s.toString());
        }
        return new ProjectDTO(study, services, studyTeam);
    }

    /**
     * Deletes a member from a project
     *
     * @param studyName
     * @param user
     * @param toRemoveEmail
     */
    public void deleteMemberFromTeam(String studyName, String user, String toRemoveEmail) {
        studyTeamFacade.removeStudyTeam(studyName, toRemoveEmail);
        activityFacade.persistActivity(ActivityFacade.REMOVED_MEMBER + toRemoveEmail, studyName, user);
    }
}
