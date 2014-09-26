package se.kth.bbc.study.filebrowser;

import java.io.Serializable;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
import org.primefaces.model.TreeNode;
import se.kth.bbc.study.SampleFiles;
import se.kth.bbc.study.SampleFilesController;
import se.kth.bbc.study.SampleIdController;
import se.kth.bbc.study.SampleIds;
import se.kth.bbc.study.StudyMB;

/**
 *
 * @author stig
 */
@ManagedBean(name = "sampleModel")
@SessionScoped
public class SampleTreeModel implements Serializable, FileStructureListener {

    @ManagedProperty("#{studyManagedBean}")
    private transient StudyMB studymb;

    @EJB
    private transient SampleIdController sampleIDController;

    @EJB
    private transient SampleFilesController sampleFilesController;

    //The root of the file tree, only used for having a rooot
    private TreeNode root;
    //The root of the study folder
    private SampleTreeNode studyRoot;

    @PostConstruct
    public void init() {
        //root node
        root = new SampleTreeNode(new FileSummary("", "", "", "", ""), null);
        //level 1: study
        studyRoot = new SampleTreeNode(new FileSummary(studymb.getStudyName(), "", "", "", ""), root);
        //level 2: all samples
        List<SampleIds> samples = sampleIDController.findAllByStudy(studymb.getStudyName());
        for (SampleIds sam : samples) {
            TreeNode node = new SampleTreeNode(new FileSummary(studymb.getStudyName(), sam.getSampleIdsPK().getId(), "", "", ""), studyRoot);
            //level 3: all extensions
            List<String> extensions = sampleFilesController.findAllExtensionsForSample(sam.getSampleIdsPK().getId());
            for (String s : extensions) {
                TreeNode typeFolder = new SampleTreeNode(new FileSummary(studymb.getStudyName(), sam.getSampleIdsPK().getId(), s, "", ""), node);
                //level 4: all files
                List<SampleFiles> files = sampleFilesController.findAllByIdType(sam.getSampleIdsPK().getId(), s);
                for (SampleFiles file : files) {
                    TreeNode leaf = new SampleTreeNode(new FileSummary(studymb.getStudyName(), sam.getSampleIdsPK().getId(), s, file.getSampleFilesPK().getFilename(), file.getStatus()), typeFolder);
                }
            }
        }
        studymb.registerFileListener(this);
        //TODO: unregister? Does not doing this create memory leaks?
    }

    @Override
    public void removeSample(String sampleId) {
        List<TreeNode> samples = studyRoot.getChildren();
        SampleTreeNode toRemove = new SampleTreeNode(new FileSummary(studymb.getStudyName(), sampleId, "", "", ""), null);
        samples.remove(toRemove);
    }

    @Override
    public void removeType(String sampleId, String type) {
        // get the sample node
        List<TreeNode> samples = studyRoot.getChildren();
        SampleTreeNode sampleTemplate = new SampleTreeNode(new FileSummary(studymb.getStudyName(), sampleId, "", "", ""), null);
        TreeNode sampleReal = samples.get(samples.indexOf(sampleTemplate));

        //remove the type node
        List<TreeNode> types = sampleReal.getChildren();
        SampleTreeNode toRemove = new SampleTreeNode(new FileSummary(studymb.getStudyName(), sampleId, type, "", ""), null);
        types.remove(toRemove);
    }

    @Override
    public void removeFile(String sampleId, String type, String file) {
        // get the sample node
        List<TreeNode> samples = studyRoot.getChildren();
        SampleTreeNode sampleTemplate = new SampleTreeNode(new FileSummary(studymb.getStudyName(), sampleId, "", "", ""), null);
        TreeNode sampleReal = samples.get(samples.indexOf(sampleTemplate));

        //get the type node
        List<TreeNode> types = sampleReal.getChildren();
        SampleTreeNode typeTemplate = new SampleTreeNode(new FileSummary(studymb.getStudyName(), sampleId, type, "", ""), null);
        TreeNode typeReal = types.get(types.indexOf(typeTemplate));

        //remove the file
        List<TreeNode> files = typeReal.getChildren();
        SampleTreeNode toRemove = new SampleTreeNode(new FileSummary(studymb.getStudyName(), sampleId, type, file, ""), null);
        files.remove(toRemove);
    }

    @Override
    public void newSample(String sampleId) {
        SampleTreeNode sample = new SampleTreeNode(new FileSummary(studymb.getStudyName(), sampleId, "", "", ""), studyRoot);
    }

