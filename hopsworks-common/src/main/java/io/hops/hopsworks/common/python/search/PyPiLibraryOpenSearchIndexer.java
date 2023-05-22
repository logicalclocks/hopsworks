/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.common.python.search;

import io.hops.hopsworks.common.opensearch.OpenSearchClientController;
import io.hops.hopsworks.common.util.PayaraClusterManager;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.OpenSearchException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.opensearch.action.admin.cluster.health.ClusterHealthResponse;
import org.opensearch.action.admin.indices.delete.DeleteIndexRequest;
import org.opensearch.action.bulk.BulkRequest;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.client.GetAliasesResponse;
import org.opensearch.client.indices.CreateIndexRequest;
import org.opensearch.client.indices.GetIndexTemplatesResponse;
import org.opensearch.cluster.health.ClusterHealthStatus;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.DependsOn;
import javax.ejb.EJB;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerService;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.OptionalLong;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.opensearch.common.xcontent.XContentFactory.jsonBuilder;

@Singleton
@Startup
@DependsOn("Settings")
@Lock(LockType.READ)
public class PyPiLibraryOpenSearchIndexer {

  @EJB
  private OpenSearchClientController openSearchClientCtrl;
  @EJB
  private Settings settings;
  @EJB
  private PayaraClusterManager payaraClusterManager;
  @Resource
  private TimerService timerService;
  private Timer timer;

  private boolean isIndexed = false;
  
  private static final Logger LOGGER = Logger.getLogger(PyPiLibraryOpenSearchIndexer.class.getName());

  @Lock(LockType.WRITE)
  public void updateIndexed() {
    if(settings.isPyPiIndexerTimerEnabled()) {
      GetAliasesResponse pypiAlias;
      try {
        pypiAlias = openSearchClientCtrl.getAliases(Settings.OPENSEARCH_PYPI_LIBRARIES_ALIAS);
        this.isIndexed = !pypiAlias.getAliases().isEmpty();
      } catch (OpenSearchException e) {
        LOGGER.log(Level.SEVERE, "Exception occurred trying to get indices.", e);
      }
    }
  }

  public boolean isIndexed() {
    if (!this.isIndexed) {
      this.updateIndexed();
    }
    return this.isIndexed;
  }

  @PostConstruct
  public void init() {
    if(settings.isPyPiIndexerTimerEnabled()) {
      timer = timerService.createTimer(0, 600000L, "PyPi Search Indexer");
    } else {
      LOGGER.log(Level.INFO, "PyPi Indexer is disabled, will not index libraries in opensearch");
    }
  }
  
  private String getTimeStampFromIndex(String index) {
    return index.replace(Settings.OPENSEARCH_PYPI_LIBRARIES_INDEX_PATTERN_PREFIX, "");
  }
  
  private long getRecentIndex(Set<String> keySet) {
    if (keySet.size() > 1) {
      LOGGER.log(Level.WARNING, "Found more than one index for Alias: {0}", Settings.OPENSEARCH_PYPI_LIBRARIES_ALIAS);
    }
    OptionalLong index = keySet.stream().map(this::getTimeStampFromIndex).mapToLong(Long::parseLong).max();
    return index.orElse(0L);
  }
  
  private long getIndexAge() {
    try {
      GetAliasesResponse pypiAlias = openSearchClientCtrl.getAliases(Settings.OPENSEARCH_PYPI_LIBRARIES_ALIAS);
      // Delete pypi index and reload ear to force trigger a pypi reindex.
      if (pypiAlias != null && pypiAlias.getAliases() != null && !pypiAlias.getAliases().isEmpty()) {
        long lastIndexed = getRecentIndex(pypiAlias.getAliases().keySet());
        long interval = System.currentTimeMillis() - lastIndexed;
        LOGGER.log(Level.FINE, "PyPi Alias last indexed: {0} HOURS ago", TimeUnit.MILLISECONDS.toHours(interval));
        return interval;
      }
    } catch (OpenSearchException e) {
      LOGGER.log(Level.WARNING, "Failed to get PyPi Alias: {0}", e.getMessage());
    }
    return Long.MAX_VALUE;
  }
  
  private boolean shouldIndex() {
    if (payaraClusterManager.amIThePrimary()) {
      String rawInterval = settings.getPyPiIndexerTimerInterval();
      Long intervalValue = settings.getConfTimeValue(rawInterval);
      TimeUnit intervalTimeunit = settings.getConfTimeTimeUnit(rawInterval);
      long interval = intervalTimeunit.toMillis(intervalValue);
      long lastIndexed = getIndexAge();
      //if last indexed is longer that interval
      return lastIndexed > interval;
    }
    return false;
  }

