package se.kth.bbc.jobs.yarn;

import se.kth.bbc.jobs.JobController;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.UploadedFile;
import se.kth.bbc.lims.MessagesController;
import se.kth.bbc.study.StudyMB;

/**
 *
 * @author stig
 */
@ManagedBean
@ViewScoped
public class YarnController implements Serializable {

    private static final Logger logger = Logger.getLogger(YarnController.class.getName());

    private static final String KEY_AM_JARPATH = "appMasterJarPath";
    private static final String KEY_APP_NAME = "appname";
    private static final String KEY_ARGS = "args";
    private static final String KEY_MAIN = "mainClassName";

    private final JobController jc = new JobController();

    @ManagedProperty(value = "#{studyManagedBean}")
    private transient StudyMB study;

    @PostConstruct
    public void init() {
        try {
            jc.setBasePath(study.getStudyName(), study.getUsername());
        } catch (IOException c) {
            logger.log(Level.SEVERE, "Failed to create directory structure.", c);
            MessagesController.addErrorMessage("Failed to initialize Yarn controller. Running Yarn jobs will not work.");
        }
    }

    public String getAppMasterJarPath() {
        return jc.getFilePath(KEY_AM_JARPATH);
    }

    public void setAppName(String name) {
        jc.putVariable(KEY_APP_NAME, name);
    }

    public String getAppName() {
        return jc.getVariable(KEY_APP_NAME);
    }

    public String getArgs() {
        return jc.getVariable(KEY_ARGS);
    }

    public void setArgs(String args) {
        jc.putVariable(KEY_ARGS, args);
    }

    public void setMainClassName(String mainClass) {
        jc.putVariable(KEY_MAIN, mainClass);
    }

    public String getMainClassName() {
        return jc.getVariable(KEY_MAIN);
    }

    public void handleAMUpload(FileUploadEvent event) {
        try {
            jc.handleFileUpload(KEY_AM_JARPATH, event);
        } catch (IllegalStateException e) {
            try {
                jc.setBasePath(study.getStudyName(), study.getUsername());
                jc.handleFileUpload(KEY_AM_JARPATH, event);
            } catch (IOException c) {
                logger.log(Level.SEVERE, "Failed to create directory structure.", c);
                MessagesController.addErrorMessage("Failed to initialize Yarn controller. Running Yarn jobs will not work.");
            }
        }
    }

    public void extraFileUploads(FileUploadEvent event) {
        //TODO: make a tmp folder per user, per study, per...
        UploadedFile file = event.getFile();
        String key = file.getFileName();
        try {
            jc.handleFileUpload(key, event);
        } catch (IllegalStateException e) {
            try {
                jc.setBasePath(study.getStudyName(), study.getUsername());
                jc.handleFileUpload(key, event);
            } catch (IOException c) {
                logger.log(Level.SEVERE, "Failed to create directory structure.", c);
                MessagesController.addErrorMessage("Failed to initialize Yarn controller. Running Yarn jobs will not work.");
            }
        }
    }

    public void runJar() {
        //TODO: fix this
        Map<String, String> files = jc.getFiles();
        String appMasterJar = files.remove(KEY_AM_JARPATH);
        YarnRunner.Builder builder = new YarnRunner.Builder(appMasterJar,"appMaster.jar");
        if (!files.isEmpty()) {
            //builder.addAllLocalResourcesPaths(files);
        }
        builder.appMasterArgs(jc.getVariable(KEY_ARGS)).appMasterMainClass(jc.getVariable(KEY_MAIN));
        YarnRunner runner;
        try {
            runner = builder.build();
        } catch (IllegalStateException e) {
            logger.log(Level.SEVERE, "Could not initialize YarnRunner.", e);
            MessagesController.addErrorMessage("Failed to initialize Yarn Client");
            return;
        }
        try {
            runner.startAppMaster();
        } catch (IOException | YarnException e) {
            logger.log(Level.SEVERE, "Error while initializing AppMaster.", e);
            MessagesController.addErrorMessage("Failed to initialize AppMaster.");
            return;
        }
    }

    public void setStudy(StudyMB study) {
        this.study = study;
    }

}
