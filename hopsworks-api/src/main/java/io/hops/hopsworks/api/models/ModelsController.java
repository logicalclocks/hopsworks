package io.hops.hopsworks.api.models;

import io.hops.hopsworks.api.models.dto.ModelDTO;
import io.hops.hopsworks.common.dao.jobs.description.Jobs;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.Utils;
import io.hops.hopsworks.common.jobs.JobController;
import io.hops.hopsworks.common.jobs.spark.SparkJobConfiguration;
import io.hops.hopsworks.common.jupyter.JupyterController;
import io.hops.hopsworks.common.provenance.core.Provenance;
import io.hops.hopsworks.common.provenance.state.ProvFileStateParamBuilder;
import io.hops.hopsworks.common.provenance.state.ProvStateController;
import io.hops.hopsworks.common.provenance.state.dto.ProvStateElastic;
import io.hops.hopsworks.common.provenance.state.dto.ProvStateListDTO;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.JobException;
import io.hops.hopsworks.exceptions.ModelsException;
import io.hops.hopsworks.exceptions.ProvenanceException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.XAttrSetFlag;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.parquet.Strings;
import org.eclipse.persistence.jaxb.JAXBContextFactory;
import org.eclipse.persistence.jaxb.MarshallerProperties;
import org.json.JSONObject;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ModelsController {

  private static final Logger LOGGER = Logger.getLogger(ModelsController.class.getName());

  @EJB
  private DistributedFsService dfs;
  @EJB
  private ProvStateController provenanceController;
  @EJB
  private JobController jobController;
  @EJB
  private JupyterController jupyterController;

  public void attachModel(Project project, String userFullName, ModelDTO modelDTO)
      throws DatasetException, ModelsException {

    modelDTO.setUserFullName(userFullName);

    String modelPath = Utils.getProjectPath(project.getName()) + Settings.HOPS_MODELS_DATASET + "/" +
        modelDTO.getName() + "/" + modelDTO.getVersion();

    DistributedFileSystemOps dfso = null;
    try {
      dfso = dfs.getDfsOps();

      JAXBContext sparkJAXBContext = JAXBContextFactory.createContext(new Class[] {ModelDTO.class},
          null);
      Marshaller marshaller = sparkJAXBContext.createMarshaller();
      marshaller.setProperty(MarshallerProperties.JSON_INCLUDE_ROOT, false);
      marshaller.setProperty(MarshallerProperties.MEDIA_TYPE, MediaType.APPLICATION_JSON);
      StringWriter sw = new StringWriter();
      marshaller.marshal(modelDTO, sw);
      String modelSummaryStr = sw.toString();

      if(modelDTO.getMetrics() != null && !modelDTO.getMetrics().getAttributes().isEmpty()) {
        modelSummaryStr = castMetricsToDouble(modelSummaryStr);
      }

      byte[] model = modelSummaryStr.getBytes(StandardCharsets.UTF_8);

      EnumSet<XAttrSetFlag> flags = EnumSet.noneOf(XAttrSetFlag.class);
      flags.add((XAttrSetFlag.CREATE));

      dfso.setXAttr(modelPath, "provenance.model_summary", model, flags);

    } catch(IOException | JAXBException ex) {
      throw new DatasetException(RESTCodes.DatasetErrorCode.ATTACH_XATTR_ERROR, Level.SEVERE,
          "path: " + modelPath, ex.getMessage(), ex);
    } finally {
      if (dfso != null) {
        dfs.closeDfsClient(dfso);
      }
    }
  }

  public ProvStateElastic getModel(Project project, String mlId) throws ProvenanceException {
    ProvFileStateParamBuilder provFilesParamBuilder = new ProvFileStateParamBuilder()
        .withProjectInodeId(project.getInode().getId())
        .withMlType(Provenance.MLType.MODEL.name())
        .withPagination(0, 1)
        .withMlId(mlId);
    ProvStateListDTO fileState = provenanceController.provFileStateList(project, provFilesParamBuilder);
    if (fileState != null) {
      List<ProvStateElastic> experiments = fileState.getItems();
      if (experiments != null && !experiments.isEmpty()) {
        return experiments.iterator().next();
      }
    }
    return null;
  }

  private String castMetricsToDouble(String modelSummaryStr) throws ModelsException {
    JSONObject modelSummary = new JSONObject(modelSummaryStr);
    if(modelSummary.has("metrics")) {
      JSONObject metrics = modelSummary.getJSONObject("metrics");
      for(Object metric: metrics.keySet()) {
        String metricKey = null;
        try {
          metricKey = (String) metric;
        } catch (Exception e) {
          throw new ModelsException(RESTCodes.ModelsErrorCode.KEY_NOT_STRING, Level.FINE,
              "keys in metrics dict must be string", e.getMessage(), e);
        }
        try {
          metrics.put(metricKey, Double.valueOf(metrics.getString(metricKey)));
        } catch (Exception e) {
          throw new ModelsException(RESTCodes.ModelsErrorCode.METRIC_NOT_NUMBER, Level.FINE,
              "Provided value for metric " + metricKey + " is not a number" , e.getMessage(), e);
        }
      }
      modelSummary.put("metrics", metrics);
    }
    return modelSummary.toString();
  }

  public String versionProgram(Project project, Users user, String jobName, String kernelId,
                               String modelName, int modelVersion)
      throws JobException, ServiceException {
    if(!Strings.isNullOrEmpty(jobName)) {
      //model in job
      Jobs experimentJob = jobController.getJob(project, jobName);
      SparkJobConfiguration sparkJobConf = (SparkJobConfiguration)experimentJob.getJobConfig();
      String suffix = sparkJobConf.getAppPath().substring(sparkJobConf.getAppPath().lastIndexOf("."));
      String relativePath = Settings.HOPS_MODELS_DATASET + "/" + modelName + "/" + modelVersion + "/program" + suffix;
      jobController.versionProgram(sparkJobConf, project, user,
          new Path(Utils.getProjectPath(project.getName()) + relativePath));
      return relativePath;
    } else {
      //model in jupyter
      String relativePath = Settings.HOPS_MODELS_DATASET + "/" + modelName + "/" + modelVersion + "/program.ipynb";
      Path path = new Path(Utils.getProjectPath(project.getName())
          + "/" + relativePath);
      jupyterController.versionProgram(project, user, kernelId, path);
      return relativePath;
    }
  }
}
