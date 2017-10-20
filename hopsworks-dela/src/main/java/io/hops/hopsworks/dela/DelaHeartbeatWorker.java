package io.hops.hopsworks.dela;

import com.google.gson.Gson;
import io.hops.hopsworks.common.dela.AddressJSON;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.dela.dto.hopssite.ClusterServiceDTO;
import io.hops.hopsworks.dela.exception.ThirdPartyException;
import io.hops.hopsworks.dela.hopssite.HopssiteController;
import io.hops.hopsworks.util.CertificateHelper;
import io.hops.hopsworks.util.SettingsHelper;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.KeyStore;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerService;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.mail.Session;
import org.javatuples.Pair;
import org.javatuples.Triplet;

@Startup
@Singleton
public class DelaHeartbeatWorker {

  private final static Logger LOG = Logger.getLogger(DelaHeartbeatWorker.class.getName());

  @Resource
  TimerService timerService;

  @Resource(lookup = "mail/BBCMail")
  private Session mailSession;

  @EJB
  private Settings settings;
  @EJB
  private DelaStateController delaStateCtrl;
  @EJB
  private HopssiteController hopsSiteProxy;
  @EJB
  private TransferDelaController delaCtrl;

  private State state;

  @PostConstruct
  private void init() {
    if (delaStateCtrl.delaEnabled()) {
      state = State.SETTINGS;
      timerService.createTimer(0, settings.getHOPSSITE_HEARTBEAT_RETRY(), "Timer for dela settings check.");
      LOG.log(Level.INFO, "state:{0}", state);
    }
  }

  @PreDestroy
  private void destroyTimer() {
    for (Timer timer : timerService.getTimers()) {
      timer.cancel();
    }
  }

  @Timeout
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  private void timeout(Timer timer) {
    LOG.log(Level.INFO, "state timeout:{0}", new Object[]{state});
    switch (state) {
      case SETTINGS:
        settings(timer);
        break;
      case DELA_VERSION:
        delaVersion(timer);
        break;
      case DELA_CONTACT:
        delaContact(timer);
        break;
      case REGISTER:
        hopsSiteRegister(timer);
        break;
      case HEAVY_PING:
        heavyPing(timer);
        break;
      case PING:
        ping(timer);
        break;
      default:
        throw new IllegalStateException("unknown state");
    }
  }

  //********************************************************************************************************************
  private void settings(Timer timer) {
    Optional<Triplet<KeyStore, KeyStore, String>> certSetup = CertificateHelper.initKeystore(settings);
    if (certSetup.isPresent()) {
      delaStateCtrl.delaCertsAvailable(certSetup.get().getValue0(), certSetup.get().getValue1(), 
        certSetup.get().getValue2());
      delaVersion(resetToDelaVersion(timer));
    } else {
      LOG.log(Level.WARNING, "dela certificates not ready. waiting...");
    }
  }

  private String delaVersion;
  private void delaVersion(Timer timer) {
    LOG.log(Level.INFO, "retrieving hops-site dela_version");
    try {
      delaVersion = hopsSiteProxy.delaVersion();
      delaStateCtrl.hopssiteContacted();
      delaContact(resetToDelaContact(timer));
    } catch (ThirdPartyException tpe) {
      LOG.log(Level.WARNING, "source:<{0}:{1}>:{2}",
        new Object[]{tpe.getSource(), tpe.getSourceDetails(), tpe.getMessage()});
      //try again later - maybe it works
    }
  }

  private void delaContact(Timer timer) {
    LOG.log(Level.INFO, "state:{0}", state);
    AddressJSON delaTransferEndpoint;
    try {
      delaTransferEndpoint = SettingsHelper.delaTransferEndpoint(settings);
    } catch (ThirdPartyException tpe) {
      try {
        delaTransferEndpoint = getDelaTransferEndpoint(delaVersion);
        delaStateCtrl.transferDelaContacted();
        settings.setDELA_PUBLIC_ENDPOINT(delaTransferEndpoint);
        hopsSiteRegister(resetToRegister(timer), delaTransferEndpoint);
      } catch (ThirdPartyException tpe2) {
        LOG.log(Level.WARNING, "source:<{0}:{1}>:{2}",
          new Object[]{tpe2.getSource(), tpe2.getSourceDetails(), tpe2.getMessage()});
        //try again later - maybe it works
      }
    }

  }