    @Override
    public void newFile(String sampleId, String type, String file, String status) {
        FileSummary addFile = new FileSummary(studymb.getStudyName(), sampleId, type, file, status);
        List<TreeNode> samples = studyRoot.getChildren();
        SampleTreeNode sampleTemplate = new SampleTreeNode(new FileSummary(studymb.getStudyName(), sampleId, "", "", ""), null);
        int index = samples.indexOf(sampleTemplate);
        if (index >= 0) {// list contains sample
            //add new file as part of old sample
            TreeNode sample = samples.get(index);
            SampleTreeNode typeTemplate = new SampleTreeNode(new FileSummary(studymb.getStudyName(), sampleId, type, "", ""), null);
            List<TreeNode> types = sample.getChildren();
            index = types.indexOf(typeTemplate);
            if (index >= 0) { // there already are files of this type: just add the file node
                TreeNode typefolder = types.get(index);
                SampleTreeNode newfile = new SampleTreeNode(addFile, typefolder);
            } else {//no files of this type exist yet: add type folder and file node
                SampleTreeNode typefolder = new SampleTreeNode(new FileSummary(studymb.getStudyName(), sampleId, type, "", ""), sample);
                SampleTreeNode newfile = new SampleTreeNode(addFile, typefolder);
            }
        } else { //list does not contain sample
            //add new sample and new file
            SampleTreeNode sample = new SampleTreeNode(new FileSummary(studymb.getStudyName(), sampleId, "", "", ""), studyRoot);
            SampleTreeNode typefolder = new SampleTreeNode(new FileSummary(studymb.getStudyName(), sampleId, type, "", ""), sample);
            SampleTreeNode newfile = new SampleTreeNode(addFile, typefolder);
        }
    }

    @Override
    public void changeStudy(String newStudy) {
        //TODO: do on different thread?
        root.getChildren().clear();
        //level 1: study
        studyRoot = new SampleTreeNode(new FileSummary(studymb.getStudyName(), "", "", "", ""), root);
        //level 2: all samples
        List<SampleIds> samples = sampleIDController.findAllByStudy(studymb.getStudyName());
        for (SampleIds sam : samples) {
            TreeNode node = new SampleTreeNode(new FileSummary(studymb.getStudyName(), sam.getSampleIdsPK().getId(), "", "", ""), studyRoot);
            //level 3: all extensions
            List<String> extensions = sampleFilesController.findAllExtensionsForSample(sam.getSampleIdsPK().getId());
            for (String s : extensions) {
                TreeNode typeFolder = new SampleTreeNode(new FileSummary(studymb.getStudyName(), sam.getSampleIdsPK().getId(), s, "", ""), node);
                //level 4: all files
                List<SampleFiles> files = sampleFilesController.findAllByIdType(sam.getSampleIdsPK().getId(), s);
                for (SampleFiles file : files) {
                    TreeNode leaf = new SampleTreeNode(new FileSummary(studymb.getStudyName(), sam.getSampleIdsPK().getId(), s, file.getSampleFilesPK().getFilename(), file.getStatus()), typeFolder);
                }
            }
        }
    }

    @Override
    public void updateStatus(String sampleId, String type, String filename, String newStatus) {
                // get the sample node
        List<TreeNode> samples = studyRoot.getChildren();
        SampleTreeNode sampleTemplate = new SampleTreeNode(new FileSummary(studymb.getStudyName(), sampleId, "", "", ""), null);
        TreeNode sampleReal = samples.get(samples.indexOf(sampleTemplate));

        //get the type node
        List<TreeNode> types = sampleReal.getChildren();
        SampleTreeNode typeTemplate = new SampleTreeNode(new FileSummary(studymb.getStudyName(), sampleId, type, "", ""), null);
        TreeNode typeReal = types.get(types.indexOf(typeTemplate));

        //remove the file
        List<TreeNode> files = typeReal.getChildren();
        SampleTreeNode toChange = new SampleTreeNode(new FileSummary(studymb.getStudyName(), sampleId, type, filename, ""), null);
        files.remove(toChange);
        toChange = new SampleTreeNode(new FileSummary(studymb.getStudyName(),sampleId,type,filename,newStatus),typeReal);
    }

    /*
     * ATTRIBUTE GETTERS AND SETTERS
     */
    public StudyMB getStudymb() {
        return studymb;
    }

    public void setStudymb(StudyMB studymb) {
        this.studymb = studymb;
    }

    public TreeNode getRoot() {
        return root;
    }

    public void setRoot(TreeNode root) {
        this.root = root;
    }

}
