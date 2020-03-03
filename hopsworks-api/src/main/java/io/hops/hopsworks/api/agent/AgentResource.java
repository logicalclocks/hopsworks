/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
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
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.hops.hopsworks.api.agent;

import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.common.agent.AgentController;
import io.hops.hopsworks.common.dao.command.HeartbeatReplyDTO;
import io.hops.hopsworks.common.dao.command.SystemCommand;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.common.dao.python.CondaCommands;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Path("/agentresource")
@Stateless
@RolesAllowed({"HOPS_ADMIN", "AGENT"})
@Api(value = "Agent Service")
@TransactionAttribute(TransactionAttributeType.NEVER)
public class AgentResource {
  
  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private AgentController agentController;

  final static Logger logger = Logger.getLogger(AgentResource.class.getName());

  public enum AgentAction {
    PING,
    HEARTBEAT,
    REGISTER,
    NONE;
    
    public static AgentAction fromString(String action) {
      return valueOf(action.toUpperCase());
    }
  }
  
  @ApiOperation(value = "Handle kagent operations such as register, heartbeat, ping",
      response = AgentView.class)
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response handle(
      @ApiParam(value = "Agent request", required = true) AgentView request,
      @ApiParam(value = "Action to be performed on agent resource", required = true)
      @QueryParam("action")
      @DefaultValue("NONE") AgentAction action, @Context SecurityContext sc) throws ServiceException {

    switch (action) {
      case PING:
        logger.log(Level.FINE, "AgentResource: Ping");
        handlePing();
        return Response.ok().build();
      case HEARTBEAT:
        logger.log(Level.FINE, "AgentResource: Heartbeat");
        final HeartbeatReplyDTO commands = handleHeartbeat(request);
        
        final AgentView agentHBReply = heartbeatReplyToAgentView(commands);
        
        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK)
            .entity(agentHBReply).build();
      case REGISTER:
        logger.log(Level.FINE, "AgentResource: Register");
        handleRegister(request);
        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
      case NONE:
        throw new IllegalArgumentException("Action on AgentResource not specified");
      default:
        throw new IllegalArgumentException("Unknown action: " + action + " on AgentResource");
    }
  }

  private AgentView heartbeatReplyToAgentView(HeartbeatReplyDTO hbReply) {
    final List<SystemCommandView> systemCommands = new ArrayList<>(hbReply.getSystemCommands().size());
    final SystemCommandView.Builder scvBuilder = new SystemCommandView.Builder();
    for (SystemCommand sc : hbReply.getSystemCommands()) {
      systemCommands.add(scvBuilder.reset()
          .setOp(sc.getOp())
          .setStatus(sc.getStatus())
          .setCommandId(sc.getId())
          .setArguments(sc.getCommandArgumentsAsString())
          .setPriority(sc.getPriority())
          .setUser(sc.getExecUser())
          .build());
    }
    
    final List<CondaCommandView> condaCommands = new ArrayList<>(hbReply.getCondaCommands().size());
    final CondaCommandView.Builder ccvBuilder = new CondaCommandView.Builder();
    for (CondaCommands cc : hbReply.getCondaCommands()) {
      condaCommands.add(ccvBuilder.reset()
          .setCondaOp(cc.getOp())
          .setUser(cc.getUser())
          .setProject(cc.getProj())
          .setCommandId(cc.getId())
          .setArguments(cc.getArg())
          .setStatus(cc.getStatus())
          .setVersion(cc.getVersion())
          .setChannelUrl(cc.getChannelUrl())
          .setInstallType(cc.getInstallType())
          .setLib(cc.getLib())
          .setEnvironmentYml(cc.getEnvironmentYml())
          .setInstallJupyter(cc.getInstallJupyter())
          .build());
    }
    
    final AgentView agentReply = new AgentView();
    agentReply.setSystemCommands(systemCommands);
    agentReply.setCondaCommands(condaCommands);
    return agentReply;
  }
  
  private void handlePing() {
    logger.log(Level.FINE, "Handling ping");
  }
  
  private HeartbeatReplyDTO handleHeartbeat(AgentView request) throws ServiceException {
    if (request == null) {
      throw new IllegalArgumentException("Heartbeat is null");
    }
    return agentController.heartbeat(request.toAgentHeartbeatDTO());
  }
  
  private void handleRegister(AgentView request) throws ServiceException {
    logger.log(Level.FINE, "Handling register");
    if (request == null) {
      throw new IllegalArgumentException("Registration request is null");
    }
    if (request.getHostId() == null || request.getPassword() == null) {
      throw new IllegalArgumentException("Invalid registration request");
    }
    
    agentController.register(request.getHostId(), request.getPassword());
  }
}
