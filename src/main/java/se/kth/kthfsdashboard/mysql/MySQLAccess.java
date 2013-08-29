package se.kth.kthfsdashboard.mysql;

import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.UploadedFile;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
public class MySQLAccess implements Serializable {

    final static String USERNAME = "kthfs";
    final static String PASSWORD = "kthfs";
    final static String DATABASE = "kthfs";
    final static String BACKUP_FILENAME = "dashboard.sql";
    
    final static Logger logger = Logger.getLogger(MySQLAccess.class.getName());

    public StreamedContent getBackup() {
        List<String> command = new ArrayList<String>();
        command.add("mysqldump");
        command.add("--single-transaction");
        command.add("-u" + USERNAME);
        command.add("-p" + PASSWORD);
        command.add(DATABASE);
        try {
            ProcessBuilder builder = new ProcessBuilder(command).redirectErrorStream(true);
            Process process = builder.start();
//            process.waitFor();
            InputStream inputStream = process.getInputStream();
            StreamedContent backupContent = new DefaultStreamedContent(inputStream, "application/sql", BACKUP_FILENAME);
            return backupContent;
        } catch (Exception ex) {
            logger.log(Level.SEVERE, null, ex);
            return null;
        }
    }

    public boolean restore(UploadedFile file) {
        List<String> command = new ArrayList<String>();
        command.add("mysql");
        command.add("-u" + USERNAME);
        command.add("-p" + PASSWORD);
        command.add(DATABASE);
        try {
            InputStream inputStream = file.getInputstream();
            Process process = new ProcessBuilder(command).start();
            byte[] bytes = new byte[1024];
            int read;
            while ((read = inputStream.read(bytes)) != -1) {
                process.getOutputStream().write(bytes, 0, read);
            }
            inputStream.close();
            process.getOutputStream().flush();
            process.getOutputStream().close();
            process.waitFor();
            if (process.exitValue() == 0) {
                return true;
            }
            return false;
        } catch (Exception ex) {
            logger.log(Level.SEVERE, null, ex);
            return false;
        }
    }
}