package se.kth.kthfsdashboard.utils;

import java.text.DecimalFormat;
import java.text.ParseException;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
public class ParseUtils {

    public static Double parseDouble(String d) throws ParseException {
        DecimalFormat format = new DecimalFormat("#.##");
        return format.parse(d.toUpperCase().replace("+", "")).doubleValue();
    }

    public static long parseLong(String d) throws ParseException {
        DecimalFormat format = new DecimalFormat("#.##");
        return format.parse(d.toUpperCase().replace("+", "")).longValue();
    }

    public static boolean isInteger(String s) {
        try {
            Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }
}
