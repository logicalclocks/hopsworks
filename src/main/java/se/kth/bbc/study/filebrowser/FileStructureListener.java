package se.kth.bbc.study.filebrowser;

/**
 * Callback interface for filebrowser updating. Called by StudyMB to notify
 * filebrowser model of changes in the backend.
 *
 * @author stig
 */
public interface FileStructureListener {

    /**
     * Removes the sample with id <i>sampleid</i> from the model. Called when it
     * is removed in the backend to reflect the change in the frontend.
     */
    public void removeSample(String sampleId);

    /**
     * Removes the type folder with extension <i>type</i> under sample with id
     * <i>sampleid</i> from the model. Called when it is removed in the backend
     * to reflect the change in the frontend.
     */
    public void removeType(String sampleId, String type);

    /**
     * Removes the file <i>file</i> under sample with id <i>sampleid</i> and
     * extension <i>type</i> from the model. Called when it is removed in the
     * backend to reflect the change in the frontend.
     */
    public void removeFile(String sampleId, String type, String file);

    public void newSample(String sampleId);

    public void newFile(String sampleId, String type, String file, String status);

    public void changeStudy(String newStudy);

    public void updateStatus(String sampleId, String type, String filename, String newStatus);

}
