package se.kth.bbc.study.filebrowser;

import java.util.List;
import org.primefaces.model.TreeNode;
import se.kth.bbc.study.SampleFiles;
import se.kth.bbc.study.SampleIds;

/**
 *
 * @author stig
 */
public interface FileStructureListener {

    public void removeSample(String sampleId);

    public void removeType(String sampleId, String type);

    public void removeFile(String sampleId, String type, String file);

    public void newSample(String sampleId);

    public void newFile(String sampleId, String type, String file, String status);

    public void changeStudy(String newStudy);

    public void updateStatus(String sampleId, String type, String filename, String newStatus);

}
