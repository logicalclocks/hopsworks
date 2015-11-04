package se.kth.bbc.project;

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
import org.apache.commons.io.FileUtils;
import se.kth.bbc.activity.ActivityFacade;
import se.kth.bbc.fileoperations.FileOperations;
import se.kth.bbc.lims.ClientSessionState;
import se.kth.bbc.lims.MessagesController;
import se.kth.bbc.project.services.ProjectServiceEnum;
import se.kth.bbc.project.services.ProjectServiceFacade;
import se.kth.hopsworks.util.ConfigFileGenerator;
import se.kth.hopsworks.util.Settings;

/**
 *
 * @author stig
 */
@ManagedBean
@ViewScoped
public class NewProjectController implements Serializable {

  private static final String TEMPLATE_BBC = "Biobank";
  private static final ProjectServiceEnum[] SERVICES_BBC = {
    ProjectServiceEnum.ZEPPELIN,
    ProjectServiceEnum.JOBS};
  private static final String TEMPLATE_CUSTOM = "Custom...";
//  private static final String TEMPLATE_SPARK = "Spark";
//  private static final ProjectServiceEnum[] SERVICES_SPARK = {
//    ProjectServiceEnum.SPARK};
  private ProjectServiceEnum[] customServices; // Services selected by user
  private String chosenTemplate; // Chosen template: if custom, customServices is used
  private String newProjectName; //The name of the new project
  private Project project = null; //The project ultimately created

  private static final Logger logger = Logger.getLogger(
      NewProjectController.class.getName());

  @EJB
  private ProjectServiceFacade projectServices;

  @EJB
  private ProjectFacade projectFacade;

  @EJB
  private ProjectTeamFacade projectTeamFacade;

  @EJB
  private FileOperations fileOps;

  @EJB
  private ActivityFacade activityFacade;

  @EJB
  private Settings settings;

  @ManagedProperty(value = "#{projectManagedBean}")
  private transient ProjectMB studies;

  @ManagedProperty(value = "#{clientSessionState}")
  private ClientSessionState sessionState;

  public ProjectServiceEnum[] getAvailableServices() {
    return ProjectServiceEnum.values();
  }

  public void setStudies(ProjectMB studies) {
    this.studies = studies;
  }

  public void setSessionState(ClientSessionState sessionState) {
    this.sessionState = sessionState;
  }

  public List<String> getProjectTemplates() {
    List<String> values = new ArrayList<>(4);
    values.add(TEMPLATE_BBC);
//    values.add(TEMPLATE_SPARK);
    values.add(TEMPLATE_CUSTOM);
    return values;
  }

  public void setCustomServices(ProjectServiceEnum[] services) {
    this.customServices = services;
  }

