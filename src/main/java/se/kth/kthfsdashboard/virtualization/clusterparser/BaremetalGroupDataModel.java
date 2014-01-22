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
public class BaremetalGroupDataModel extends ListDataModel<BaremetalGroup> 
    implements SelectableDataModel<BaremetalGroup> {

    public BaremetalGroupDataModel() {
    }

    public BaremetalGroupDataModel(List<BaremetalGroup> list) {
        super(list);
    }

    
    @Override
    public Object getRowKey(BaremetalGroup t) {
        return t.getServices().get(0);
    }

    @Override
    public BaremetalGroup getRowData(String rowKey) {
        List<BaremetalGroup> nodes = (List<BaremetalGroup>) getWrappedData();
        for (BaremetalGroup group : nodes) {
            if (group.getServices().get(0).equals(rowKey)) {
                return group;
            }
        }
        return null;
    }
    
}
