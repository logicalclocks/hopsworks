package se.kth.bbc.study;

import java.io.Serializable;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
import javax.faces.bean.ViewScoped;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

/**
 *
 * @author stig
 */
@ManagedBean(name = "treeTableView")
@SessionScoped
public class TreeTableView implements Serializable {

    private TreeNode root;

    @ManagedProperty("#{studyManagedBean}")
    private transient StudyMB studymb;
    
    @EJB
    private transient SampleIdController sampleIDController;

    @EJB
    private transient SampleFilesController sampleFilesController;

    @PostConstruct
    public void init() {
        //root node
        root = new DefaultTreeNode(new FileSummary("", "", "", "", ""), null);
        //level 1: study
        TreeNode studyRoot = new DefaultTreeNode(new FileSummary(studymb.getStudyName(), "", "", "", ""), root);
        //level 2: all samples
        List<SampleIds> samples = sampleIDController.findAllByStudy(studymb.getStudyName());
        for (SampleIds sam : samples) {
            TreeNode node = new DefaultTreeNode(new FileSummary(studymb.getStudyName(), sam.getSampleIdsPK().getId(), "", "", ""), studyRoot);
            //level 3: all extensions
            List<String> extensions = sampleFilesController.findAllExtensionsForSample(sam.getSampleIdsPK().getId());
            for (String s : extensions) {
                TreeNode typeFolder = new DefaultTreeNode(new FileSummary(studymb.getStudyName(), sam.getSampleIdsPK().getId(), s, "", ""), node);
                //level 4: all files
                List<SampleFiles> files = sampleFilesController.findAllByIdType(sam.getSampleIdsPK().getId(), s);
                for (SampleFiles file : files) {
                    TreeNode leaf = new DefaultTreeNode(new FileSummary(studymb.getStudyName(), sam.getSampleIdsPK().getId(), s, file.getSampleFilesPK().getFilename(), file.getStatus()), typeFolder);
                    
                }
            }
        }
    }

    public TreeNode getRoot() {
        return root;
    }

    public void setStudymb(StudyMB studymb) {
        this.studymb = studymb;
    }
    
    public void reInit(){
        root = null;
        root = new DefaultTreeNode(new FileSummary("", "", "", "", ""), null);
        //level 1: study
        TreeNode studyRoot = new DefaultTreeNode(new FileSummary(studymb.getStudyName(), "", "", "", ""), root);
        //level 2: all samples
        List<SampleIds> samples = sampleIDController.findAllByStudy(studymb.getStudyName());
        for (SampleIds sam : samples) {
            TreeNode node = new DefaultTreeNode(new FileSummary(studymb.getStudyName(), sam.getSampleIdsPK().getId(), "", "", ""), studyRoot);
            //level 3: all extensions
            List<String> extensions = sampleFilesController.findAllExtensionsForSample(sam.getSampleIdsPK().getId());
            for (String s : extensions) {
                TreeNode typeFolder = new DefaultTreeNode(new FileSummary(studymb.getStudyName(), sam.getSampleIdsPK().getId(), s, "", ""), node);
                //level 4: all files
                List<SampleFiles> files = sampleFilesController.findAllByIdType(sam.getSampleIdsPK().getId(), s);
                for (SampleFiles file : files) {
                    TreeNode leaf = new DefaultTreeNode(new FileSummary(studymb.getStudyName(), sam.getSampleIdsPK().getId(), s, file.getSampleFilesPK().getFilename(), file.getStatus()), typeFolder);
                    
                }
            }
        }
    }

}
