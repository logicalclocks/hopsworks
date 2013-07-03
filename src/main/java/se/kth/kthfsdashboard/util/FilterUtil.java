package se.kth.kthfsdashboard.util;

import javax.faces.model.SelectItem;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
public class FilterUtil {
   
      public static SelectItem[] createFilterOptions(String[] data) {
      SelectItem[] options = new SelectItem[data.length + 1];

      options[0] = new SelectItem("", "Any");
      for (int i = 0; i < data.length; i++) {
         options[i + 1] = new SelectItem(data[i], data[i]);
      }
      return options;
   }
   
}
