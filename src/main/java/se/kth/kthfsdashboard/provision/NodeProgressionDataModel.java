/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.kthfsdashboard.provision;

import java.util.List;
import javax.faces.model.ListDataModel;
import org.primefaces.model.SelectableDataModel;

/**
 * Data model for the user interface with the progression state of the nodes.
 * 
 * @author Alberto Lorente Leal <albll@kth.se>
 */
public class NodeProgressionDataModel extends ListDataModel<NodeProgression>
        implements SelectableDataModel<NodeProgression> {

    public NodeProgressionDataModel() {
    }

    public NodeProgressionDataModel(List<NodeProgression> list) {
        super(list);
    }

    @Override
    public Object getRowKey(NodeProgression t) {
        return t.getNodeId();
    }

    @Override
    public NodeProgression getRowData(String rowKey) {
        List<NodeProgression> nodes = (List<NodeProgression>) getWrappedData();
        for (NodeProgression node : nodes) {
            if (node.getNodeId().equals(rowKey)) {
                return node;
            }
        }
        return null;
    }
    
    public int size(){
        return ((List<NodeProgression>)getWrappedData()).size();
    }
}
