package se.kth.kthfsdashboard.graph;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import se.kth.kthfsdashboard.role.Role;
import se.kth.kthfsdashboard.role.RoleEJB;
import se.kth.kthfsdashboard.struct.DatePeriod;
import se.kth.kthfsdashboard.utils.ParseUtils;
import se.kth.kthfsdashboard.utils.UrlUtils;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
@ManagedBean
@RequestScoped
public class GraphController implements Serializable {

    @EJB
    private RoleEJB roleEjb;
    @EJB
    private GraphEJB graphEjb;
    @ManagedProperty("#{param.cluster}")
    private String cluster;
    @ManagedProperty("#{param.hostid}")
    private String hostId;
    private Date start;
    private Date end;
    private String period;
    private static final List<DatePeriod> datePeriods = new ArrayList<DatePeriod>();
    private static final List<Integer> columns;
    private static String URL_PATH = "/rest/collectd/graph?";
    private static final Logger logger = Logger.getLogger(GraphController.class.getName());
    
    static {
        columns = new ArrayList<Integer>(Arrays.asList(2, 3, 4, 5));
        datePeriods.add(new DatePeriod("hour", "1h"));
        datePeriods.add(new DatePeriod("2hr", "2h"));
        datePeriods.add(new DatePeriod("4hr", "4h"));
        datePeriods.add(new DatePeriod("day", "1d"));
        datePeriods.add(new DatePeriod("week", "7d"));
        datePeriods.add(new DatePeriod("month", "1m"));
        datePeriods.add(new DatePeriod("year", "1y"));        
    }

    public GraphController() {

        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        c.add(Calendar.HOUR_OF_DAY, -1);
        start = c.getTime();
        end = new Date();
        period = "1h";
    }

    @PostConstruct
    public void init() {
        logger.info("init GraphController");
    }

    public String getCluster() {
        return cluster;
    }

    public void setCluster(String cluster) {
        this.cluster = cluster;
    }

    public String getHostId() {
        return hostId;
    }

    public void setHostId(String hostId) {
        this.hostId = hostId;
    }

    public Date getStart() {
        return start;
    }

    public void setStart(Date start) {
        this.start = start;
    }

    public Long getStartTime() {
        return longTime(getStart());
    }

    public Date getEnd() {
        return end;
    }

    public void setEnd(Date end) {
        this.end = end;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public List<DatePeriod> getDatePeriods() {
        return datePeriods;
    }

    public List<Integer> getColumns() {
        return columns;
    }

    public Long getEndTime() {
        return longTime(getEnd());
    }

    public void updateDates() {
        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        String unit = period.substring(period.length() - 1);
        int delta = Integer.parseInt(period.substring(0, period.length() - 1));

        if (unit.equals("h")) {
            c.add(Calendar.HOUR_OF_DAY, -delta);
        } else if (unit.equals("d")) {
            c.add(Calendar.DAY_OF_MONTH, -delta);
        } else if (unit.equals("m")) {
            c.add(Calendar.MONTH, -delta);
        } else if (unit.equals("y")) {
            c.add(Calendar.YEAR, -delta);
        } else {
            return;
        }
        start = c.getTime();
        end = new Date();
    }

    public void useCalendar() {
        period = null;
    }

    private Long longTime(Date d) {
        return d.getTime() / 1000;
    }

    private String createGraphUrl(String target, String id, HashMap<String, String> extraParams) throws MalformedURLException {
        HashMap params = new HashMap<String, String>();
        params.put("id", id);
        params.put("target", target);
        params.put("start", getStartTime().toString());
        params.put("end", getEndTime().toString());
        for (String key : extraParams.keySet()) {
            params.put(key, extraParams.get(key));
        }
        return UrlUtils.addParams(URL_PATH, params);
    }

    public String getRoleGraphUrl(String service, String role, String id) throws MalformedURLException {
        HashMap extraParams = new HashMap<String, String>();
        String target = service + "-" + role;
        extraParams.put("host", hostId);
        return createGraphUrl(target, id, extraParams);
    }

    public String getGraphUrl(String target, String id) throws MalformedURLException {
        HashMap extraParams = new HashMap<String, String>();
        Graph graph = graphEjb.find(target, id);
        String vars[] = {};
        if (!graph.getVar().isEmpty()) {
            vars = graph.getVar().split(";");
        }
        for (String var : vars) {
            if (var.startsWith("n=") && var.split("=")[1].contains("..")) { // n=X..X
                String varName = "n";
                String varValue = "";
                String valueExp = var.split("=")[1];
                Long from, to;
                String fromExp = valueExp.substring(0, valueExp.indexOf(".."));
                String toExp = valueExp.substring(valueExp.indexOf("..") + 2, valueExp.length());
                if (ParseUtils.isInteger(fromExp)) {
                    from = Long.parseLong(fromExp);
                } else {
                    from = Long.parseLong(readValue(fromExp));
                }
                if (ParseUtils.isInteger(toExp)) {
                    to = Long.parseLong(toExp);
                } else {
                    to = Long.parseLong(readValue(toExp));
                }
                for (Long i = from; i <= to; i++) {
                    varValue += varValue.isEmpty() ? i : "," + i;
                }
                extraParams.put(varName, varValue);
            } else if (var.startsWith("host=")) {
                String varName = "host";
                String exp = var.split("=")[1];
                String varValue = readValue(exp);
                extraParams.put(varName, varValue);
            } else {
                throw new RuntimeException("Invalid syntax: " + var);
            }
        }
        if (!extraParams.containsKey("host")) {
            extraParams.put("host", hostId);
        }
        return createGraphUrl(target, id, extraParams);
    }

    private String readValue(String exp) {
        String service = exp.substring(exp.indexOf("(") + 1, exp.indexOf(","));
        String role = exp.substring(exp.indexOf(",") + 1, exp.indexOf(")"));
        if (exp.startsWith("COUNT(")) {
            return roleEjb.count(cluster, service, role).toString();
        } else if (exp.startsWith("HOST(")) {
            List<Role> roles = roleEjb.findRoles(cluster, service, role);
            String host = roles.size() > 0 ? roles.get(0).getHostId() : null;
            return host;
        } else if (exp.startsWith("HOSTS(")) {
            String hosts = "";
            for (Role r : roleEjb.findRoles(cluster, service, role)) {
                hosts += hosts.isEmpty() ? r.getHostId() : "," + r.getHostId();
            }
            return hosts;
        } else {
            throw new RuntimeException("Invalid Expression: " + exp);
        }
    }
    
    public String getTarget(String target) {
        return target.toUpperCase();
    }
    
    public String getTarget(String service, String role) {
        return (service + "-" + role).toUpperCase();
    }    
}