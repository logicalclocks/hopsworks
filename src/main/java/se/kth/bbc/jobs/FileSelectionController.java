package se.kth.bbc.jobs;

import java.io.Serializable;
import java.util.Map;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import org.primefaces.event.FileUploadEvent;

/**
 *
 * @author stig
 */
@ManagedBean
@ViewScoped
public class FileSelectionController implements Serializable {

  private String selectedPath;
  private boolean isMainFile;
  private JobController controller;
  private boolean initialized = false;
  private Map<String, String> attributes;

  public String getSelectedPath() {
    return selectedPath;
  }

  public void setSelectedPath(String selectedPath) {
    this.selectedPath = selectedPath;
  }

  public void init(JobController controller, boolean isMainFile,
          Map<String, String> attributes) {
    this.controller = controller;
    this.isMainFile = isMainFile;
    this.attributes = attributes;
    this.initialized = true;
  }

  public void uploadFile(FileUploadEvent event) {
    if (initialized) {
      if (isMainFile) {
        controller.uploadMainFile(event, attributes);
      } else {
        controller.uploadExtraFile(event, attributes);
      }
    } else {
      throw new IllegalStateException(
              "FileSelectionController has not been initialized.");
    }
  }

  public void invalidate() {
    this.initialized = false;
  }

  public void submitSelectedPath() {
    if (initialized) {
      if (isMainFile) {
        controller.selectMainFile(selectedPath, attributes);
      } else {
        controller.selectExtraFile(selectedPath, attributes);
      }
    }
  }
}
