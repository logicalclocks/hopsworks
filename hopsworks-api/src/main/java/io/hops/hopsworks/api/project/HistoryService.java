package io.hops.hopsworks.api.project;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import org.json.JSONObject;
import io.hops.hopsworks.common.dao.jobs.JobsHistory;
import io.hops.hopsworks.common.dao.jobs.JobsHistoryFacade;
import io.hops.hopsworks.common.dao.jobhistory.YarnAppHeuristicResultDetailsFacade;
import io.hops.hopsworks.common.dao.jobhistory.YarnAppHeuristicResultFacade;
import io.hops.hopsworks.common.dao.jobhistory.YarnAppResult;
import io.hops.hopsworks.common.dao.jobhistory.YarnAppResultDTO;
import io.hops.hopsworks.common.dao.jobhistory.YarnAppResultFacade;
import io.hops.hopsworks.api.filter.AllowedRoles;
import io.hops.hopsworks.api.jobs.JobService;
import io.hops.hopsworks.api.util.JsonResponse;
import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.common.constants.message.ResponseMessages;
import io.hops.hopsworks.common.dao.jobs.description.JobDescriptionFacade;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.jobs.jobhistory.ConfigDetailsDTO;
import io.hops.hopsworks.common.jobs.jobhistory.JobDetailDTO;
import io.hops.hopsworks.common.jobs.jobhistory.JobHeuristicDTO;
import io.hops.hopsworks.common.jobs.jobhistory.JobHeuristicDetailsComparator;
import io.hops.hopsworks.common.jobs.jobhistory.JobHeuristicDetailsDTO;
import io.hops.hopsworks.common.jobs.jobhistory.JobProposedConfigurationDTO;
import io.hops.hopsworks.common.util.Settings;
import io.swagger.annotations.Api;

@Path("history")
@RolesAllowed({"HOPS_ADMIN", "HOPS_USER"})
@Api(value = "History Service", description = "History Service")
@TransactionAttribute(TransactionAttributeType.NEVER)
public class HistoryService {

  private static final String MEMORY_HEURISTIC_CLASS
          = "com.linkedin.drelephant.spark.heuristics.MemoryLimitHeuristic";
  private static final String TOTAL_DRIVE_MEMORY
          = "Total driver memory allocated";
  private static final String TOTAL_EXECUTOR_MEMORY
          = "Total executor memory allocated";
  private static final String TOTAL_STORAGE_MEMORY
          = "Total memory allocated for storage";

  private static final String STAGE_RUNTIME_HEURISTIC_CLASS
          = "com.linkedin.drelephant.spark.heuristics.StageRuntimeHeuristic";
  private static final String AVERAGE_STATE_FAILURE
          = "Spark average stage failure rate";
  private static final String PROBLEMATIC_STAGES = "Spark problematic stages";
  private static final String STAGE_COMPLETED = "Spark stage completed";
  private static final String STAGE_FAILED = "Spark stage failed";

  private static final String JOB_RUNTIME_HEURISTIC_CLASS
          = "com.linkedin.drelephant.spark.heuristics.JobRuntimeHeuristic";
  private static final String AVERAGE_JOB_FAILURE
          = "Spark average job failure rate";
  private static final String JOBS_COMPLETED = "Spark completed jobs number";
  private static final String JOBS_FAILED_NUMBER = "Spark failed jobs number";

  private static final String EXECUTOR_LOAD_BALANCE_CLASS
          = "com.linkedin.drelephant.spark.heuristics.ExecutorLoadHeuristic";

  private List<JobHeuristicDetailsDTO> resultsForAnalysis = new ArrayList<>();
  private JobHeuristicDetailsComparator comparator
          = new JobHeuristicDetailsComparator();

  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private YarnAppResultFacade yarnAppResultFacade;
  @EJB
  private ProjectFacade projectFacade;
  @Inject
  private JobService jobs;
  @EJB
  private JobDescriptionFacade jobFacade;
  @EJB
  private JobsHistoryFacade jobsHistoryFacade;
  @EJB
  private YarnAppHeuristicResultFacade yarnAppHeuristicResultsFacade;
  @EJB
  private YarnAppHeuristicResultDetailsFacade yarnAppHeuristicResultDetailsFacade;
  @EJB
  private Settings settings;
  @EJB
  private HdfsUsersController hdfsUsersBean;

