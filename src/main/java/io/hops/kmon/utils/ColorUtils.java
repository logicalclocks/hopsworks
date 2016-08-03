package io.hops.kmon.utils;

import java.util.ArrayList;
import java.util.List;
import io.hops.kmon.struct.ColorType;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
public class ColorUtils {
    
     public static final String VAR_COLOR = "COLOR(@n)";
   
     public static List<String> chartColors() {         
         List<String> colors =  new ArrayList<String>();
                 for (ColorType c: ColorType.values()) {
            colors.add(c.toString());
        }
        colors.add(VAR_COLOR);
        return colors;         
     }
   
}