  @Timeout
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public void execute(Timer timer) throws OpenSearchException {
    if (!shouldIndex()) {
      return;
    }
    LOGGER.log(Level.INFO, "Running PyPi Indexer");
    
    try {
      ClusterHealthResponse clusterHealthResponse = openSearchClientCtrl.clusterHealthGet();
      GetIndexTemplatesResponse templateResponse =
        openSearchClientCtrl.templateGet(Settings.OPENSEARCH_PYPI_LIBRARIES_ALIAS);
    
      //If OpenSearch is down or template not in OpenSearch reschedule the timer
      if(clusterHealthResponse.getStatus().equals(ClusterHealthStatus.RED)) {
        LOGGER.log(Level.INFO, "OpenSearch currently down, rescheduling indexing for pypi libraries");
        return;
      } else if(templateResponse.getIndexTemplates().isEmpty()) {
        LOGGER.log(Level.INFO, "OpenSearch template " + Settings.OPENSEARCH_PYPI_LIBRARIES_ALIAS +
          " currently missing, rescheduling indexing for pypi libraries");
        return;
      }
    } catch(Exception e) {
      LOGGER.log(Level.SEVERE, "Exception occurred trying to index pypi libraries, rescheduling timer", e);
      return;
    }

    String newIndex = Settings.OPENSEARCH_PYPI_LIBRARIES_INDEX_PATTERN_PREFIX + System.currentTimeMillis();

    try {
      GetAliasesResponse pypiAlias = openSearchClientCtrl.getAliases(Settings.OPENSEARCH_PYPI_LIBRARIES_ALIAS);
    
      String[] indicesToDelete = openSearchClientCtrl.mngIndicesGetBySimplifiedRegex(
        Settings.OPENSEARCH_PYPI_LIBRARIES_INDEX_REGEX);
    
      Element body = Jsoup.connect(settings.getPyPiSimpleEndpoint()).maxBodySize(0).get().body();
      Elements elements = body.getElementsByTag("a");

      CreateIndexRequest createIndexRequest = new CreateIndexRequest(newIndex);
      openSearchClientCtrl.mngIndexCreate(createIndexRequest);
    
      final int bulkSize = 100;
      int currentBulkSize = 0;
      int currentId = 0;
      BulkRequest bulkRequest = new BulkRequest();
    
      LOGGER.log(Level.INFO, "Starting to index libraries from pypi simple index");
    
      for (Element library : elements) {
        IndexRequest indexRequest = new IndexRequest()
          .index(newIndex)
          .id(String.valueOf(currentId))
          .source(jsonBuilder()
            .startObject()
            .field("library", library.text())
            .endObject());
      
        bulkRequest.add(indexRequest);
        currentBulkSize += 1;
        currentId += 1;
      
        if(currentBulkSize == bulkSize) {
          openSearchClientCtrl.bulkUpdateDoc(bulkRequest);
          bulkRequest = new BulkRequest();
          currentBulkSize = 0;
        }
      }
    
      //Also send last batch
      if(bulkRequest.numberOfActions() > 0) {
        openSearchClientCtrl.bulkUpdateDoc(bulkRequest);
      }
    
      if(pypiAlias.getAliases().isEmpty()) {
        openSearchClientCtrl.createAlias(Settings.OPENSEARCH_PYPI_LIBRARIES_ALIAS, newIndex);
      } else {
        String currentSearchIndex = pypiAlias.getAliases().keySet().iterator().next();
        openSearchClientCtrl.aliasSwitchIndex(Settings.OPENSEARCH_PYPI_LIBRARIES_ALIAS, currentSearchIndex, newIndex);
      }
    
      LOGGER.log(Level.INFO, "Finished indexing");
    
      for (String index : indicesToDelete) {
        DeleteIndexRequest deleteIndexRequest = new DeleteIndexRequest().indices(index);
        openSearchClientCtrl.mngIndexDelete(deleteIndexRequest);
      }
    } catch(Exception ex) {
      LOGGER.log(Level.SEVERE, "Indexing pypi libraries failed", ex);
      if(openSearchClientCtrl.mngIndexExists(newIndex)) {
        DeleteIndexRequest deleteIndexRequest = new DeleteIndexRequest().indices(newIndex);
        openSearchClientCtrl.mngIndexDelete(deleteIndexRequest);
      }
    }
  }
}
