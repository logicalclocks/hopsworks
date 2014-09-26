package se.kth.bbc.study.filebrowser;

import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

/**
 *
 * @author stig
 */
public class SampleTreeNode extends DefaultTreeNode {

    public SampleTreeNode(FileSummary data, TreeNode parent) {
        super(data, parent);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (getClass() != o.getClass()) {
            return false;
        }        
        final SampleTreeNode other = (SampleTreeNode) o;
        if(super.getData() == null){
            return other.getData()==null;
        }else{
            return ((FileSummary)super.getData()).equals(other.getData());
        }
    }
}
