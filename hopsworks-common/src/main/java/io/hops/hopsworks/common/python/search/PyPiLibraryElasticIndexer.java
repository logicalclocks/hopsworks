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

import io.hops.hopsworks.common.elastic.ElasticClientController;
import io.hops.hopsworks.common.util.Settings;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.GetAliasesResponse;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexTemplatesResponse;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.DependsOn;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

@Singleton
@Startup
@DependsOn("Settings")
public class PyPiLibraryElasticIndexer {

  @EJB
  private ElasticClientController elasticClientCtrl;
  @EJB
  private Settings settings;
  @Resource
  private TimerService timerService;

  private boolean isIndexed = false;

  private static final Logger LOGGER = Logger.getLogger(
      PyPiLibraryElasticIndexer.class.getName());

  public boolean isIndexed() {
    return this.isIndexed;
  }

  @PostConstruct
  public void init() {
    scheduleTimer(0);
  }

  private void scheduleTimer(long duration) {
    timerService.createSingleActionTimer(duration, new TimerConfig("PyPi Search Indexer", false));
  }

  @Timeout
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public void execute(Timer timer) {
    long errorRescheduleTimeout = 300000;
    
    LOGGER.log(Level.INFO, "Running PyPi Indexer");
  
    try {
      ClusterHealthResponse clusterHealthResponse = elasticClientCtrl.clusterHealthGet();
      GetIndexTemplatesResponse templateResponse =
        elasticClientCtrl.templateGet(Settings.ELASTIC_PYPI_LIBRARIES_ALIAS);
    
      //If elasticsearch is down or template not in elastic reschedule the timer
      if(clusterHealthResponse.getStatus().equals(ClusterHealthStatus.RED)) {
        scheduleTimer(errorRescheduleTimeout);
        LOGGER.log(Level.INFO, "Elastic currently down, rescheduling indexing for pypi libraries");
        return;
      } else if(templateResponse.getIndexTemplates().isEmpty()) {
        scheduleTimer(errorRescheduleTimeout);
        LOGGER.log(Level.INFO, "Elastic template " + Settings.ELASTIC_PYPI_LIBRARIES_ALIAS + " currently missing, " +
          "rescheduling indexing for pypi libraries");
        return;
      }
    } catch(Exception e) {
      scheduleTimer(errorRescheduleTimeout);
      LOGGER.log(Level.SEVERE, "Exception occurred trying to index pypi libraries, rescheduling timer", e);
      return;
    }
  
    try {
      GetAliasesResponse pypiAlias = elasticClientCtrl.getAliases(Settings.ELASTIC_PYPI_LIBRARIES_ALIAS);
    
      if(!pypiAlias.getAliases().isEmpty()) {
        this.isIndexed = true;
      }
    
      String[] indicesToDelete = elasticClientCtrl.mngIndicesGet(Settings.ELASTIC_PYPI_LIBRARIES_INDEX_REGEX);
    
      String newIndex = Settings.ELASTIC_PYPI_LIBRARIES_INDEX_PATTERN_PREFIX + System.currentTimeMillis();
      CreateIndexRequest createIndexRequest = new CreateIndexRequest(newIndex);
      elasticClientCtrl.mngIndexCreate(createIndexRequest);
    
      Element body = Jsoup.connect(settings.getPyPiSimpleEndpoint()).maxBodySize(0).get().body();
      Elements elements = body.getElementsByTag("a");
    
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
          elasticClientCtrl.bulkUpdateDoc(bulkRequest);
          bulkRequest = new BulkRequest();
          currentBulkSize = 0;
        }
      }
    
      //Also send last batch
      if(bulkRequest.numberOfActions() > 0) {
        elasticClientCtrl.bulkUpdateDoc(bulkRequest);
      }
    
      if(pypiAlias.getAliases().isEmpty()) {
        elasticClientCtrl.createAlias(Settings.ELASTIC_PYPI_LIBRARIES_ALIAS, newIndex);
      } else {
        String currentSearchIndex = pypiAlias.getAliases().keySet().iterator().next();
        elasticClientCtrl.aliasSwitchIndex(Settings.ELASTIC_PYPI_LIBRARIES_ALIAS, currentSearchIndex, newIndex);
      }
      this.isIndexed = true;
    
      LOGGER.log(Level.INFO, "Finished indexing");
    
      for (String index : indicesToDelete) {
        DeleteIndexRequest deleteIndexRequest = new DeleteIndexRequest().indices(index);
        elasticClientCtrl.mngIndexDelete(deleteIndexRequest);
      }
    } catch(Exception ex) {
      LOGGER.log(Level.SEVERE, "Indexing pypi libraries failed", ex);
      scheduleTimer(errorRescheduleTimeout);
      return;
    }
    String rawInterval = settings.getPyPiIndexerTimerInterval();
    Long intervalValue = settings.getConfTimeValue(rawInterval);
    TimeUnit intervalTimeunit = settings.getConfTimeTimeUnit(rawInterval);
    scheduleTimer(intervalTimeunit.toMillis(intervalValue));
  }
}
