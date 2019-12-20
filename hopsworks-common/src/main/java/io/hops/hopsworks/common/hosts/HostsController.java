/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 */
package io.hops.hopsworks.common.hosts;

import com.google.common.base.Strings;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.host.HostDTO;
import io.hops.hopsworks.common.dao.host.Hosts;
import io.hops.hopsworks.common.dao.host.HostsFacade;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.Optional;
import java.util.logging.Level;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class HostsController {
  
  @EJB
  private HostsFacade hostsFacade;
  
  public Hosts findByHostname(String hostname) throws ServiceException {
    Optional<Hosts> optional = hostsFacade.findByHostname(hostname);
    if (optional.isPresent()) {
      return optional.get();
    } else {
      throw new ServiceException(RESTCodes.ServiceErrorCode.HOST_NOT_FOUND, Level.WARNING, "hostname: " + hostname);
    }
  }
  
  public boolean removeByHostname(String hostname) {
    if (hostname == null) {
      throw new IllegalArgumentException("Hostname was not provided");
    }
    
    Optional<Hosts> optional = hostsFacade.findByHostname(hostname);
    if (optional.isPresent()) {
      hostsFacade.remove(optional.get());
      return true;
    }
    return false;
  }
  
  public Response addOrUpdateClusterNode(UriInfo uriInfo, String hostname, HostDTO updateNode) {
    Optional<Hosts> optional = hostsFacade.findByHostname(hostname);
    if (optional.isPresent()) {
      return updateExistingClusterNode(optional.get(), updateNode);
    } else {
      return createNewClusterNode(uriInfo, hostname, updateNode);
    }
  }
  
  private Response createNewClusterNode(UriInfo uriInfo, String hostname, HostDTO updateNode) {
    // Do some sanity check
    if (Strings.isNullOrEmpty(updateNode.getHostname())) {
      throw new IllegalArgumentException("Hostname was not provided");
    }
    
    if (Strings.isNullOrEmpty(updateNode.getHostIp())) {
      throw new IllegalArgumentException("Host ip was not provided");
    }
    
    if (updateNode.getCondaEnabled() == null) {
      updateNode.setCondaEnabled(false);
    }
    
    if (updateNode.getNumGpus() == null) {
      updateNode.setNumGpus(0);
    }
  
    // Make sure we store what we want in the DB and not what the user wants to
    Hosts finalNode = new Hosts();
    finalNode.setHostname(updateNode.getHostname());
    finalNode.setHostIp(updateNode.getHostIp());
    finalNode.setCondaEnabled(updateNode.getCondaEnabled());
    finalNode.setNumGpus(updateNode.getNumGpus());
    hostsFacade.save(finalNode);
    URI uri = uriInfo.getBaseUriBuilder()
      .path(ResourceRequest.Name.HOSTS.toString())
      .path(finalNode.getHostname())
      .build();
    return Response.created(uri).entity(finalNode).build();
  }
  
  private Response updateExistingClusterNode(Hosts storedNode, HostDTO updateNode) {
    if (updateNode.getHostIp() != null && !updateNode.getHostIp().isEmpty()) {
      storedNode.setHostIp(updateNode.getHostIp());
    }
  
    if (updateNode.getPublicIp() != null && !updateNode.getPublicIp().isEmpty()) {
      storedNode.setPublicIp(updateNode.getPublicIp());
    }
  
    if (updateNode.getPrivateIp() != null && !updateNode.getPrivateIp().isEmpty()) {
      storedNode.setPrivateIp(updateNode.getPrivateIp());
    }
  
    if (updateNode.getAgentPassword() != null && !updateNode.getAgentPassword().isEmpty()) {
      storedNode.setAgentPassword(updateNode.getAgentPassword());
    }
  
    if (updateNode.getCondaEnabled() != null) {
      storedNode.setCondaEnabled(updateNode.getCondaEnabled());
    }
    hostsFacade.update(storedNode);
    return Response.noContent().build();
  }
}
