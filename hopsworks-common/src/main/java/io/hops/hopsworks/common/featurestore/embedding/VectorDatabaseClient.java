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
import io.hops.hopsworks.vectordb.VectorDatabaseFactory;

import javax.annotation.PreDestroy;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class VectorDatabaseClient {

  @EJB
  private OpenSearchClient openSearchClient;
  private VectorDatabase vectorDatabase;
  private static final Logger LOG = Logger.getLogger(EmbeddingController.class.getName());

  public synchronized VectorDatabase getClient() throws FeaturestoreException {
    if (vectorDatabase == null) {
      try {
        vectorDatabase = VectorDatabaseFactory.getOpensearchDatabase(openSearchClient.getClient());
      } catch (OpenSearchException | ServiceDiscoveryException e) {
        throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.COULD_NOT_CREATE_FEATUREGROUP,
            Level.FINE, "Cannot create opensearch vectordb");
      }
    }
    return vectorDatabase;
  }

  @PreDestroy
  private void close() {
    try {
      vectorDatabase.close();
    } catch (Exception ex) {
      LOG.log(Level.SEVERE, null, ex);
    }
  }
}
