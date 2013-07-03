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
public class ChefAttributeDataModel extends ListDataModel<ChefAttributes>
        implements SelectableDataModel<ChefAttributes> {

    public ChefAttributeDataModel() {
    }

    public ChefAttributeDataModel(List<ChefAttributes> list) {
        super(list);
    }

    @Override
    public Object getRowKey(ChefAttributes t) {
        return t.getRole();
    }

    @Override
    public ChefAttributes getRowData(String string) {
        List<ChefAttributes> attributes = (List<ChefAttributes>)getWrappedData();
        for(ChefAttributes chef: attributes){
            if(chef.getRole().equals(string)){
                return chef;
            }
        }
        return null;
            
    }
    
    
}
