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
package io.hops.hopsworks.common.commands.featurestore.search;

import io.hops.hopsworks.common.commands.CommandException;
import io.hops.hopsworks.common.featurestore.FeaturestoreFacade;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupFacade;
import io.hops.hopsworks.common.featurestore.featureview.FeatureViewFacade;
import io.hops.hopsworks.common.featurestore.trainingdatasets.TrainingDatasetFacade;
import io.hops.hopsworks.common.opensearch.OpenSearchClientController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.OpenSearchException;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featureview.FeatureView;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.DependsOn;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.logging.Level;
import java.util.logging.Logger;

@Startup
@Singleton
@DependsOn("Settings")
@TransactionAttribute(TransactionAttributeType.NEVER)
public class SearchFSReindexer {
  private static final Logger LOGGER = Logger.getLogger(SearchFSReindexer.class.getName());
  
  @EJB
  private OpenSearchClientController searchClient;
  @EJB
  private FeaturestoreFacade featurestoreFacade;
  @EJB
  private FeaturegroupFacade featureGroupFacade;
  @EJB
  private FeatureViewFacade featureViewFacade;
  @EJB
  private TrainingDatasetFacade trainingDatasetFacade;
  @EJB
  private SearchFSCommandLogger searchFSCommandLogger;
  
  public SearchFSCommandStatus status() {
    return new SearchFSCommandStatus(searchFSCommandLogger.count());
  }
  /**
   * For this reindex mechanism to work properly, the cluster needs to be idle while this is ongoing
   * This works the same as the epipe reindex
   * @throws OpenSearchException
   * @throws FeaturestoreException
   */
  public void reindex() throws OpenSearchException, FeaturestoreException, CommandException {
    if(searchFSCommandLogger.count() != 0) {
      String msg = "make sure command_search_fs is empty before trigerring a reindex";
      throw new CommandException(RESTCodes.CommandErrorCode.INVALID_SQL_QUERY, Level.INFO, msg);
    }
    LOGGER.info("reindexing featurestore search");
    try {
      searchClient.mngIndexDelete(Settings.FEATURESTORE_INDEX);
    } catch(OpenSearchException e) {
      if(e.getErrorCode().equals(RESTCodes.OpenSearchErrorCode.OPENSEARCH_INDEX_NOT_FOUND)) {
        //index doesn't exist, nothing to delete
        return;
      }
    }
    for (Featurestore featureStore : featurestoreFacade.findAll()) {
      reindex(featureStore);
    }
  }
  
  private void reindex(Featurestore featureStore) throws FeaturestoreException {
    for (Featuregroup featuregroup : featureGroupFacade.getByFeatureStore(featureStore, null)) {
      reindex(featuregroup);
    }
    for (FeatureView featureView : featureViewFacade.getByFeatureStore(featureStore, null)) {
      reindex(featureView);
    }
    for (TrainingDataset trainingDataset : trainingDatasetFacade.findByFeaturestore(featureStore)) {
      reindex(trainingDataset);
    }
  }
  
  private void reindex(Featuregroup featuregroup) throws FeaturestoreException {
    searchFSCommandLogger.create(featuregroup);
    searchFSCommandLogger.updateMetadata(featuregroup);
    searchFSCommandLogger.updateTags(featuregroup);
    searchFSCommandLogger.updateKeywords(featuregroup);
  }
  
  private void reindex(FeatureView featureView) throws FeaturestoreException {
    searchFSCommandLogger.create(featureView);
    searchFSCommandLogger.updateMetadata(featureView);
    searchFSCommandLogger.updateTags(featureView);
    searchFSCommandLogger.updateKeywords(featureView);
  }
  
  private void reindex(TrainingDataset trainingDataset) throws FeaturestoreException {
    searchFSCommandLogger.create(trainingDataset);
    searchFSCommandLogger.updateMetadata(trainingDataset);
    searchFSCommandLogger.updateTags(trainingDataset);
    searchFSCommandLogger.updateKeywords(trainingDataset);
  }
}
