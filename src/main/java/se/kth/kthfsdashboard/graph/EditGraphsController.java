package se.kth.kthfsdashboard.graph;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
@ManagedBean
@RequestScoped
public class EditGraphsController implements Serializable {

    @EJB
    private GraphEJB graphEjb;
    @ManagedProperty("#{param.target}")
    private String target;
    private Graph selectedGraph;
    private Graph[] selectedGraphs;
    private List<Graph> graphs = new ArrayList<Graph>();
    private List<String> targets;
    private static final Logger logger = Logger.getLogger(EditGraphsController.class.getName());
    private String message;

    public EditGraphsController() {
    }

    @PostConstruct
    public void init() {
        logger.info("init EditGraphsController");
        loadTargets();
        if (target != null) {
           loadGraphs();
        }
    }

    private void loadTargets() {
        targets = graphEjb.findTargets();
    }

    private void loadGraphs() {
        List<Graph> selectedGraphsList = new ArrayList<Graph>();
        graphs = graphEjb.find(target);
        selectedGraphs = new Graph[0];
        for (Graph g: graphs) {
            if (g.isSelected()) {
                selectedGraphsList.add(g);
            }
        }        
        selectedGraphs = selectedGraphsList.toArray(selectedGraphs);
    }
    
    public void save() {                
        graphEjb.updateGraphs(target, graphs, Arrays.asList(selectedGraphs));
        String msg = selectedGraphs.length + " graph(s) selected for " + target;
        FacesContext context = FacesContext.getCurrentInstance();
        context.addMessage(null, new FacesMessage("Successful", msg));
        logger.info(msg);
    }    

    public Graph getSelectedGraph() {
        return selectedGraph;
    }

    public void setSelectedGraph(Graph selectedGraph) {
        this.selectedGraph = selectedGraph;
    }

    public Graph[] getSelectedGraphs() {
        return selectedGraphs;
    }

    public void setSelectedGraphs(Graph[] selectedGraphs) {
        this.selectedGraphs = selectedGraphs;
    }

    public List<Graph> getGraphs() {
        return graphs;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }
    
    public List<String> getTargets() {
        return targets;
    }    

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}