  @GET
  @Path("all/{projectId}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.ALL})
  public Response getAllProjects(@PathParam("projectId") int projectId,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {

    Project returnProject = projectFacade.find(projectId);
    List<YarnAppResultDTO> appResultsToReturn = new ArrayList<>();
    List<JobsHistory> jobsHistories = jobsHistoryFacade.findByProjectId(
            returnProject.getId());

    for (JobsHistory jh : jobsHistories) {
      YarnAppResult appResult = yarnAppResultFacade.findAllByName(jh.getAppId());
      if (appResult != null) {
        YarnAppResultDTO appToAdd = new YarnAppResultDTO(appResult, jh.
                getExecutionDuration(), appResult.getFinishTime() - appResult.
                getStartTime());
        appToAdd.setOwnerFullName(returnProject.getOwner().getFname() + " "
                + returnProject.getOwner().getLname());
        appResultsToReturn.add(appToAdd);
      }
    }

    GenericEntity<List<YarnAppResultDTO>> yarnApps
            = new GenericEntity<List<YarnAppResultDTO>>(appResultsToReturn) {};

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            yarnApps).build();
  }

  @GET
  @Path("details/jobs/{jobId}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.ALL})
  public Response getJob(@PathParam("jobId") String jobId,
          @Context SecurityContext sc,
          @Context HttpServletRequest req,
          @HeaderParam("Access-Control-Request-Headers") String requestH) throws
          AppException {

    JsonResponse json = getJobDetailsFromDrElephant(jobId);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            json).build();
  }

  @GET
  @Path("config/jobs/{jobId}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.ALL})
  public Response getConfig(@PathParam("jobId") String jobId,
          @Context SecurityContext sc,
          @Context HttpServletRequest req,
          @HeaderParam("Access-Control-Request-Headers") String requestH) throws
          AppException {

    JobsHistory jh = jobsHistoryFacade.findByAppId(jobId);

    int yarnAppHeuristicIdMemory = yarnAppHeuristicResultsFacade.
            searchByIdAndClass(jobId, MEMORY_HEURISTIC_CLASS);

    ConfigDetailsDTO configDTO = new ConfigDetailsDTO(jobId);

    configDTO.setTotalDriverMemory(yarnAppHeuristicResultDetailsFacade.
            searchByIdAndName(yarnAppHeuristicIdMemory, TOTAL_DRIVE_MEMORY));
    String totalExMemory = yarnAppHeuristicResultDetailsFacade.
            searchByIdAndName(yarnAppHeuristicIdMemory, TOTAL_EXECUTOR_MEMORY);
    String[] splitTotalExMemory = splitExecutorMemory(totalExMemory);

    configDTO.setAmMemory(jh.getAmMemory());
    configDTO.setAmVcores(jh.getAmVcores());

    configDTO.setTotalExecutorMemory(splitTotalExMemory[0]);
    configDTO.setExecutorMemory(convertGBtoMB(splitTotalExMemory[1]));
    configDTO.setNumberOfExecutors(Integer.parseInt(splitTotalExMemory[2]));

    configDTO.setClassName(jh.getClassName());
    configDTO.setJarFile(jh.getJarFile());
    configDTO.setArguments(jh.getArguments());
    configDTO.setBlocksInHdfs(jh.getInputBlocksInHdfs());

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            configDTO).build();
  }

  @POST
  @Path("heuristics")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
  public Response Heuristics(JobDetailDTO jobDetailDTO,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {

    JobHeuristicDTO jobsHistoryResult = jobsHistoryFacade.
            searchHeuristicRusults(jobDetailDTO);

    Iterator<String> jobIt = jobsHistoryResult.getSimilarAppIds().iterator();

    while (jobIt.hasNext()) {
      String appId = jobIt.next();
      JsonResponse json = getJobDetailsFromDrElephant(appId);
      JobsHistory jobsHistory = jobsHistoryFacade.findByAppId(appId);

      // Check if Dr.Elephant can find the Heuristic details for this application.
      // If the status is FAILED then continue to the next iteration.
      if (json.getStatus() == "FAILED") {
        continue;
      }

      StringBuilder jsonString = (StringBuilder) json.getData();
      JSONObject jsonObj = new JSONObject(jsonString.toString());

      String totalSeverity = jsonObj.get("severity").toString();

      int yarnAppHeuristicIdMemory = yarnAppHeuristicResultsFacade.
              searchByIdAndClass(appId, MEMORY_HEURISTIC_CLASS);
      int yarnAppHeuristicIdStage = yarnAppHeuristicResultsFacade.
              searchByIdAndClass(appId, STAGE_RUNTIME_HEURISTIC_CLASS);
      int yarnAppHeuristicIdJob = yarnAppHeuristicResultsFacade.
              searchByIdAndClass(appId, JOB_RUNTIME_HEURISTIC_CLASS);

      JobHeuristicDetailsDTO jhD = new JobHeuristicDetailsDTO(appId,
              totalSeverity);
      jhD.setTotalDriverMemory(yarnAppHeuristicResultDetailsFacade.
              searchByIdAndName(yarnAppHeuristicIdMemory, TOTAL_DRIVE_MEMORY));
      String totalExMemory = yarnAppHeuristicResultDetailsFacade.
              searchByIdAndName(yarnAppHeuristicIdMemory, TOTAL_EXECUTOR_MEMORY);
      String[] splitTotalExMemory = splitExecutorMemory(totalExMemory);

      jhD.setAmMemory(jobsHistory.getAmMemory());
      jhD.setAmVcores(jobsHistory.getAmVcores());
      jhD.setExecutionTime(jobsHistory.getExecutionDuration());

      jhD.setTotalExecutorMemory(splitTotalExMemory[0]);
      jhD.setExecutorMemory(convertGBtoMB(splitTotalExMemory[1]));
      jhD.setNumberOfExecutors(Integer.parseInt(splitTotalExMemory[2]));

      jhD.setMemorySeverity(yarnAppHeuristicResultsFacade.searchForSeverity(
              appId, MEMORY_HEURISTIC_CLASS));
      jhD.setStageRuntimeSeverity(yarnAppHeuristicResultsFacade.
              searchForSeverity(appId, STAGE_RUNTIME_HEURISTIC_CLASS));
      jhD.setJobRuntimeSeverity(yarnAppHeuristicResultsFacade.searchForSeverity(
              appId, JOB_RUNTIME_HEURISTIC_CLASS));
      jhD.setLoadBalanceSeverity(yarnAppHeuristicResultsFacade.
              searchForSeverity(appId, EXECUTOR_LOAD_BALANCE_CLASS));

      jhD.setMemoryForStorage(yarnAppHeuristicResultDetailsFacade.
              searchByIdAndName(yarnAppHeuristicIdMemory, TOTAL_STORAGE_MEMORY));

      // JOBS
      jhD.setAverageJobFailure(yarnAppHeuristicResultDetailsFacade.
              searchByIdAndName(yarnAppHeuristicIdJob, AVERAGE_JOB_FAILURE));
      jhD.setCompletedJobsNumber(yarnAppHeuristicResultDetailsFacade.
              searchByIdAndName(yarnAppHeuristicIdJob, JOBS_COMPLETED));
      jhD.setFailedJobsNumber(yarnAppHeuristicResultDetailsFacade.
              searchByIdAndName(yarnAppHeuristicIdJob, JOBS_FAILED_NUMBER));

      // STAGE
      jhD.setAverageStageFailure(yarnAppHeuristicResultDetailsFacade.
              searchByIdAndName(yarnAppHeuristicIdStage, AVERAGE_STATE_FAILURE));
      jhD.setCompletedStages(yarnAppHeuristicResultDetailsFacade.
              searchByIdAndName(yarnAppHeuristicIdStage, STAGE_COMPLETED));
      jhD.setFailedStages(yarnAppHeuristicResultDetailsFacade.searchByIdAndName(
              yarnAppHeuristicIdStage, STAGE_FAILED));
      jhD.setProblematicStages(yarnAppHeuristicResultDetailsFacade.
              searchByIdAndName(yarnAppHeuristicIdStage, PROBLEMATIC_STAGES));

      jobsHistoryResult.addJobHeuristicDetails(jhD);
      resultsForAnalysis.add(jhD);

    }

    defaultAnalysis(jobsHistoryResult);
    premiumAnalysis(jobsHistoryResult);

    GenericEntity<JobHeuristicDTO> jobsHistory
            = new GenericEntity<JobHeuristicDTO>(jobsHistoryResult) {};

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            jobsHistory).build();
  }

  /**
   * A rest call to Dr. Elephant which returns the details for a specific
   * application
   *
   * @param jobId
   * @return
   */
  private JsonResponse getJobDetailsFromDrElephant(String jobId) {

    try {
      JsonResponse json = new JsonResponse();
      URL url = new URL(settings.getDrElephantUrl() + "/rest/job?id=" + jobId);
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      conn.setRequestMethod("GET");
      conn.setRequestProperty("Accept", "application/json");

      if (conn.getResponseCode() != 200) {
        json.setStatus("FAILED");
        json.setData("Failed : HTTP error code : " + conn.getResponseCode());
        json.setSuccessMessage(ResponseMessages.JOB_DETAILS);
        conn.disconnect();
        return json;
      }

      BufferedReader br = new BufferedReader(new InputStreamReader(
              (conn.getInputStream())));

      String output;
      StringBuilder outputBuilder = new StringBuilder();
      while ((output = br.readLine()) != null) {
        outputBuilder.append(output);
      }

      json.setData(outputBuilder);
      json.setStatus("OK");
      json.setSuccessMessage(ResponseMessages.JOB_DETAILS);
      conn.disconnect();
      return json;

    } catch (MalformedURLException e) {
    } catch (IOException e) {
    }

    return null;
  }

  /*
   * This method splits the total memory to the number of executors and per
   * executor memory
   * For example in the case of 1 GB (512 MB x 2),
   * the method will return an array of Strings with:
   * 1. The total memory (in this case 1 GB)
   * 2. Per executor memory (in this case 512 MB)
   * 3. Number of executors (in this case 2)
   */
  private String[] splitExecutorMemory(String executorMemory) {
    String[] memoryDetails = new String[3];
    String[] splitParenthesis = executorMemory.split("[\\(\\)]");
    String[] parts = splitParenthesis[1].split("x");

    memoryDetails[0] = splitParenthesis[0].trim();  // Total memory
    memoryDetails[1] = parts[0].trim();             // per executor memory
    memoryDetails[2] = parts[1].trim();             // number of executors

    return memoryDetails;
  }

  private int convertGBtoMB(String memory) {
    int memoryInMB;
    String[] splited = memory.split("\\s+");

    if (splited[1].equals("GB")) {
      memoryInMB = Integer.parseInt(splited[0]) * 1024;
    } else {
      memoryInMB = Integer.parseInt(splited[0]);
    }
    return memoryInMB;
  }

  /**
   * The default analysis tries to find the minimum resources required for an
   * application.
   *
   * @param jobsHistoryResult
   */
  private void defaultAnalysis(JobHeuristicDTO jobsHistoryResult) {
    int defaultAmMemory = 512;
    int defaultAmVcores = 1;
    int defaultNumOfExecutors = 1;
    int defaultExecutorsMemory = 1024;
    int defaultExecutorCores = 1;
    long executionDuration = 0;

    Iterator<JobHeuristicDetailsDTO> itr = resultsForAnalysis.iterator();

    while (itr.hasNext()) {
      JobHeuristicDetailsDTO obj = itr.next();

      if ((obj.getTotalSeverity().equals("LOW") || obj.getTotalSeverity().
              equals("NONE")) && ((obj.getAmMemory() * obj.getAmVcores()
              <= defaultAmMemory * defaultAmVcores) && (obj.
              getNumberOfExecutors() * obj.getExecutorMemory()
              <= defaultNumOfExecutors * defaultExecutorsMemory))) {
        defaultAmMemory = obj.getAmMemory();
        defaultAmVcores = obj.getAmVcores();
        defaultNumOfExecutors = obj.getNumberOfExecutors();
        defaultExecutorsMemory = obj.getExecutorMemory();
        executionDuration = obj.getExecutionTime();
      }
    }
    JobProposedConfigurationDTO proposal = new JobProposedConfigurationDTO(
            "Minimal", "[Minimum resources]", defaultAmMemory, defaultAmVcores,
            defaultNumOfExecutors,
            defaultExecutorCores, defaultExecutorsMemory);

    if (executionDuration == 0) {
      proposal.setEstimatedExecutionTime("Unpredictable");
    } else {
      proposal.setEstimatedExecutionTime(convertMsToTime(executionDuration));
    }

    jobsHistoryResult.addProposal(proposal);
  }

  /**
   * The premium analysis takes into account the minimum execution duration of
   * an application and
   * returns the required resources in order to achieve this time.
   *
   * @param jobsHistoryResult
   */
  private void premiumAnalysis(JobHeuristicDTO jobsHistoryResult) {
    int defaultAmMemory = 512;
    int defaultAmVcores = 1;
    int defaultNumOfExecutors = 1;
    int defaultExecutorsMemory = 1024;
    int defaultExecutorCores = 1;
    long executionDuration = 0;
    boolean premium = false;

    Collections.sort(resultsForAnalysis, comparator);

    Iterator<JobHeuristicDetailsDTO> itr = resultsForAnalysis.iterator();

    while (itr.hasNext()) {
      JobHeuristicDetailsDTO obj = itr.next();

      if ((obj.getTotalSeverity().equals("LOW") || obj.getTotalSeverity().
              equals("NONE")) && (obj.getAmMemory() > defaultAmMemory || obj.
              getAmVcores() > defaultAmVcores || obj.getNumberOfExecutors()
              > defaultNumOfExecutors || obj.getExecutorMemory()
              > defaultExecutorsMemory)) {
        defaultAmMemory = obj.getAmMemory();
        defaultAmVcores = obj.getAmVcores();
        defaultExecutorsMemory = obj.getExecutorMemory();
        defaultNumOfExecutors = obj.getNumberOfExecutors();
        executionDuration = obj.getExecutionTime();
        premium = true;
        break;
      }
    }

    int blocks = jobsHistoryResult.getInputBlocks();

    // The system checks the number of blocks for the input file.
    // Then proposed a configuration with the same number of executors as the number of the blocks.
    if (blocks != 0) {
      if (defaultNumOfExecutors != blocks) {
        executionDuration = 0;
      }
      defaultNumOfExecutors = blocks;
      premium = true;
    }

    if (premium) {
      JobProposedConfigurationDTO proposal = new JobProposedConfigurationDTO(
              "Fast", "[Maximim Resources]", defaultAmMemory, defaultAmVcores,
              defaultNumOfExecutors,
              defaultExecutorCores, defaultExecutorsMemory);

      proposal.setEstimatedExecutionTime(convertMsToTime(executionDuration));

      jobsHistoryResult.addProposal(proposal);
    }
  }

  /**
   * A converter of Milliseconds (MS) to HH:MM:SS
   *
   * @param timeMs
   * @return
   */
  private String convertMsToTime(long timeMs) {
    if (timeMs == 0) {
      return "Unpredictable";
    }
    String hms = String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(
            timeMs),
            TimeUnit.MILLISECONDS.toMinutes(timeMs) - TimeUnit.HOURS.toMinutes(
            TimeUnit.MILLISECONDS.toHours(timeMs)),
            TimeUnit.MILLISECONDS.toSeconds(timeMs) - TimeUnit.MINUTES.
            toSeconds(TimeUnit.MILLISECONDS.toMinutes(timeMs)));

    return hms;
  }

  /**
   * The method calculated the average amount of memory - in the scale of 512 MB
   *
   * @param value
   * @param size
   * @return
   */
  private int average(int value, int size) {
    int averageValue = ((value + size - 1) / size);

    return averageValue;
  }

}
