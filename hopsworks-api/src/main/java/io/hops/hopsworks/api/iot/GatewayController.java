package io.hops.hopsworks.api.iot;

import io.hops.hopsworks.common.dao.iot.GatewayFacade;
import io.hops.hopsworks.common.dao.iot.IoTGateways;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.exceptions.GatewayException;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
public class GatewayController {
  
  @EJB
  private GatewayFacade gatewayFacade;
  
  private static final Logger LOGGER = Logger.getLogger(GatewayController.class.getName());
  
  public IoTGateways getGateway(Project project, Integer id) throws GatewayException {
    IoTGateways gateway = gatewayFacade.findByProjectAndId(project, id);
    if (gateway == null) {
      throw new GatewayException(RESTCodes.GatewayErrorCode.GATEWAY_NOT_FOUND, Level.FINEST, "gatewayId:" + id);
    }
    return gateway;
  }
}
