package se.kth.kthfsdashboard.util;

import java.text.DecimalFormat;
import java.text.ParseException;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
public class Parser {

    public Double parseDouble(String d) throws ParseException {
        DecimalFormat format = new DecimalFormat("#.##");
        return format.parse(d.toUpperCase().replace("+", "")).doubleValue();
    }

    public long parseLong(String d) throws ParseException {
        DecimalFormat format = new DecimalFormat("#.##");
        return format.parse(d.toUpperCase().replace("+", "")).longValue();
    }
    
}
