package se.kth.kthfsdashboard.graph;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import se.kth.kthfsdashboard.role.ServiceRoleMapper;
import se.kth.kthfsdashboard.struct.RoleType;
import se.kth.kthfsdashboard.struct.ServiceType;
import se.kth.kthfsdashboard.struct.ChartModel;
import se.kth.kthfsdashboard.utils.ColorUtils;
import se.kth.kthfsdashboard.utils.FormatUtils;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
@ManagedBean
@RequestScoped
public class GraphControllerStatic implements Serializable {

    private static final List<String> targets;
    private static final ChartModel[] models;
    private static final List<String> colors;
    private static final List<String> formats;

    private static final Logger logger = Logger.getLogger(GraphControllerStatic.class.getName());
    
    static {
        targets = new ArrayList<String>();
        targets.add("HOST");
        for (ServiceType s : ServiceType.values()) {
            targets.add(s.toString().toUpperCase());
            if (ServiceRoleMapper.getRoles(s) != null && !ServiceRoleMapper.getRoles(s).isEmpty()) {
                for (RoleType r : ServiceRoleMapper.getRoles(s)) {
                    targets.add(s.toString().toUpperCase() + "-" + r.toString().toUpperCase());
                }
            }
        }
        models = ChartModel.values();        
        colors = ColorUtils.chartColors();
        formats = FormatUtils.rrdChartFormats();
    }

    public GraphControllerStatic() {
    }

    @PostConstruct
    public void init() {
        logger.info("init GraphsControllerStatic");
    }

    public List<String> getTargets() {
        return targets;
    }

    public ChartModel[] getModels() {
        return models;
    }

    public List<String> getColors() {
        return colors;
    }

    public List<String> getFormats() {
        return formats;
    }
}