package se.kth.kthfsdashboard.log;

import java.util.ArrayList;
import java.util.List;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

/**
 *
 * @author x
 */
@ManagedBean(name = "logController")
@RequestScoped
public class LogController {

    @EJB
    private LogEJB logEJB;
    private Log log = new Log();
    private List<Log> logList = new ArrayList<Log>();


    public String doCreateLog() {
        setLog(getLogEJB().addLog(getLog()));
        setLogList(getLogEJB().findLogs());
        return "showAlllogs.xhtml";
    }

    public String doShowlogs() {
        setLogList(getLogEJB().findLogs());

//        FacesContext ctx = FacesContext.getCurrentInstance();
//        ctx.addMessage(null, new FacesMessage("book created", "BOOK created!!!!!!!!!!!!!"));
//        return null;
        return "showAlllogs.xhtml";
    }
     

    public LogEJB getLogEJB() {
        return logEJB;
    }

    public void setLogEJB(LogEJB logEJB) {
        this.logEJB = logEJB;
    }

    public Log getLog() {
        return log;
    }

    public void setLog(Log log) {
        this.log = log;
    }

    public List<Log> getLogList() {
        return logList;
    }

    public void setLogList(List<Log> logList) {
        this.logList = logList;
    }

}
