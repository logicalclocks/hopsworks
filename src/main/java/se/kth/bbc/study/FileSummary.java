package se.kth.bbc.study;

import java.io.Serializable;
import java.util.Objects;

/**
 * Container for data to be displayed in the file browser.
 *
 * @author Stig
 */
public class FileSummary implements Serializable, Comparable<FileSummary> {

    private String name;
    private String status; //TODO: make into enum field (same as DB)
    private boolean file;
    private String type;

    public FileSummary(String name, String status, boolean isFile, String type) {
        this.name = name;
        this.file = isFile;
        this.status = status;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isFile() {
        return file;
    }

    public void setFile(boolean file) {
        this.file = file;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
    
    

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 61 * hash + Objects.hashCode(this.name);
        hash = 61 * hash + Objects.hashCode(this.status);
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
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        if (!Objects.equals(this.status, other.status)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int compareTo(FileSummary document) {
        return this.getName().compareTo(document.getName());
    }
    
    public String getFullFilename(){
        return name + "." + type;
    }
}
