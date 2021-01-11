/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.provenance.ops;

import io.hops.hopsworks.common.provenance.core.ProvParser;
import io.hops.hopsworks.common.provenance.core.Provenance;
import io.hops.hopsworks.common.provenance.core.elastic.BasicElasticHit;
import io.hops.hopsworks.common.provenance.core.elastic.ElasticAggregationParser;
import io.hops.hopsworks.common.provenance.ops.ProvOps;
import io.hops.hopsworks.common.provenance.ops.ProvOpsAggregations;
import io.hops.hopsworks.common.provenance.ops.dto.ProvOpsDTO;
import io.hops.hopsworks.exceptions.ProvenanceException;
import io.hops.hopsworks.restutils.RESTCodes;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.filter.Filter;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.metrics.TopHits;
import org.elasticsearch.search.sort.SortOrder;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

public class ProvOpsElasticEE {
  
  public static AggregationBuilder getAggregationBuilder(ProvOpsAggregations agg)
    throws ProvenanceException {
    switch(agg) {
      case APP_USAGE: return artifactOpsAppAggregation();
      default:
        throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.INTERNAL_ERROR, Level.WARNING,
          "unknown aggregation" + agg);
    }
  }
  
  public static ElasticAggregationParser<?, ProvenanceException> getAggregationParser(
    ProvOpsAggregations agg)
    throws ProvenanceException {
    switch(agg) {
      case APP_USAGE: return artifactOpsAppAggregationParser();
      default:
        throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.INTERNAL_ERROR, Level.WARNING,
          "unknown aggregation" + agg);
    }
  }
  
  private static AggregationBuilder artifactOpsAppAggregation() {
    return
      AggregationBuilders.terms("artifacts")
        .field(ProvParser.Fields.ML_ID.toString())
          .subAggregation(AggregationBuilders.terms("apps")
            .field(ProvParser.Fields.APP_ID.toString())
            .subAggregation(AggregationBuilders
              .filter("create", QueryBuilders
                .termQuery(ProvOps.Fields.INODE_OPERATION.toString(),
                  Provenance.FileOps.CREATE.toString()))
              .subAggregation(AggregationBuilders
                .topHits("create_op")
                .sort(ProvOps.Fields.TIMESTAMP.toString(), SortOrder.DESC)
                .size(1)))
            .subAggregation(AggregationBuilders
              .filter("delete",
                QueryBuilders.termQuery(ProvOps.Fields.INODE_OPERATION.toString(),
                  Provenance.FileOps.DELETE.toString()))
              .subAggregation(AggregationBuilders
                .topHits("delete_op")
                .sort(ProvOps.Fields.TIMESTAMP.toString(), SortOrder.DESC)
                .size(1)))
            .subAggregation(AggregationBuilders
              .filter("read",
                QueryBuilders.termQuery(ProvOps.Fields.INODE_OPERATION.toString(),
                  Provenance.FileOps.ACCESS_DATA.toString()))
              .subAggregation(AggregationBuilders
                .topHits("read_op")
                .sort(ProvOps.Fields.TIMESTAMP.toString(), SortOrder.DESC)
                .size(1)))
            .subAggregation(AggregationBuilders
              .filter("append",
                QueryBuilders.termQuery(ProvOps.Fields.INODE_OPERATION.toString(),
                  Provenance.FileOps.MODIFY_DATA.toString()))
              .subAggregation(AggregationBuilders
                .topHits("append_op")
                .sort(ProvOps.Fields.TIMESTAMP.toString(), SortOrder.DESC)
                .size(1))));
  }
  
  private static ElasticAggregationParser<ProvOpsDTO, ProvenanceException> artifactOpsAppAggregationParser() {
    return (Aggregations aggregations) -> {
      List<ProvOpsDTO> result = new LinkedList<>();
      Terms artifacts = aggregations.get("artifacts");
      if(artifacts == null) {
        return result;
      }
      for(Terms.Bucket artifactBucket : artifacts.getBuckets()) {
        //artifact and artifactItems(apps)
        ProvOpsDTO artifact = new ProvOpsDTO();
        List<ProvOpsDTO> artifactItems = new ArrayList<>();
        artifact.setItems(artifactItems);
        result.add(artifact);
        
        Terms apps = artifactBucket.getAggregations().get("apps");
        if (apps == null) {
          continue;
        }
        
        for (Terms.Bucket appBucket : apps.getBuckets()) {
          //app and appItems(ops)
          ProvOpsDTO app = new ProvOpsDTO();
          List<ProvOpsDTO> opsItems = new ArrayList<>();
          artifactItems.add(app);
          app.setItems(opsItems);
          
          //create
          Filter createFilter = appBucket.getAggregations().get("create");
          if (createFilter != null) {
            TopHits createOpHits = createFilter.getAggregations().get("create_op");
            if(createOpHits.getHits().getHits().length == 1) {
              BasicElasticHit hit = BasicElasticHit.instance(createOpHits.getHits().getAt(0));
              ProvOpsDTO doc =ProvOpsParser.mlInstance(hit);
              opsItems.add(getMinifiedOp(doc));
              updateArtifact(artifact, doc);
              updateApp(app, doc);
            }
          }
          //delete
          Filter deleteFilter = appBucket.getAggregations().get("delete");
          if (deleteFilter != null) {
            TopHits deleteOpHits = deleteFilter.getAggregations().get("delete_op");
            if(deleteOpHits.getHits().getHits().length == 1) {
              BasicElasticHit hit = BasicElasticHit.instance(deleteOpHits.getHits().getAt(0));
              ProvOpsDTO doc = ProvOpsParser.mlInstance(hit);
              opsItems.add(getMinifiedOp(doc));
              updateArtifact(artifact, doc);
              updateApp(app, doc);
            }
          }
          //read
          Filter readFilter = appBucket.getAggregations().get("read");
          if (readFilter != null) {
            TopHits readOpHits = readFilter.getAggregations().get("read_op");
            if(readOpHits.getHits().getHits().length == 1) {
              BasicElasticHit hit = BasicElasticHit.instance(readOpHits.getHits().getAt(0));
              ProvOpsDTO doc = ProvOpsParser.mlInstance(hit);
              opsItems.add(getMinifiedOp(doc));
              updateArtifact(artifact, doc);
              updateApp(app, doc);
            }
          }
          //append
          Filter appendFilter = appBucket.getAggregations().get("append");
          if (appendFilter != null) {
            TopHits appendOpHits = appendFilter.getAggregations().get("append_op");
            if(appendOpHits.getHits().getHits().length == 1) {
              BasicElasticHit hit = BasicElasticHit.instance(appendOpHits.getHits().getAt(0));
              ProvOpsDTO doc = ProvOpsParser.mlInstance(hit);
              opsItems.add(getMinifiedOp(doc));
              updateArtifact(artifact, doc);
              updateApp(app, doc);
            }
          }
        }
      }
      return result;
    };
  }
  
  public static void updateArtifact(ProvOpsDTO artifact, ProvOpsDTO doc) {
    if(artifact.getMlId() == null) {
      artifact.setMlId(doc.getMlId());
      artifact.setProjectName(doc.getProjectName());
      artifact.setProjectInodeId(doc.getProjectInodeId());
      artifact.setDatasetInodeId(doc.getDatasetInodeId());
      artifact.setDocSubType(doc.getDocSubType().upgradeIfPart());
    }
  }
  
  public static void updateApp(ProvOpsDTO app, ProvOpsDTO doc) {
    if(app.getAppId() == null) {
      app.setAppId(doc.getAppId());
      app.setUserId(doc.getUserId());
    }
  }
  
  private static ProvOpsDTO getMinifiedOp(ProvOpsDTO doc) {
    ProvOpsDTO minOp = new ProvOpsDTO();
    minOp.setInodeOperation(doc.getInodeOperation());
    minOp.setTimestamp(doc.getTimestamp());
    minOp.setReadableTimestamp(doc.getReadableTimestamp());
    return minOp;
  }
  
  private static AggregationBuilder artifactFilesAggregationParser() {
    return
      AggregationBuilders.terms("artifacts")
        .field(ProvParser.Fields.ML_ID.toString())
        .subAggregation(
          AggregationBuilders.terms("files")
            .field(ProvParser.Fields.INODE_ID.toString())
            .subAggregation(AggregationBuilders
              .filter("create", QueryBuilders.termQuery(ProvOps.Fields.INODE_OPERATION.toString(),
                Provenance.FileOps.CREATE.toString()))
              .subAggregation(AggregationBuilders
                .topHits("create_op")
                .sort(ProvOps.Fields.TIMESTAMP.toString(), SortOrder.ASC)
                .size(1)))
            .subAggregation(AggregationBuilders
              .filter("delete", QueryBuilders.termQuery(ProvOps.Fields.INODE_OPERATION.toString(),
                Provenance.FileOps.ACCESS_DATA.toString()))
              .subAggregation(AggregationBuilders
                .topHits("delete_op")
                .sort(ProvOps.Fields.TIMESTAMP.toString(), SortOrder.ASC)
                .size(1)))
            .subAggregation(AggregationBuilders
              .filter("read", QueryBuilders.termQuery(ProvOps.Fields.INODE_OPERATION.toString(),
                Provenance.FileOps.ACCESS_DATA.toString()))
              .subAggregation(AggregationBuilders
                .topHits("first_read")
                .sort(ProvOps.Fields.TIMESTAMP.toString(), SortOrder.ASC)
                .size(1)))
            .subAggregation(AggregationBuilders
              .filter("append", QueryBuilders.termQuery(ProvOps.Fields.INODE_OPERATION.toString(),
                Provenance.FileOps.ACCESS_DATA.toString()))
              .subAggregation(AggregationBuilders
                .topHits("first_append")
                .sort(ProvOps.Fields.TIMESTAMP.toString(), SortOrder.ASC)
                .size(1))));
  }
}