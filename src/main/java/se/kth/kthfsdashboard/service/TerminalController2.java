package se.kth.kthfsdashboard.service;

import java.io.Serializable;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.persistence.Transient;
import se.kth.kthfsdashboard.communication.WebCommunication;
import se.kth.kthfsdashboard.host.Host;
import se.kth.kthfsdashboard.host.HostEJB;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
@ManagedBean
@SessionScoped
public class TerminalController2 implements Serializable {

    @EJB
    @Transient
    private HostEJB hostEjb;
    private static final Logger logger = Logger.getLogger(TerminalController2.class.getName());
    private String state;
    private String service;
    private String cluster;
    private Integer a = 0;

    enum States {

        SPARK, SPARK_SHELL
    }

    public TerminalController2() {
        state = States.SPARK.toString();
    }

    @PostConstruct
    public void init() {
        logger.info("init TerminalController2");
    }
    
    public Integer getA() {
        a +=1 ;
        return a;
    }

    public String getState() {
        return state;
    }

    public void setStateToSpark() {
        this.state = States.SPARK.toString();
    }

    public void setStateToSParkShell() {
        this.state = States.SPARK_SHELL.toString();
    }

    public boolean isStateSpark() {
        return state.equals(States.SPARK.toString());
    }

    public boolean isStateSparkShell() {
        return state.equals(States.SPARK_SHELL.toString());
    }

    public String getPrompt(String cluster, String service) {

        this.service = service;
        this.cluster = cluster;
        state = States.SPARK.toString();
        return "client@Spark:~$";
    }
    
    public String getPrompt2() {
        return state + ">";
    }

    public String handleCommand(String command, String[] params) {
//        String service = "Spark";
//        String cluster = "cluster1";
        System.out.println("Satet = " + state);

//      TODO: Check special characters like ";" to avoid injection
        String ip;
        if (state.equals(States.SPARK.toString())) {
            if (service.equalsIgnoreCase("Spark")) {
                if (!command.equals("spark-shell")) {
                    return "Unknown command. Accepted commands are: spark-shell";
                }
            } else {
                return null;
            }
            try {
//             TODO: get only one host
                List<Host> hosts = hostEjb.find(cluster, service, "");
                if (hosts.isEmpty()) {
                    throw new RuntimeException("No live node available.");
                }
                WebCommunication web = new WebCommunication();
                state = States.SPARK_SHELL.toString();
                ip = hosts.get(0).getPublicOrPrivateIp();
                return web.executeStart(ip, cluster, service, "-", command, params);
            } catch (Exception ex) {
                state = States.SPARK.toString();
                logger.log(Level.SEVERE, null, ex);
                return "Error: Could not contact a node";
            }
        } else if (state.equals(States.SPARK_SHELL.toString())) {
//             TODO: get only one host
            List<Host> hosts = hostEjb.find(cluster, service, "");
            if (hosts.isEmpty()) {
                throw new RuntimeException("No live node available.");
            }
            try {
                WebCommunication web = new WebCommunication();
                state = States.SPARK_SHELL.toString();
                ip = hosts.get(0).getPublicOrPrivateIp();
                if (command.equals("exit")) {
                    state = States.SPARK.toString();
                }
                return web.executeContinue(ip, cluster, service, "-", command, params);
            } catch (Exception ex) {
                state = States.SPARK_SHELL.toString();
                logger.log(Level.SEVERE, null, ex);
                return "Error: Could not contact a node";
            }
        } else {
            return null;
        }

    }
}