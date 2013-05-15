package se.kth.kthfsdashboard.util;

import java.util.Map;
import javax.faces.context.FacesContext;
import javax.servlet.http.Cookie;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
public class CookieTools {

    public String read(String name) {
        Map<String, Object> requestCookieMap  =  FacesContext.
                getCurrentInstance().getExternalContext().getRequestCookieMap();
        
        Cookie c = (Cookie) requestCookieMap.get(name);
        
        if (c == null) {
           return "";
        }
        return c.getValue();
    }
    
    public void write(String name, String value) {
        FacesContext.getCurrentInstance().getExternalContext()
                .addResponseCookie(name, value, null);
    }
}