  private void hopsSiteRegister(Timer timer) {
    LOG.log(Level.INFO, "state:{0}", state);
    AddressJSON delaTransferEndpoint;
    try {
      delaTransferEndpoint = SettingsHelper.delaTransferEndpoint(settings);
    } catch (ThirdPartyException ex) {
      resetToDelaContact(timer);
      return;
    }
    try {
      hopsSiteRegister(timer, delaTransferEndpoint);
    } catch (ThirdPartyException tpe) {
      LOG.log(Level.WARNING, "source:<{0}:{1}>:{2}",
        new Object[]{tpe.getSource(), tpe.getSourceDetails(), tpe.getMessage()});
      //try again later - maybe it works
    }
  }

  private void hopsSiteRegister(Timer timer, AddressJSON delaTransferEndpoint) throws ThirdPartyException {
    String delaHttpEndpoint = SettingsHelper.delaHttpEndpoint(settings);
    hopsSiteRegister(timer, delaHttpEndpoint, delaTransferEndpoint);
  }

  private void hopsSiteRegister(Timer timer, String delaClusterAddress, AddressJSON delaTransferAddress)
    throws ThirdPartyException {

    String publicCId = hopsSiteProxy.registerCluster(delaClusterAddress, new Gson().toJson(delaTransferAddress),
      mailSession.getProperty("mail.from"));
    settings.setDELA_CLUSTER_ID(publicCId);
    heavyPing(resetToHeavyPing(timer));
  }

  private void heavyPing(Timer timer) {
    LOG.log(Level.INFO, "state:{0}", state);
    Pair<List<String>, List<String>> datasets;
    try {
      datasets = delaCtrl.getContents();
    } catch (ThirdPartyException tpe) {
      LOG.log(Level.WARNING, "source:<{0}:{1}>:{2}",
        new Object[]{tpe.getSource(), tpe.getSourceDetails(), tpe.getMessage()});
      if (ThirdPartyException.Source.SETTINGS.equals(tpe.getSource())) {
        resetToDelaContact(timer);
      } else {
        //try again later - maybe it works
      }
      return;
    }
    try {
      hopsSiteProxy.heavyPing(datasets.getValue0(), datasets.getValue1());
      ping(resetToPing(timer));
    } catch (ThirdPartyException tpe) {
      LOG.log(Level.WARNING, "source:<{0}:{1}>:{2}",
        new Object[]{tpe.getSource(), tpe.getSourceDetails(), tpe.getMessage()});
      if (ThirdPartyException.Error.CLUSTER_NOT_REGISTERED.is(tpe.getMessage())) {
        resetToRegister(timer);
      } else if (ThirdPartyException.Source.SETTINGS.equals(tpe.getSource())) {
        resetToRegister(timer);
      } else {
        //try again later - maybe it works
      }
    }
  }

  private void ping(Timer timer) {
    LOG.log(Level.INFO, "state:{0}", state);
    Pair<List<String>, List<String>> datasets;
    try {
      datasets = delaCtrl.getContents();
    } catch (ThirdPartyException tpe) {
      LOG.log(Level.WARNING, "source:<{0}:{1}>:{2}",
        new Object[]{tpe.getSource(), tpe.getSourceDetails(), tpe.getMessage()});
      if (ThirdPartyException.Source.SETTINGS.equals(tpe.getSource())) {
        resetToDelaContact(timer);
      } else {
        //try again later - maybe it works
      }
      return;
    }
    try {
      hopsSiteProxy.ping(new ClusterServiceDTO.Ping(datasets.getValue0().size(), datasets.getValue1().size()));
    } catch (ThirdPartyException tpe) {
      LOG.log(Level.WARNING, "source:<{0}:{1}>:{2}",
        new Object[]{tpe.getSource(), tpe.getSourceDetails(), tpe.getMessage()});
      if (ThirdPartyException.Error.CLUSTER_NOT_REGISTERED.is(tpe.getMessage())) {
        resetToRegister(timer);
      } else if (ThirdPartyException.Error.HEAVY_PING.is(tpe.getMessage())) {
        resetToHeavyPing(timer);
      } else if (ThirdPartyException.Source.SETTINGS.equals(tpe.getSource())) {
        resetToDelaContact(timer);
      } else {
        //try again later - maybe it works
      }
    }
  }

