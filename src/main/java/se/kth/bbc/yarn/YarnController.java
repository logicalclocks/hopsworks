package se.kth.bbc.yarn;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.UploadedFile;
import se.kth.bbc.lims.MessagesController;
import se.kth.bbc.lims.Constants;

/**
 *
 * @author stig
 */
@ManagedBean
@ViewScoped
public class YarnController implements Serializable {

    private static final Logger logger = Logger.getLogger(YarnController.class.getName());

    //The local path to the application master jar
    private String appMasterJarPath = null;
    private UploadedFile appMasterJar;
    private String appName = null;
    private String args = null;
    private final Map<String, String> extraFiles = new HashMap<>();
    private String mainClassName;
    

    public String getAppMasterJarPath() {
        return appMasterJarPath;
    }

    public void setAppMasterJarPath(String path) {
        appMasterJarPath = path;
    }

    public void setAppName(String name) {
        this.appName = name;
    }

    public String getAppName() {
        return this.appName;
    }

    public String getArgs() {
        return args;
    }

    public void setArgs(String args) {
        this.args = args;
    }
    
    public void setMainClassName(String mainClass){
        this.mainClassName = mainClass;
    }
    
    public String getMainClassName(){
        return mainClassName;
    }

    public void handleFileUpload(FileUploadEvent event) {
        appMasterJar = event.getFile();

        appMasterJarPath = Constants.LOCAL_APPMASTER_DIR + File.separator + appMasterJar.getFileName();
        this.appName = appMasterJar.getFileName();
        try {
            copyFile(event.getFile().getInputstream(),appMasterJarPath);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Could not upload file.", ex);
            MessagesController.addErrorMessage("Failed to upload appMaster jar.");
        }
    }

    public void extraFileUploads(FileUploadEvent event) {
        //TODO: make a tmp folder per user, per study, per...
        UploadedFile file = event.getFile();
        String path = Constants.LOCAL_EXTRA_DIR + File.separator + file.getFileName();
        try {
            copyFile(event.getFile().getInputstream(),path);
            extraFiles.put(file.getFileName(), path);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Could not upload file.", e);
            MessagesController.addErrorMessage("Failed to upload file " + file.getFileName());
        }
    }

    private void copyFile(InputStream in, String path) {
        try {

            // write the inputStream to a FileOutputStream
            OutputStream out = new FileOutputStream(new File(path));

            int read = 0;
            byte[] bytes = new byte[1024];

            while ((read = in.read(bytes)) != -1) {
                out.write(bytes, 0, read);
            }

            in.close();
            out.flush();
            out.close();

            MessagesController.addInfoMessage("Success.", "Jar successfully uploaded.");
        } catch (IOException e) {
            System.out.println(e.getMessage());
            MessagesController.addErrorMessage("Failed to upload jar.");
        }
    }

    public void runJar() {
        YarnRunner.Builder builder = new YarnRunner.Builder(appMasterJarPath);
        if (!extraFiles.isEmpty()) {
            builder.addAllLocalResourcesPaths(extraFiles);
        }
        builder.appMasterArgs(args).appMasterMainClass(mainClassName);
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
            logger.log(Level.SEVERE,"Error while initializing AppMaster.",e);
            MessagesController.addErrorMessage("Failed to initialize AppMaster.");
            return;
        }
    }

}
