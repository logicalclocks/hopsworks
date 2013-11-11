/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.kthfsdashboard.job;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author Alberto Lorente Leal <albll@kth.se>
 */
public class JobConverter implements Converter{
    
    public Map<String, Job> jobs = new HashMap<String,Job>();
    
//    static{
//        jobs.put("alberto", new Job("alberto", "workflow1", "20-10-2013", 2000));
//        jobs.put("jorgen", new Job("jorgen", "workflow2", "15-10-2013", 4800));
//        jobs.put("jim", new Job("jim", "workflow3", "12-10-2013", 2000));
//        jobs.put("kamal", new Job("kamal", "workflow4", "16-3-2013", 3000));
//        
//    }
    public JobConverter(List<Job> jobsList){
        for(Job job:jobsList){
            jobs.put(job.getExecutedBy(), job);
        }
    }
    
    public Object getAsObject(final FacesContext fc, final UIComponent component, final String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        else {
            return jobs.get(value);
        }
    }

    public String getAsString(final FacesContext fc, final UIComponent component, final Object value) {
        if (value == null || value.equals("")) {
            return "";
        } else {
            return String.valueOf(((Job) value).getUniqueID());
        }
    }
}
