package se.kth.bbc.flink;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.UploadedFile;
import se.kth.bbc.flink.FlinkRunner;
import se.kth.bbc.lims.MessagesController;
import se.kth.bbc.study.StudyMB;
import se.kth.bbc.yarn.JobController;

/**
 *
 * @author stig
 */
@ManagedBean
@ViewScoped
public class FlinkController implements Serializable {

  private static final Logger logger = Logger.getLogger(FlinkController.class.
          getName());

  private final JobController jc = new JobController();

  private static final String KEY_JOB_JAR = "jobJarPath";

  @ManagedProperty(value = "#{studyManagedBean}")
  private transient StudyMB study;

  private String jobjarmain;
  private String jobArgs;
  private int paral = 1;
  private int port;
  private String host;

  private static ExecutorService exc = Executors.newCachedThreadPool();

  @PostConstruct
  public void init() {
    try {
      jc.setBasePath(study.getStudyName(), study.getUsername());
    } catch (IOException c) {
      logger.log(Level.SEVERE, "Failed to create directory structure.", c);
      MessagesController.addErrorMessage(
              "Failed to initialize Yarn controller. Running Yarn jobs will not work.");
    }
  }

  public void handleJobJarUpload(FileUploadEvent event) {
    try {
      jc.handleFileUpload(KEY_JOB_JAR, event);
    } catch (IllegalStateException e) {
      init();
      jc.handleFileUpload(KEY_JOB_JAR, event);
    }
  }

  public void setJobJarMain(String s) {
    this.jobjarmain = s.trim();
  }

  public String getJobJarMain() {
    return jobjarmain;
  }

  public String getJobArgs() {
    return jobArgs;
  }

  public void setJobArgs(String jobArgs) {
    this.jobArgs = jobArgs.trim();
  }

  public void setStudy(StudyMB study) {
    this.study = study;
  }

  public void setParal(String s) {
    try {
      paral = Integer.valueOf(s.trim());
    } catch (NumberFormatException e) {
      paral = 2;
    }
  }

  public String getParal() {
    return "" + paral;
  }

  public void extraFileUploads(FileUploadEvent event) {
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
        MessagesController.addErrorMessage(
                "Failed to initialize Flink controller. Running Yarn jobs will not work.");
      }
    }
  }

  public void runJar() {
    /*
     * Runnable c = new Runnable() {
     *
     * @Override
     * public void run() {
     * try {
     * getAddressPort(Constants.FLINK_CONF_DIR);
     * } catch (IOException e) {
     * MessagesController.addErrorMessage("Failed to find Flink JobManager
     * info.");
     * logger.log(Level.SEVERE,"Could not load configuration from
     * \".yarn-properties\". Are you sure Flink is running on Yarn with
     * configuration directory as "+Constants.FLINK_CONF_DIR+"?");
     * }
     * FlinkRunnerSimple.FlinkBuilder b = new FlinkRunnerSimple.FlinkBuilder(new
     * File(jc.getFilePath(KEY_JOB_JAR)), jobjarmain, host, port);
     * b.setParallelism(paral);
     * b.setJobArgs(parseArgs(jobArgs).split(" "));
     * FlinkRunnerSimple p = b.build();
     * try {
     * p.runJob();
     * } catch (IOException | ProgramInvocationException | YarnException ex) {
     * logger.log(Level.SEVERE, null, ex);
     * MessagesController.addErrorMessage("Failed to run job.");
     * }
     * }
     * };
     * exc.execute(c);
     */
    try {
      FlinkRunner.maint();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private String parseArgs(String s) {
    for (int i = 0; i < s.length(); i++) {
      if (s.charAt(i) == '$') {
        i++;
        if (i >= s.length()) {
          break;
        }
        if (s.charAt(i) == '{') {
          int end = s.indexOf('}', i);
          if (end != -1) {
            String pre = s.substring(0, i - 1);
            String post = s.substring(end + 1);
            String key = s.substring(i + 1, end);
            String value = jc.getFilePath(key);
            if (value != null) {
              s = pre + value + post;
            } else {
              s = pre + post;
            }
          }
        }
      }
    }
    return s;
  }

  public String getJobJarName() {
    String path = jc.getFilePath(KEY_JOB_JAR);
    if (path == null) {
      return null;
    }
    int lastslash = path.lastIndexOf("/");
    return path.substring(lastslash);
  }

  public class StringPair {

    private String one;
    private String two;

    public String getOne() {
      return one;
    }

    public void setOne(String one) {
      this.one = one;
    }

    public String getTwo() {
      return two;
    }

    public void setTwo(String two) {
      this.two = two;
    }

    public StringPair(String one, String two) {
      this.one = one;
      this.two = two;
    }
  }

  public List<StringPair> getUploadedFiles() {
    List<StringPair> pairs = new ArrayList<>();
    for (String a : jc.getFiles().keySet()) {
      String path = jc.getFilePath(a);
      int lastslash = path.lastIndexOf("/");
      String flnm = path.substring(lastslash);
      pairs.add(new StringPair(a, flnm));
    }
    return pairs;
  }

  private void getAddressPort(String location) throws IOException {
    File f = new File(location);
    Charset charset = Charset.defaultCharset();
    try (BufferedReader reader = Files.newBufferedReader(f.toPath(), charset)) {
      String line = null;
      while ((line = reader.readLine()) != null) {
        if (line.contains("jobManager")) {
          int index = line.indexOf("=");
          String val = line.substring(index + 1);
          int sep = line.indexOf("\\:");
          host = line.substring(index + 1, sep);
          port = Integer.valueOf(line.substring(sep + 2));
        }
      }
    } catch (IOException x) {
      System.err.format("IOException: %s%n", x);
      throw x;
    }
    if (host == null || port == 0) {
      throw new IOException("Failed to read configuration from file.");
    }
  }
}
