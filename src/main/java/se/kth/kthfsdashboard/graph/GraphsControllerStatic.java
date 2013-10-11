package se.kth.kthfsdashboard.graph;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import se.kth.kthfsdashboard.role.ServiceRoleMapper;
import se.kth.kthfsdashboard.struct.ColorType;
import se.kth.kthfsdashboard.struct.RoleType;
import se.kth.kthfsdashboard.struct.ServiceType;
import se.kth.kthfsdashboard.utils.ChartModel;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
@ManagedBean
@RequestScoped
public class GraphsControllerStatic implements Serializable {

    private static final String COLOR_N = "COLOR(@n)";
    private static final List<String> targets;
    private static final ChartModel[] models;
    private static final List<String> colors;
    private static final List<String> formats;

    private static final Logger logger = Logger.getLogger(GraphsControllerStatic.class.getName());
    
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
        
        colors = new ArrayList<String>();
        for (ColorType c: ColorType.values()) {
            colors.add(c.toString());
        }
        colors.add(COLOR_N);

        formats = new ArrayList<String>();
        formats.add("%5.2lf");
        formats.add("%5.2lf %S");
    }

    public GraphsControllerStatic() {
    }

    @PostConstruct
    public void init() {
        logger.info("init EditGraphsController2");
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