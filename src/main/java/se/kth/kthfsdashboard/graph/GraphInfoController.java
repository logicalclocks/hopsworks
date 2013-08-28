package se.kth.kthfsdashboard.graph;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.primefaces.event.FileUploadEvent;
import se.kth.kthfsdashboard.utils.RrdtoolCommand;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
@ManagedBean
@RequestScoped
public class GraphInfoController implements Serializable {

    @EJB
    private GraphEJB graphEjb;
    private static final Logger logger = Logger.getLogger(GraphInfoController.class.getName());

    public GraphInfoController() {
    }

    @PostConstruct
    public void init() {
        logger.info("init GraphController3");
    }

    public List<String> getGraphIds(String target, String graphGroup) {
        return graphEjb.findSelectedIds(target, graphGroup);
    }

    public List<String> getGraphIds(String service, String role, String graphGroup) {
        String target = service + "-" + role;
        return graphEjb.findSelectedIds(target, graphGroup);

    }

    public List<String> getGroups(String target) {
        return graphEjb.findGroups(target);

    }

    public List<String> getGroups(String service, String role) {
        String target = service + "-" + role;
        return graphEjb.findGroups(target);

    }
}