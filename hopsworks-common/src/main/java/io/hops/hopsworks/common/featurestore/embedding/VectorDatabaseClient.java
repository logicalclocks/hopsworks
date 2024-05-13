/*
 * This file is part of Hopsworks
 * Copyright (C) 2023, Hopsworks AB. All rights reserved
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

package io.hops.hopsworks.common.featurestore.embedding;

import com.logicalclocks.servicediscoverclient.exceptions.ServiceDiscoveryException;
import io.hops.hopsworks.common.opensearch.OpenSearchClient;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.OpenSearchException;
import io.hops.hopsworks.restutils.RESTCodes;
import io.hops.hopsworks.vectordb.VectorDatabase;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.DependsOn;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@DependsOn("OpenSearchClient")
public class VectorDatabaseClient {

  @EJB
  private OpenSearchClient openSearchClient;
  @EJB
  private OpensearchVectorDatabaseConstrainedRetry vectorDatabase;
  private static final Logger LOG = Logger.getLogger(EmbeddingController.class.getName());

  @PostConstruct
  public void init() {
    try {
      vectorDatabase.init(openSearchClient.getClient());
    } catch (OpenSearchException | ServiceDiscoveryException e) {
      vectorDatabase = null;
      LOG.log(Level.SEVERE, "Cannot create opensearch vectordb client");
    }
  }

  public synchronized VectorDatabase getClient() throws FeaturestoreException {
    if (vectorDatabase != null) {
      return vectorDatabase;
    } else {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.COULD_NOT_CREATE_FEATUREGROUP,
          Level.FINE, "Cannot create opensearch vectordb client.");
    }
  }

  @PreDestroy
  private void close() {
    try {
      if (vectorDatabase != null) {
        vectorDatabase.close();
      }
    } catch (Exception ex) {
      LOG.log(Level.SEVERE, null, ex);
    }
  }
}
