package se.kth.bbc.study.filebrowser;

import java.io.Serializable;
import java.util.Objects;

/**
 * Container for data to be displayed in the file browser.
 *
 * @author Stig
 */
public class FileSummary implements Serializable, Comparable<FileSummary> {

    private String sampleID;
    private String type;
    private String filename;
    private String status;
    private String studyName;

    public FileSummary(String studyName, String sampleID, String type, String filename, String status) {
        this.sampleID = sampleID;
        this.type = type;
        this.filename = filename;
        this.status = status;
        this.studyName = studyName;
    }

    public String getSampleID() {
        return sampleID;
    }

    public void setSampleID(String sampleID) {
        this.sampleID = sampleID;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStudyName() {
        return studyName;
    }

    public void setStudyName(String studyName) {
        this.studyName = studyName;
    }

    public String getDisplayStatus() {
        switch (status) {
            case "available":
                return "Available";
            case "copying_to_hdfs":
                return "Copying to HDFS";
            case "uploading":
                return "Uploading";
            default:
                return "";
        }
    }

    public boolean isFile() {
        return filename != null && !filename.equals("");
    }

    public boolean isTypeFolder() {
        return !isFile() && type != null && !type.equals("");
    }

    public boolean isSample() {
        return !isTypeFolder();
    }
    
    public String getDisplayName(){
        if(filename!=null && !filename.equals("")){
            return filename;
        }else if(type != null && !type.equals("")){
            return type;
        }else if(sampleID != null && !sampleID.equals("")){
            return sampleID;
        }else{
            return studyName;
        }
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 29 * hash + Objects.hashCode(this.sampleID);
        hash = 29 * hash + Objects.hashCode(this.type);
        hash = 29 * hash + Objects.hashCode(this.filename);
        hash = 29 * hash + Objects.hashCode(this.status);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final FileSummary other = (FileSummary) obj;
        if (!Objects.equals(this.sampleID, other.sampleID)) {
            return false;
        }
        if (!Objects.equals(this.type, other.type)) {
            return false;
        }
        if (!Objects.equals(this.filename, other.filename)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "FileSummary{" + "sampleID=" + sampleID + ", type=" + type + ", filename=" + filename + ", status=" + status + '}';
    }

    @Override
    public int compareTo(FileSummary o) {
        if(filename!= null){
            return filename.compareTo(o.filename);
        }else if(type != null){
            return type.compareTo(o.type);
        }else if(sampleID!= null){
            return sampleID.compareTo(o.sampleID);
        }else{
            return studyName.compareTo(o.studyName);
        }
    }
    
    
}
