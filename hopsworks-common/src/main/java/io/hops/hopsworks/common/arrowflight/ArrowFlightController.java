/*
 * This file is part of Hopsworks
 * Copyright (C) 2024, Hopsworks AB. All rights reserved
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

package io.hops.hopsworks.common.arrowflight;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logicalclocks.servicediscoverclient.service.Service;
import io.hops.hopsworks.common.featurestore.feature.FeatureGroupFeatureDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.cached.FeaturegroupPreview;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupController;
import io.hops.hopsworks.common.featurestore.storageconnectors.StorageConnectorUtil;
import io.hops.hopsworks.common.hosts.ServiceDiscoveryController;
import io.hops.hopsworks.common.project.AccessCredentialsDTO;
import io.hops.hopsworks.common.project.ProjectController;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.FeaturegroupType;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.ondemand.OnDemandFeaturegroup;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.FeaturestoreConnector;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.bigquery.FeatureStoreBigqueryConnector;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.snowflake.FeaturestoreSnowflakeConnector;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;
import io.hops.hopsworks.servicediscovery.HopsworksService;
import io.hops.hopsworks.servicediscovery.tags.FlyingDuckTags;
import org.apache.arrow.flight.Action;
import org.apache.arrow.flight.FlightClient;
import org.apache.arrow.flight.FlightDescriptor;
import org.apache.arrow.flight.FlightInfo;
import org.apache.arrow.flight.FlightRuntimeException;
import org.apache.arrow.flight.FlightStream;
import org.apache.arrow.flight.Location;
import org.apache.arrow.memory.RootAllocator;
import org.apache.arrow.vector.VectorSchemaRoot;
import org.apache.arrow.vector.table.Row;
import org.apache.arrow.vector.table.Table;
import org.apache.arrow.vector.types.pojo.Field;
import org.javatuples.Pair;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Class controlling the interaction with the arrow flight server
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ArrowFlightController {

  @EJB
  protected StorageConnectorUtil storageConnectorUtil;
  @EJB
  private ProjectController projectController;
  @EJB
  protected ServiceDiscoveryController serviceDiscoveryController;
  @EJB
  protected FeaturegroupController featuregroupController;

  private final ObjectMapper objectMapper = new ObjectMapper();

  /**
   * Initializes an Arrow Flight connection to Flying Duck using TLS with the given access credentials
   *
   * @param project      the project that owns the Hive database
   * @param user         the user making the request
   * @return FlightClient
   * @throws FeaturestoreException
   */
  private FlightClient initFlightClient(Project project, Users user)
      throws FeaturestoreException, InterruptedException {
    FlightClient flightClient = null;
    try {
      AccessCredentialsDTO accessCredentialsDTO = projectController.credentials(project, user);

      InputStream caChainInputStream =
          new ByteArrayInputStream(accessCredentialsDTO.getCaChain().getBytes(StandardCharsets.UTF_8));
      InputStream clientCertInputStream =
          new ByteArrayInputStream(accessCredentialsDTO.getClientCert().getBytes(StandardCharsets.UTF_8));
      InputStream clientKeyInputStream =
          new ByteArrayInputStream(accessCredentialsDTO.getClientKey().getBytes(StandardCharsets.UTF_8));

      // Flyingduck port is exposed as server.flyingduck.service.consul, however flyingduck is quite picky
      // when it comes to certificates and it requires the hostname to be flyingduck.service.consul
      // so here we fetch the port from the service discovery and then we build the rest of the name
      Service flyingduckService = serviceDiscoveryController
          .getAnyAddressOfServiceWithDNS(HopsworksService.FLYING_DUCK.getNameWithTag(FlyingDuckTags.server));
      String flyingduckEndpoing = serviceDiscoveryController
          .constructServiceFQDN(HopsworksService.FLYING_DUCK.getName()) + ":" + flyingduckService.getPort();

      flightClient = FlightClient.builder()
          .useTls()
          .allocator(new RootAllocator())
          .location(new Location("grpc+tls://" + flyingduckEndpoing))
          .trustedCertificates(caChainInputStream)
          .clientCertificate(clientCertInputStream, clientKeyInputStream)
          .build();

      // register client certificates
      ArrowFlightCredentialDTO arrowFlightCredentials = new ArrowFlightCredentialDTO(accessCredentialsDTO);
      flightClient.doAction(new Action("register-client-certificates",
              objectMapper.writeValueAsString(arrowFlightCredentials).getBytes()))
          .hasNext();

      return flightClient;
    } catch (Exception e) {
      if (flightClient != null) {
        flightClient.close();
      }
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.COULD_NOT_INITIATE_ARROW_FLIGHT_CONNECTION,
          Level.SEVERE, "project: " + project.getName(), e.getMessage(), e);
    }
  }

  /**
   * Starts Arrow Flight connection to Flying Duck using the given project-user and then executes a query
   *
   * @param query        the read query (Proprietary to Flying Duck)
   * @param project      the project that owns the Hive database
   * @param user         the user making the request
   * @return FeaturegroupPreview
   * @throws FeaturestoreException
   */
  public FeaturegroupPreview executeReadArrowFlightQuery(String query, Project project, Users user)
      throws FeaturestoreException {
    try(FlightClient flightClient = initFlightClient(project, user)) {
      // get flight info
      FlightInfo flightInfo = flightClient.getInfo(FlightDescriptor.command(query.getBytes(StandardCharsets.US_ASCII)));

      // read data
      FeaturegroupPreview featuregroupPreview = new FeaturegroupPreview();
      try(FlightStream flightStream = flightClient.getStream(flightInfo.getEndpoints().get(0).getTicket())) {
        try (VectorSchemaRoot vectorSchemaRootReceived = flightStream.getRoot()) {
          while (flightStream.next()) {
            try (Table table = new Table(vectorSchemaRootReceived.getFieldVectors())) {
              for (Row tableRow: table) {
                FeaturegroupPreview.Row row = new FeaturegroupPreview.Row();
                for (Field field: table.getSchema().getFields()) {
                  row.addValue(new Pair<>(field.getName().toLowerCase(), // UI breaks if header is capitalized
                    tableRow.isNull(field.getName()) ? "" : tableRow.getExtensionType(field.getName()).toString()));
                }
                featuregroupPreview.addRow(row);
              }
            }
          }
        } catch (FlightRuntimeException e) {
          if (e.getMessage().contains("No such file or directory")) {
            return featuregroupPreview; // nothing was writtent to hudi
          }
          throw e;
        }
      }
      return featuregroupPreview;
    } catch (Exception e) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ARROW_FLIGHT_READ_QUERY_ERROR, Level.FINE,
          "project: " + project.getName() + ", Arrow Flight query: " + query, e.getMessage(), e);
    }
  }


  /**
   * Gets Query string that can be used in Arrow Flight server
   *
   * @param featuregroup the featuregroup to preview
   * @param project      the project that owns the Hive database
   * @param user         the user making the request
   * @param tbl             table name
   * @param limit           the number of rows to visualize
   * @return read query (Proprietary to Arrow Flight server)
   * @throws FeaturestoreException
   */
  public String getArrowFlightQuery(Featuregroup featuregroup, Project project, Users user, String tbl, int limit)
      throws FeaturestoreException {
    ArrowFlightQueryDTO queryDto = new ArrowFlightQueryDTO();

    // query
    String query = featuregroupController.getOfflineFeaturegroupQuery(featuregroup, project, user, tbl, limit);
    query = query.replace("`", "\"");
    queryDto.setQueryString(query);

    // features
    List<FeatureGroupFeatureDTO> features = featuregroupController.getFeatures(featuregroup, project, user);
    List<String> featureNames = features.stream().map(FeatureGroupFeatureDTO::getName).collect(Collectors.toList());
    Map<String, List<String>> featureMap = Collections.singletonMap(tbl, featureNames);
    queryDto.setFeatures(featureMap);

    // filters (not necessary since it will always be used only for Preview)
    queryDto.setFilters(null);

    // connectors
    Map<String, ArrowFlightConnectorDTO> connectorMap =
        Collections.singletonMap(tbl, getArrowFlightConnectorDTO(featuregroup));
    queryDto.setConnectors(connectorMap);

    try {
      return objectMapper.writeValueAsString(queryDto);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }

  private ArrowFlightConnectorDTO getArrowFlightConnectorDTO(Featuregroup featuregroup) 
      throws FeaturestoreException{
    ArrowFlightConnectorDTO connector = new ArrowFlightConnectorDTO();
    if (featuregroup.getFeaturegroupType() == FeaturegroupType.ON_DEMAND_FEATURE_GROUP) {
      OnDemandFeaturegroup onDemandFeaturegroup = featuregroup.getOnDemandFeaturegroup();
      FeaturestoreConnector featurestoreConnector = onDemandFeaturegroup.getFeaturestoreConnector();

      connector.setType(featurestoreConnector.getConnectorType().name());
      connector.setOptions(getConnectorOptions(featurestoreConnector));
      connector.setQuery(onDemandFeaturegroup.getQuery().replaceAll(";( )*$", ""));
      connector.setAlias(featuregroupController.getTblName(featuregroup));
      connector.setFilters(null);
    } else {
      connector.setType("hudi");
    }
    return connector;
  }

  /**
   * Gets Map used for connector options in ArrowFlight
   *
   * @param featurestoreConnector   the connector from which to extract options
   * @return Map
   * @throws FeaturestoreException
   */
  private Map<String, String> getConnectorOptions(FeaturestoreConnector featurestoreConnector)
      throws FeaturestoreException {
    Map<String, String> optionMap = new HashMap<String, String>();

    switch (featurestoreConnector.getConnectorType()) {
      case SNOWFLAKE:
        FeaturestoreSnowflakeConnector snowflakeConnector = featurestoreConnector.getSnowflakeConnector();
        optionMap.put("user", snowflakeConnector.getDatabaseUser());
        optionMap.put("account", snowflakeConnector.getUrl()
            .replace("https://", "")
            .replace(".snowflakecomputing.com", ""));
        optionMap.put("database", snowflakeConnector.getDatabaseName() + "/" + snowflakeConnector.getDatabaseSchema());

        if (snowflakeConnector.getPwdSecret() != null) {
          optionMap.put("password", storageConnectorUtil.getSecret(snowflakeConnector.getPwdSecret(), String.class));
        } else {
          optionMap.put("authenticator", "oauth");
          optionMap.put("token", storageConnectorUtil.getSecret(snowflakeConnector.getTokenSecret(), String.class));
        }

        if (snowflakeConnector.getWarehouse() != null) {
          optionMap.put("warehouse", snowflakeConnector.getWarehouse());
        }

        if (snowflakeConnector.getApplication() != null) {
          optionMap.put("application", snowflakeConnector.getApplication());
        }
        break;
      case BIGQUERY:
        FeatureStoreBigqueryConnector connector = featurestoreConnector.getBigqueryConnector();
        optionMap.put("key_path", connector.getKeyPath());
        optionMap.put("project_id", connector.getQueryProject());
        optionMap.put("dataset_id", connector.getDataset());
        optionMap.put("parent_project", connector.getParentProject());
        break;
      default:
        throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_TYPE, Level.SEVERE,
            "Arrow Flight doesn't support connector of type: " + featurestoreConnector.getConnectorType().name());
    }

    return optionMap;
  }

  /**
   * Throws exception if feature group is not supported by Arrow Flight server
   *
   * @param featuregroup the featuregroup
   * @throws FeaturestoreException
   */
  public void checkFeatureGroupSupportedByArrowFlight(Featuregroup featuregroup) throws FeaturestoreException {
    if (featuregroup.getFeaturegroupType() == FeaturegroupType.ON_DEMAND_FEATURE_GROUP) {
      OnDemandFeaturegroup onDemandFeaturegroup = featuregroup.getOnDemandFeaturegroup();
      FeaturestoreConnector featurestoreConnector = onDemandFeaturegroup.getFeaturestoreConnector();
      switch (featurestoreConnector.getConnectorType()) {
        case SNOWFLAKE:
        case BIGQUERY:
          return;
        default:
          throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_TYPE, Level.SEVERE,
              "Arrow Flight doesn't support connector of type: " + featurestoreConnector.getConnectorType().name());
      }
    }
    return;
  }
}
