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

package io.hops.hopsworks.api.jobs;

import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.common.dao.jobhistory.Execution;
import io.hops.hopsworks.common.dao.jobhistory.ExecutionFacade;
import io.hops.hopsworks.common.dao.jobhistory.YarnApplicationAttemptStateFacade;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.util.IoUtils;
import io.hops.hopsworks.common.util.Settings;
import org.apache.hadoop.security.AccessControlException;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.io.IOException;
import java.io.StringReader;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class InfluxDBService {
  private static final Logger LOGGER = Logger.getLogger(InfluxDBService.class.
          getName());

  @EJB
  private YarnApplicationAttemptStateFacade appAttemptStateFacade;
  @EJB
  private ExecutionFacade exeFacade;
  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private Settings settings;

  private String appId;

  InfluxDBService setAppId(String appId) {
    this.appId = appId;
    return this;
  }

  @GET
  @Path("/{database}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public Response getMetrics(
          @PathParam("database") String database,
          @QueryParam("columns") String columns,
          @QueryParam("measurement") String measurement,
          @QueryParam("tags") String tags,
          @QueryParam("groupBy") String groupBy,
          @Context SecurityContext sc, @Context HttpServletRequest req) throws
          AppException, AccessControlException {

    // TODO: FIX authentication, check if user has access to project
    // https://github.com/influxdata/influxdb-java/blob/master/src/main/java/org/influxdb/dto/QueryResult.java

    InfluxDB influxdb = InfluxDBFactory.connect(settings.getInfluxDBAddress(),
            settings.getInfluxDBUser(), settings.getInfluxDBPW());
    Response response = null;

    StringBuffer query = new StringBuffer();
    query.append("select " + columns + " from " + measurement);
    query.append(" where " + tags);
    if (groupBy != null) query.append(" group by " + groupBy);

    LOGGER.log(Level.FINE, "Influxdb - Running query: " + query.toString());

    Query q = new Query(query.toString(), database);
    QueryResult reply = influxdb.query(q, TimeUnit.MILLISECONDS);

    if (reply.hasError()) {
      response = noCacheResponse.getNoCacheResponseBuilder(Response.Status.NOT_FOUND).
              entity(reply.getError()).build();
    } else if (reply.getResults().get(0).getSeries() != null) {
      InfluxDBResultDTO influxResults = new InfluxDBResultDTO();
      influxResults.setQuery(query.toString());
      influxResults.setResult(reply);

      response = noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
              entity(influxResults).build();
    } else {
      response = noCacheResponse.getNoCacheResponseBuilder(Response.Status.NO_CONTENT).
              entity("").build();
    }

    influxdb.close();

    return response;
  }

  @GET
  @Path("/allexecutors")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public Response getAllExecutorsMetrics(
          @QueryParam("filters") final String filters,
          @Context SecurityContext sc, @Context HttpServletRequest req) throws
          AppException {

    LOGGER.log(Level.FINE, "Vizops - Retrieving executor information");

    Response response = null;

    Execution execution = exeFacade.findByAppId(appId);
    if (execution != null) {
      boolean isAppRunning = !execution.getState().isFinalState();
      String trackingUrl = appAttemptStateFacade.findTrackingUrlByAppId(appId);
      if (trackingUrl != null && !trackingUrl.isEmpty()) {
        // trackingURL = http://dn0:8088/proxy/<appid>/
        String sparkTrackingUrl = trackingUrl + "api/v1/applications/" + appId + "/allexecutors";
        LOGGER.log(Level.FINE, "Vizops - spark tracking url: " + sparkTrackingUrl);
        LOGGER.log(Level.FINE, "Vizops - Filters: " + filters);

        if (isAppRunning) { // call endpoint through yarn proxy
          try {
            String content = IoUtils.readContentFromWeb(sparkTrackingUrl);
            LOGGER.log(Level.FINE, "Vizops - Live parsing response " + filters);
            response = noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK)
                    .entity(parseAllExecutorResults(content, filters.split(","))).build();
          } catch (IOException e) {
            if (e.getMessage().contains("500")) {
              LOGGER.log(Level.FINE, "Vizops - Error while retrieving allexecutor metrics from live server: "
                      + e.getMessage());
              response = noCacheResponse.getNoCacheResponseBuilder(Response.Status.NO_CONTENT).entity("").build();
            } else {
              LOGGER.log(Level.FINE, "Vizops - YARN proxy metrics retrieval error(not a 500): "
                      + e.getMessage());
              response = noCacheResponse.getNoCacheResponseBuilder(Response.Status.BAD_REQUEST).entity("").build();
            }
          }
        } else { // call history server
          String historyServer = settings.getSparkHistoryServerIp(); // 10.0.2.15:18080
          String historyEndpoint = "http://" + historyServer + "/api/v1/applications/" + appId + "/1/allexecutors";
          LOGGER.log(Level.FINE, "Vizops - App moved to history, calling: " + historyEndpoint);

          try {
            String historyContent = IoUtils.readContentFromWeb(historyEndpoint);
            LOGGER.log(Level.FINE, "Vizops - History parsing response " + filters);
            response = noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK)
                    .entity(parseAllExecutorResults(historyContent, filters.split(","))).build();
          } catch (IOException e) {
            if (e.getMessage().contains("500")) {
              LOGGER.log(Level.FINE, "Vizops - Error while retrieving allexecutor metrics from history server: "
                      + e.getMessage());
              response = noCacheResponse.getNoCacheResponseBuilder(Response.Status.NO_CONTENT).entity("").build();
            } else {
              LOGGER.log(Level.FINE, "Vizops - Spark history metrics retrieval error(not a 500): "
                      + e.getMessage());
              response = noCacheResponse.getNoCacheResponseBuilder(Response.Status.BAD_REQUEST).entity("").build();
            }
          }
        }
      } else {
        response = noCacheResponse.getNoCacheResponseBuilder(Response.Status.NOT_FOUND)
                .entity("TrackingURL not found").build();
      }
    } else {
      response = noCacheResponse.getNoCacheResponseBuilder(Response.Status.NOT_FOUND)
              .entity("App not found").build();
    }

    return response;
  }

  private JsonArray parseAllExecutorResults(String content, String[] filters) {
    JsonReader reader = Json.createReader(new StringReader(content));
    JsonArray processed = reader.readArray();

    if (filters != null && filters.length > 0) {
      // Only choose the elements in the filters array
      JsonArrayBuilder builder = Json.createArrayBuilder();

      for (int i = 0; i < processed.size(); i++) { // loop over each entry: driver, executor 1, 2....
        JsonObject obj = processed.getJsonObject(i);
        JsonObjectBuilder objBuilder = Json.createObjectBuilder();
        objBuilder.add("id", obj.get("id"));

        for (String tag : filters) {
          if (obj.containsKey(tag)) {
            objBuilder.add(tag, obj.get(tag));
          } // else skip
        }

        builder.add(objBuilder.build());
      }

      return builder.build();
    }

    return processed;
  }
}
