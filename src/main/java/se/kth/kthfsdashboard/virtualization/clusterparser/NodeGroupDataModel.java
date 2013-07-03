/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.kthfsdashboard.virtualization.clusterparser;

import java.util.List;
import javax.faces.model.ListDataModel;
import org.primefaces.model.SelectableDataModel;

/**
 *
 * @author Alberto Lorente Leal <albll@kth.se>
 */
public class NodeGroupDataModel extends ListDataModel<NodeGroup>
        implements SelectableDataModel<NodeGroup> {

    public NodeGroupDataModel() {
    }

    public NodeGroupDataModel(List<NodeGroup> list) {
        super(list);
    }

    @Override
    public NodeGroup getRowData(String rowKey) {
        List<NodeGroup> nodes = (List<NodeGroup>) getWrappedData();
        for (NodeGroup group : nodes) {
            if (group.getSecurityGroup().equals(rowKey)) {
                return group;
            }
        }
        return null;
    }

    @Override
    public Object getRowKey(NodeGroup group) {
        return group.getSecurityGroup();
    }
}
