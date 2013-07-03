package se.kth.kthfsdashboard.alert;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import se.kth.kthfsdashboard.host.Host;
import se.kth.kthfsdashboard.host.HostEJB;
import se.kth.kthfsdashboard.struct.DatePeriod;
import se.kth.kthfsdashboard.struct.Status;
import se.kth.kthfsdashboard.util.FilterUtil;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
@ManagedBean
@RequestScoped
public class AlertController implements Serializable {

   @EJB
   private AlertEJB alertEJB;
   @EJB
   private HostEJB hostEJB;
   @ManagedProperty("#{param.hostid}")
   private String hostId;
   @ManagedProperty("#{param.role}")
   private String role;
   @ManagedProperty("#{param.service}")
   private String service;
   @ManagedProperty("#{param.cluster}")
   private String cluster;
   private final static String[] severities;
   private final static String[] providers;
   private SelectItem[] severityOptions;
   private SelectItem[] providerOptions;
   private Alert[] selectedAlerts;
   private List<Alert> alerts;
   private List<Alert> filteredAlerts;
   private static final Logger logger = Logger.getLogger(AlertController.class.getName());
   private Date start;
   private Date end;
   private String period;
   private List<DatePeriod> datePeriods = new ArrayList<DatePeriod>();

   static {
      severities = new String[3];
      severities[0] = Alert.Severity.OKAY.toString();
      severities[1] = Alert.Severity.WARNING.toString();
      severities[2] = Alert.Severity.FAILURE.toString();

      providers = new String[2];
      providers[0] = Alert.Provider.Collectd.toString();
      providers[1] = Alert.Provider.Agent.toString();
   }

   public AlertController() {
      severityOptions = FilterUtil.createFilterOptions(severities);
      providerOptions = FilterUtil.createFilterOptions(providers);


      datePeriods.add(new DatePeriod("hour", "1h"));
      datePeriods.add(new DatePeriod("2hr", "2h"));
      datePeriods.add(new DatePeriod("4hr", "4h"));
      datePeriods.add(new DatePeriod("day", "1d"));
      datePeriods.add(new DatePeriod("week", "7d"));
      datePeriods.add(new DatePeriod("month", "1m"));
      datePeriods.add(new DatePeriod("year", "1y"));

      Calendar c = Calendar.getInstance();
      c.setTime(new Date());
      c.add(Calendar.HOUR_OF_DAY, -1);
      start = c.getTime();
      end = new Date();

      period = "1h";
   }

   @PostConstruct
   public void init() {
      logger.info("init AlertController");
      loadAlerts();
   }

   public String getRole() {
      return role;
   }

   public void setRole(String role) {
      this.role = role;
   }

   public String getService() {
      return service;
   }

   public void setService(String service) {
      this.service = service;
   }

   public String getHostId() {
      return hostId;
   }

   public void setHostId(String hostId) {
      this.hostId = hostId;
   }

   public void setCluster(String cluster) {
      this.cluster = cluster;
   }

   public String getCluster() {
      return cluster;
   }

   public List<Alert> getAlerts() {
      return alerts;
   }

   public Alert[] getSelectedAlerts() {
      return selectedAlerts;
   }

   public void setSelectedAlerts(Alert[] alerts) {
      selectedAlerts = alerts;
   }

   public SelectItem[] getSeverityOptions() {
      return severityOptions;
   }

   public SelectItem[] getProviderOptions() {
      return providerOptions;
   }

   public Date getStart() {
      return start;
   }

   public void setStart(Date start) {
      System.err.println("<<<<<<<<<<>>>>>>>>>>>>");
      this.start = start;
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
   
   public List<Alert> getFilteredAlerts() {
      return filteredAlerts;
   }

   public void setFilteredAlerts(List<Alert> filteredAlerts) {
      this.filteredAlerts = filteredAlerts;
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
      loadAlerts();
      System.err.println("Update: " + start + " >> " + end);
   }

   public void useCalendar() {
      period = null;
      loadAlerts();
   }

   public void loadAlerts() {
      
      System.err.println("Load: " + start + " >> " + end);
      alerts = alertEJB.findAll(start, end);
   }

   public void deleteSelectedAlerts() {
      for (Alert alert : selectedAlerts) {
         alertEJB.removeAlert(alert);
      }
      informAlertsDeleted(selectedAlerts.length + " alert(s) deleted.");
   }

   public void deleteAllAlerts() {
      alertEJB.removeAllAlerts();
      informAlertsDeleted("All alerts deleted.");
   }

   private void informAlertsDeleted(String msg) {
      FacesContext context = FacesContext.getCurrentInstance();
      context.addMessage(null, new FacesMessage("Successful", msg));
   }
}
