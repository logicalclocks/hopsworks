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
import org.codehaus.jettison.json.JSONObject;
import org.primefaces.event.FileUploadEvent;
import se.kth.kthfsdashboard.utils.ChartModel;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
@ManagedBean
@RequestScoped
public class ImportGraphController implements Serializable {

    @EJB
    private GraphEJB graphEjb;
    private static final Logger logger = Logger.getLogger(ImportGraphController.class.getName());

    public ImportGraphController() {
    }

    @PostConstruct
    public void init() {
        logger.info("init ImportGraphController");
    }

    public void handleUpload(FileUploadEvent event) {
        FacesMessage msg;
        String msgString = "";
        try {
            String jsonString = new String(event.getFile().getContents());
            JSONObject json = new JSONObject(jsonString);
            List<Graph> graphs = parseGraphs(json);
            graphEjb.importGraphs(parseGraphs(json));
            msgString = graphs.size() > 1 ? graphs.size() + " graphs" : graphs.size() + " graph";
            msgString += " imported sucessfully.";            
            msg = new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", msgString);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, null, ex);
            msg = new FacesMessage(FacesMessage.SEVERITY_INFO, "Failure", "Import failed. Check the graph description file and try again.");
        }
        logger.info(msgString);
        FacesContext.getCurrentInstance().addMessage(null, msg);
    }

    private List<Graph> parseGraphs(JSONObject json) throws Exception {

        List<Graph> graphs = new ArrayList<Graph>();
        Iterator<String> keys = json.keys();
        while (keys.hasNext()) {
            String target = (String) keys.next();
            JSONArray groupsArray = json.getJSONArray(target);

            for (int groupIndex = 0; groupIndex < groupsArray.length(); groupIndex++) {
                JSONObject groupObject = groupsArray.getJSONObject(groupIndex);
                String group = groupObject.getString("group");

                JSONArray graphsArray = groupObject.getJSONArray("graphs");
                for (int graphIndex = 0; graphIndex < graphsArray.length(); graphIndex++) {
                    JSONObject graph = graphsArray.getJSONObject(graphIndex);
                    String id = graph.getString("id");
                    String plugin = graph.getString("plugin");
                    String pluginInstance = graph.getString("plugin-instance");
                    String title = graph.getString("title");
                    String var = graph.getString("var");
                    String verticalLabel = graph.getString("vertical-label");
                    Graph graphInfo = new Graph(target, group, groupIndex, graphIndex, id, var, plugin, pluginInstance, title, verticalLabel);

                    JSONArray charts = graph.getJSONArray("charts");
                    for (int chartIndex = 0; chartIndex < charts.length(); chartIndex++) {
                        JSONObject chartObj = charts.getJSONObject(chartIndex);
                        ChartModel model = ChartModel.valueOf(chartObj.getString("model"));
                        String type = chartObj.getString("type");
                        String typeInstance = chartObj.getString("type-instance");
                        String ds = chartObj.getString("ds");
                        String label = chartObj.getString("label");
                        String color = chartObj.getString("color");
                        String format = chartObj.getString("format");
                        Chart chart = new Chart(model, plugin, pluginInstance, type, typeInstance, ds, label, color, format);
                        graphInfo.addChart(chart);
                    }
                    graphs.add(graphInfo);
                }
            }
        }
        return graphs;
    }
}