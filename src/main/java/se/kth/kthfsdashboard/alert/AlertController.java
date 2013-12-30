package se.kth.kthfsdashboard.alert;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import se.kth.kthfsdashboard.alert.Alert.Provider;
import se.kth.kthfsdashboard.alert.Alert.Severity;
import se.kth.kthfsdashboard.struct.DatePeriod;
import se.kth.kthfsdashboard.utils.FilterUtils;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
@ManagedBean
@RequestScoped
public class AlertController implements Serializable {

   @EJB
   private AlertEJB alertEJB;
   private final static String[] severities;
   private final static String[] providers;
   private SelectItem[] severityOptions;
   private SelectItem[] providerOptions;
   private Alert[] selectedAlerts;
   private List<Alert> alerts;
   private static final Logger logger = Logger.getLogger(AlertController.class.getName());
   private Date start;
   private Date end;
   private String period;
   private List<DatePeriod> datePeriods = new ArrayList<DatePeriod>();
   private String severity;
   private String provider;

   static {
      severities = new String[3];
      severities[0] = Alert.Severity.OK.toString();
      severities[1] = Alert.Severity.WARNING.toString();
      severities[2] = Alert.Severity.FAILURE.toString();

      providers = new String[2];
      providers[0] = Alert.Provider.Collectd.toString();
      providers[1] = Alert.Provider.Agent.toString();
   }

   public AlertController() {
      severityOptions = FilterUtils.createFilterOptions(severities);
      providerOptions = FilterUtils.createFilterOptions(providers);

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

   public String[] getSeverities() {
      return severities;
   }

   public String[] getProviders() {
      return providers;
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
   
   public String getSeverity() {
      return severity;
   }

   public void setSeverity(String severity) {
      this.severity = severity;
   }

   public String getProvider() {
      return provider;
   }

   public void setProvider(String provider) {
      this.provider = provider;
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
   }

   public void useCalendar() {
      period = null;
      loadAlerts();
   }

   public void loadAlerts() {
      logger.log(Level.INFO, "Loading alerts from {0} to {1}, severity={2}, provider={3}", new Object[]{start, end, severity, provider});
      if (severity != null && provider != null) {
         Provider p = Provider.valueOf(provider);
         Severity s = Severity.valueOf(severity);
         alerts = alertEJB.find(start, end, p, s);
      } else if (severity != null) {
         alerts = alertEJB.find(start, end, Severity.valueOf(severity));
      } else if (provider != null) {
         alerts = alertEJB.find(start, end, Provider.valueOf(provider));
      } else {
         alerts = alertEJB.find(start, end);
      }
   }

   public void deleteSelectedAlerts() {
      for (Alert alert : selectedAlerts) {
         alertEJB.removeAlert(alert);
      }
      loadAlerts();
      String msg = selectedAlerts.length + " ";
      msg += selectedAlerts.length > 1 ? "alerts deleted." : "alert deleted.";
      informAlertsDeleted(msg);
   }

   public void deleteAllAlerts() {
      alertEJB.removeAllAlerts();
      loadAlerts();            
      informAlertsDeleted("All alerts deleted.");
   }

   private void informAlertsDeleted(String msg) {
      FacesContext context = FacesContext.getCurrentInstance();
      context.addMessage(null, new FacesMessage("Successful", msg));
      System.err.println(msg);
   }


}
