package io.hops.hopsworks.common.dao.workflow;

import io.hops.hopsworks.common.dao.hdfs.HdfsLeDescriptors;
import io.hops.hopsworks.common.dao.hdfs.HdfsLeDescriptorsFacade;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.oozie.client.OozieClient;
import org.apache.oozie.client.OozieClientException;
import org.codehaus.jackson.map.ObjectMapper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;

import javax.annotation.PostConstruct;
import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.PrintStream;
import java.util.logging.Logger;
import io.hops.hopsworks.common.util.Settings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

@Stateless
@LocalBean
public class OozieFacade {

  private static final Logger logger = Logger.getLogger(OozieFacade.class.
          getName());

  @EJB
  private WorkflowExecutionFacade workflowExecutionFacade;

  @EJB
  private DistributedFsService dfs;

  @EJB
  private Settings settings;

  @EJB
  private HdfsLeDescriptorsFacade hdfsLeDescriptorsFacade;

  private String OOZIE_URL;
  private String JOB_TRACKER;

  @PostConstruct
  public void init() {
    OOZIE_URL = "http://" + settings.getOozieIp() + ":11000/oozie/";
    JOB_TRACKER = settings.getJhsIp() + ":8032";
//        JOB_TRACKER = "10.0.2.15:8032";
  }

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  public String getPath() {
    return path;
  }

  public DistributedFsService getDfs() {
    return dfs;
  }

  public EntityManager getEm() {

    return em;
  }

  private String path;

  private Document doc;

  public Document getDoc() {
    return doc;
  }

  private Set<String> nodeIds;

  public Boolean hasNodeId(String id) {
    return nodeIds.contains(id);
  }

  public void addNodeId(String id) {
    nodeIds.add(id);
  }

  public void removeNodeId(String id) {
    nodeIds.remove(id);
  }

  public OozieFacade() {
    nodeIds = new HashSet<String>();
  }

  public List<Map<String, String>> getLogs(WorkflowExecution execution) throws
          IOException, OozieClientException {
    OozieClient client = new OozieClient(OOZIE_URL);
    String path = null;
    DistributedFileSystemOps dfsOps = dfs.getDfsOps();
    PrintStream ps;

    List<Map<String, String>> logs = new ArrayList<Map<String, String>>();
    Map<String, String> joblog;

    String defaultLog;

    for (WorkflowJob job : execution.getJobs()) {
      joblog = new HashMap<String, String>();
      path = execution.getPath().concat("/logs/" + job.getCreatedAt().getTime()
              + "default.log");
      if (!dfsOps.exists(path) || !job.isDone()) {
        defaultLog = client.getJobLog(job.getId());
        ps = new PrintStream(dfsOps.create(path));
        ps.print(defaultLog);
        ps.flush();
        ps.close();
      } else {
        defaultLog = dfsOps.cat(path);
      }
      joblog.put("default", defaultLog);

      path = execution.getPath().concat("/logs/" + job.getCreatedAt().getTime()
              + "error.log");
      if (!dfsOps.exists(path) || !job.isDone()) {
        ps = new PrintStream(dfsOps.create(path));
        client.getJobErrorLog(job.getId(), ps);
        ps.flush();
        ps.close();
      }
      joblog.put("error", dfsOps.cat(path));

      path = execution.getPath().concat("/logs/" + job.getCreatedAt().getTime()
              + "audit.log");
      if (!dfsOps.exists(path) || !job.isDone()) {
        ps = new PrintStream(dfsOps.create(path));
        client.getJobAuditLog(job.getId(), ps);
        ps.flush();
        ps.close();
      }
      joblog.put("audit", dfsOps.cat(path));
      joblog.put("time", job.getCreatedAt().toString());
      logs.add(joblog);
    }
    return logs;
  }

  @Asynchronous
  public void run(WorkflowExecution workflowExecution) {
    String jobId = null;
    try {
      String namenode;
      HdfsLeDescriptors nn = hdfsLeDescriptorsFacade.findEndpoint();
      if (nn == null) {
        throw new OozieClientException("404", "Missing name node");
      }
      namenode = "hdfs://" + nn.getHostname();
      this.nodeIds = new HashSet<String>();
      this.path = "/Workflows/" + workflowExecution.getUser().getUsername()
              + "/" + workflowExecution.getWorkflow().getName() + "/"
              + workflowExecution.getWorkflow().getUpdatedAt().getTime() + "/";
      OozieClient client = new OozieClient(OOZIE_URL);
      Properties conf = client.createConfiguration();
      conf.setProperty(OozieClient.APP_PATH, "${nameNode}" + path);
      conf.setProperty(OozieClient.LIBPATH, "/Workflows/lib");
      conf.setProperty(OozieClient.USE_SYSTEM_LIBPATH, String.valueOf(
              Boolean.TRUE));
      conf.setProperty("jobTracker", JOB_TRACKER);
      conf.setProperty("nameNode", namenode);
      conf.setProperty("sparkMaster", "yarn-client");
      conf.setProperty("sparkMode", "client");
      Workflow workflow = workflowExecution.getWorkflow();

      if (workflow.getWorkflowExecutions().size() == 0 || !workflow.
              getUpdatedAt().equals(workflow.getWorkflowExecutions().get(0).
                      getWorkflowTimestamp())) {

        DistributedFileSystemOps dfsOps = dfs.getDfsOps();
        dfsOps.create(path.concat("lib/init"));
        dfsOps.close();
        this.doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().
                newDocument();
        workflow.makeWorkflowFile(this);
        ((Element) this.doc.getElementsByTagName("workflow-app").item(0)).
                setAttribute("name", workflowExecution.getId().toString());
        FSDataOutputStream fsStream = dfs.getDfsOps().create(path.concat(
                "workflow.xml"));
        DOMImplementationLS domImplementation = (DOMImplementationLS) this.doc.
                getImplementation();
        LSSerializer lsSerializer = domImplementation.createLSSerializer();
        PrintStream ps = new PrintStream(fsStream);
        lsSerializer.getDomConfig().setParameter("format-pretty-print",
                Boolean.TRUE); // Set this to true if the output needs to be beautified.
        // Set this to true if the declaration is needed to be outputted.
        lsSerializer.getDomConfig().setParameter("xml-declaration", false);
        ps.print(lsSerializer.writeToString(this.doc));
        ps.flush();
        ps.close();
        workflowExecution.setWorkflowTimestamp(workflowExecution.getWorkflow().
                getUpdatedAt());
        workflowExecution.setSnapshot(new ObjectMapper().valueToTree(workflow));
      }
      jobId = client.run(conf);
    } catch (Exception e) {
      workflowExecution.setError(e.getMessage());
    } finally {
      if (workflowExecution.getError() != null || jobId == null) {
        workflowExecution.setWorkflowTimestamp(null);
        workflowExecution.setSnapshot(null);
      }
      workflowExecutionFacade.edit(workflowExecution);

    }

  }

//    public WorkflowJob getJob(String id) throws OozieClientException{
//        OozieClient client = new OozieClient(OOZIE_URL);
//        WorkflowJob job = client.getJobInfo(id);
//
//        return job;
//    }
}