  public ProjectServiceEnum[] getCustomServices() {
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

  public void setNewProjectName(String name) {
    this.newProjectName = name;
  }

  public String getNewProjectName() {
    return newProjectName;
  }

  /**
   * Get the current username from session and sets it as the creator of the project, and also adding a record to the
   * ProjectTeam table for setting the role as master for within project.
   *
   * @return
   */
  public String createProject() {
    //TODO: make this method into one big transaction!
    try {
      if (!projectFacade.projectExists(newProjectName)) {
        //Create a new project object
        Date now = new Date();
        project = new Project(newProjectName, sessionState.getLoggedInUser(),
            now);
        //create folder structure
        mkProjectDIR(project.getName());

        logger.log(Level.FINE, "{0} - project directory created successfully.",
            project.
            getName());

        //Persist project object
        projectFacade.persistProject(project);
        //Add the desired services
        persistServices();
        //Add the activity information
        activityFacade.
            persistActivity(ActivityFacade.NEW_PROJECT, project,
                sessionState.getLoggedInUsername());
        //update role information in project
        addProjectMaster(project.getId());
        logger.log(Level.FINE, "{0} - project created successfully.", project.
            getName());

        return loadNewProject(project);
      } else {
        MessagesController.addErrorMessage(
            "A project with this name already exists!");
        logger.log(Level.SEVERE, "Project with name {0} already exists!",
            newProjectName);
        return null;
      }

    } catch (IOException e) {
      MessagesController.addErrorMessage(
          "Project folder could not be created in HDFS.");
      logger.log(Level.SEVERE, "Error creating project folder in HDFS.", e);
      return null;
    } catch (EJBException ex) {
      MessagesController.addErrorMessage(
          "Project could not be created in DB.");
      logger.log(Level.SEVERE, "Error creating project in DB.", ex);
      return null;
    }

  }

  //Set the project owner as project master in ProjectTeam table
  private void addProjectMaster(Integer projectId) {
    ProjectTeamPK stp = new ProjectTeamPK(projectId, sessionState.
        getLoggedInUsername());
    ProjectTeam st = new ProjectTeam(stp);
    st.setTeamRole("Master");
    st.setTimestamp(new Date());

    try {
      projectTeamFacade.persistProjectTeam(st);
      logger.log(Level.FINE, "{0} - added the project owner as a master.",
          newProjectName);
    } catch (EJBException ejb) {
      logger.log(Level.SEVERE,
          "{0} - adding the project owner as a master failed.", ejb.
          getMessage());
    }
  }

  //create project on HDFS
  private void mkProjectDIR(String projectName) throws IOException {

    String rootDir = Settings.DIR_ROOT;
    String projectPath = File.separator + rootDir + File.separator + projectName;
    String resultsPath = projectPath + File.separator
        + Settings.DIR_RESULTS;
    String cuneiformPath = projectPath + File.separator
        + Settings.DIR_CUNEIFORM;
    String samplesPath = projectPath + File.separator
        + Settings.DIR_SAMPLES;

    mkProjectLocalZeppelin(project.getName());

    fileOps.mkDir(projectPath);
    fileOps.mkDir(resultsPath);
    fileOps.mkDir(cuneiformPath);
    fileOps.mkDir(samplesPath);
  }

  //load the necessary information for displaying the project page
  private String loadNewProject(Project project) {
    return studies.fetchProject(project);
  }

  private void persistServices() {
    switch (chosenTemplate) {
      case TEMPLATE_BBC:
        projectServices.persistServicesForProject(project, SERVICES_BBC);
        break;
//      case TEMPLATE_SPARK:
//        projectServices.persistServicesForProject(project, SERVICES_BBC);
//        break;
      case TEMPLATE_CUSTOM:
        projectServices.persistServicesForProject(project, customServices);
        break;
    }
  }

  private void mkProjectLocalZeppelin(String projectName) throws IOException {

    String projectPath
        = settings.getZeppelinDir() + File.separator + "projects" + File.separator + projectName;
    try {
      FileUtils.deleteDirectory(new File(projectPath));
      logger.log(Level.INFO, "Removed existing zeppelin directory at: {0}", projectPath);
    } catch (IOException ex) {
      // do nothing - directory didnt exist, as expected.
    }
    String notebooksPath = projectPath + File.separator + "notebooks";
    String configPath = projectPath + File.separator + "conf";
    String interpretersPath = projectPath + File.separator + "interpreters";
    String zeppelinSitePath = configPath + File.separator + "zeppelin_site.xml";
    String zeppelinEnvPath = configPath + File.separator + "zeppelin_env.sh";
    String interpreterPath = interpretersPath + File.separator + "interpreter.json";
    String runPath = projectPath + File.separator + "run";

    if (ConfigFileGenerator.mkdirs(projectPath) == false) {
      throw new IOException("Could not create project-specific local directory for Zeppelin at; " + projectPath);
    }
    if (ConfigFileGenerator.mkdirs(notebooksPath) == false) {
      throw new IOException("Could not create project-specific local directory for Zeppelin at; " + notebooksPath);
    }
    if (ConfigFileGenerator.mkdirs(configPath) == false) {
      throw new IOException("Could not create project-specific local directory for Zeppelin at; " + configPath);
    }
    if (ConfigFileGenerator.mkdirs(runPath) == false) {
      throw new IOException("Could not create project-specific local directory for Zeppelin at; " + runPath);
    }

    StringBuilder zeppelinConfig = ConfigFileGenerator.instantiateFromTemplate(
        ConfigFileGenerator.ZEPPELIN_CONFIG_TEMPLATE,
        "hadoop_dir", settings.getHadoopDir(),
        "spark_dir", settings.getSparkDir()
    );
    ConfigFileGenerator.createConfigFile(new File(zeppelinSitePath), zeppelinConfig.toString());

    StringBuilder zeppelinEnv = ConfigFileGenerator.instantiateFromTemplate(
        ConfigFileGenerator.ZEPPELIN_ENV_TEMPLATE,
        "hadoop_dir", settings.getHadoopDir(),
        "extra_jars", "",
        "zeppelin_dir", settings.getZeppelinDir(),
        "spark_dir", settings.getSparkDir()
    );
    ConfigFileGenerator.createConfigFile(new File(zeppelinEnvPath), zeppelinEnv.toString());

    StringBuilder interpreter = ConfigFileGenerator.instantiateFromTemplate(
        ConfigFileGenerator.INTERPRETER_TEMPLATE,
        "", "");
    ConfigFileGenerator.createConfigFile(new File(interpreterPath), interpreter.toString());

  }

}