  private Timer resetToSettings(Timer timer) {
    timer.cancel();
    state = State.SETTINGS;
    return timerService.createTimer(0, settings.getHOPSSITE_HEARTBEAT_RETRY(), "Timer for dela settings.");
  }

  private Timer resetToDelaVersion(Timer timer) {
    timer.cancel();
    state = State.DELA_VERSION;
    return timerService.createTimer(0, settings.getHOPSSITE_HEARTBEAT_RETRY(), "Timer for dela version.");
  }
  
  private Timer resetToDelaContact(Timer timer) {
    timer.cancel();
    state = State.DELA_CONTACT;
    return timerService.createTimer(0, settings.getHOPSSITE_HEARTBEAT_RETRY(), "Timer for dela contact.");
  }

  private Timer resetToRegister(Timer timer) {
    LOG.log(Level.WARNING, "reset from:{0} to REGISTER", state);
    timer.cancel();
    state = State.REGISTER;
    return timerService.createTimer(0, settings.getHOPSSITE_HEARTBEAT_RETRY(), "Timer for hops site register.");
  }

  private Timer resetToHeavyPing(Timer timer) {
    LOG.log(Level.WARNING, "reset from:{0} to HEAVY_PING", state);
    timer.cancel();
    state = State.HEAVY_PING;
    return timerService.createTimer(0, settings.getHOPSSITE_HEARTBEAT_RETRY(), "Timer for heavy ping");
  }

  private Timer resetToPing(Timer timer) {
    LOG.log(Level.WARNING, "reset from:{0} to PING", state);
    timer.cancel();
    state = State.PING;
    return timerService.createTimer(0, settings.getHOPSSITE_HEARTBEAT_INTERVAL(), "Timer for ping");
  }

  private String getDelaTransferHttpEndpoint() throws HeartbeatException {
    String delaHttpEndpoint = settings.getDELA_TRANSFER_HTTP_ENDPOINT();
    try {
      URL u = new URL(delaHttpEndpoint);
      u.toURI();
      // validate url syntax
      if (u.getHost().isEmpty() || u.getPort() == -1) {
        LOG.log(Level.WARNING, "malformed dela url. {0} - reset in database", delaHttpEndpoint);
        throw new HeartbeatException("DELA_TRANSFER_HTTP");
      }
      return delaHttpEndpoint;
    } catch (MalformedURLException | URISyntaxException ex) {
      LOG.log(Level.SEVERE, "malformed dela url. {0} - reset in database", delaHttpEndpoint);
      throw new HeartbeatException("DELA_TRANSFER_HTTP");
    }
  }

  private AddressJSON getDelaTransferEndpoint(String delaVersion) throws ThirdPartyException {
    String delaTransferHttpEndpoint = SettingsHelper.delaTransferHttpEndpoint(settings);
    LOG.log(Level.INFO, "dela http endpoint: {0}", delaTransferHttpEndpoint);
    AddressJSON delaTransferEndpoint = delaCtrl.getDelaPublicEndpoint(delaVersion);
    LOG.log(Level.INFO, "dela transfer endpoint: {0}", delaTransferEndpoint.toString());
    return delaTransferEndpoint;
  }

  private static class HeartbeatException extends Exception {

    public HeartbeatException(String msg) {
      super(msg);
    }
  }

  private static enum State {

    SETTINGS,
    DELA_VERSION,
    DELA_CONTACT,
    REGISTER,
    HEAVY_PING,
    PING
  }
}
