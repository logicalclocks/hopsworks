package se.kth.bbc.jobs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.UploadedFile;
import se.kth.bbc.lims.Constants;
import se.kth.bbc.lims.MessagesController;

/**
 * General controller for starting jobs from the web interface.
 * @author stig
 */
public class JobController implements Serializable {

    private static final Logger logger = Logger.getLogger(JobController.class.getName());

    private final Map<String, String> variables = new HashMap<>();
    private final Map<String, String> files = new HashMap<>();

    private String basePath = null;

    public void handleFileUpload(String key, FileUploadEvent event) throws IllegalStateException {
        if (basePath == null) {
            throw new IllegalStateException("Basepath has not been set!");
        }
        UploadedFile file = event.getFile();

        String uploadPath = basePath + file.getFileName();
        try {
            copyFile(event.getFile().getInputstream(), uploadPath);
            files.put(key, uploadPath);
            MessagesController.addInfoMessage("Success.", "File "+file.getFileName()+" successfully uploaded.");
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Could not upload file.", ex);
            MessagesController.addErrorMessage("Failed to upload file " + file.getFileName() + ".");
        }
    }

    private void copyFile(InputStream in, String path) throws IOException {
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
    }

    public void setBasePath(String basepath) throws IOException {
        if(basepath.endsWith(File.separator)){
          basePath = basepath;
        }else{
          basePath = basepath + File.separator;
        }

        Path p = Paths.get(basePath);
        boolean success = p.toFile().mkdirs();
        if (!success && !p.toFile().exists()) {
            throw new IOException("Failed to create directory structure");
        }
    }

    public void putVariable(String key, String value) {
        variables.put(key, value);
    }

    public String getVariable(String key) {
        return variables.get(key);
    }

    public String getFilePath(String key) {
        return files.get(key);
    }

    public Map<String, String> getFiles() {
        return new HashMap<>(files);
    }
    
    public boolean containsVariableKey(String key){
      return variables.containsKey(key);
    }

    //TODO: clean up folder after job has been started (or stopped?)...
}
