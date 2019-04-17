package io.hops.hopsworks.api.iot;

import io.hops.hopsworks.common.dao.iot.IotGatewayFacade;
import io.hops.hopsworks.common.dao.iot.IotGatewayState;
import io.hops.hopsworks.common.dao.iot.IotGatewayConfiguration;
import io.hops.hopsworks.common.dao.iot.IotGateways;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.exceptions.GatewayException;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
public class IotGatewayController {
  
  @EJB
  private IotGatewayFacade iotGatewayFacade;
  
  private static final Logger LOGGER = Logger.getLogger(IotGatewayController.class.getName());
  
  public IotGateways getGateway(Project project, Integer id) throws GatewayException {
    IotGateways gateway = iotGatewayFacade.findByProjectAndId(project, id);
    if (gateway == null) {
      throw new GatewayException(RESTCodes.GatewayErrorCode.GATEWAY_NOT_FOUND, Level.FINEST, "gatewayId:" + id);
    }
    return gateway;
  }
  
  public IotGateways putGateway(Project project, IotGatewayConfiguration config) {
    if (project == null || config == null) {
      throw new IllegalArgumentException("Arguments cannot be null.");
    }
  
    IotGateways gateway = iotGatewayFacade.findByProjectAndId(project, config.getGatewayId());
  
    if (gateway == null) {
      gateway = new IotGateways(config, project, IotGatewayState.ACTIVE);
    } else if (gateway.getState() == IotGatewayState.INACTIVE_BLOCKED) {
      gateway.setState(IotGatewayState.BLOCKED);
    }
    
    gateway = iotGatewayFacade.putIotGateway(gateway);
    return gateway;
  }
}
