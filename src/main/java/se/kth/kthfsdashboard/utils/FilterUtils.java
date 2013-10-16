package se.kth.kthfsdashboard.utils;

import javax.faces.model.SelectItem;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
public class FilterUtils {
   
    public static SelectItem[] createFilterOptions(Object[] data) {
        SelectItem[] options = new SelectItem[data.length + 1];
        options[0] = new SelectItem("", "Any");
        for (int i = 0; i < data.length; i++) {
            options[i + 1] = new SelectItem(data[i].toString(), data[i].toString());
        }
        return options;
    }    
   
}
