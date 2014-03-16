/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.hop.wf;

import java.util.HashMap;
import java.util.Map;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author Alberto Lorente Leal <albll@kth.se>
 */
public class WorkflowConverter implements Converter{
    
    public static Map<String, Workflow> workflows = new HashMap<String, Workflow>();
    
    static {
        workflows.put("alberto", new Workflow("alberto","workflow1", "20-10-2013","blablabla", 
                "declare hello-world; deftask spell( ~out : ~<str> ) *{    out=`echo ${str[@]}`; sleep 30 }* x = 'hello' 'world'; out = spell( str : x ); target out;"));
//        workflows.put("jim", new Workflow("jim","workflow2", "12-9-2013","bowtie test", ""));
//        workflows.put("jorgen", new Workflow("jorgen"," jorgen","10-8-2013","workflow, simply a test", ""));
//        workflows.put("kamal", new Workflow("kamal"," test", "2-3-2013","workflow, simply a test", ""));
        
    }
    
    
    public Object getAsObject(final FacesContext fc, final UIComponent component, final String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        else {
            return workflows.get(value);
        }
    }

    public String getAsString(final FacesContext fc, final UIComponent component, final Object value) {
        if (value == null || value.equals("")) {
            return "";
        } else {
            return String.valueOf(((Workflow) value).getWorkflowTags());
        }
    }
}
