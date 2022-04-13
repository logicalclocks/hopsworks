/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.provenance.ops;

import io.hops.hopsworks.common.provenance.core.ProvParser;
import io.hops.hopsworks.common.provenance.core.Provenance;
import io.hops.hopsworks.common.provenance.core.opensearch.BasicOpenSearchHit;
import io.hops.hopsworks.common.provenance.core.opensearch.OpenSearchAggregationParser;
import io.hops.hopsworks.common.provenance.ops.ProvOps;
import io.hops.hopsworks.common.provenance.ops.ProvOpsAggregations;
import io.hops.hopsworks.common.provenance.ops.dto.ProvOpsDTO;
import io.hops.hopsworks.exceptions.ProvenanceException;
import io.hops.hopsworks.restutils.RESTCodes;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.search.aggregations.AggregationBuilder;
import org.opensearch.search.aggregations.AggregationBuilders;
import org.opensearch.search.aggregations.Aggregations;
import org.opensearch.search.aggregations.bucket.filter.Filter;
import org.opensearch.search.aggregations.bucket.terms.Terms;
import org.opensearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.opensearch.search.aggregations.metrics.TopHits;
import org.opensearch.search.sort.SortOrder;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ProvOpsOpenSearchAggregations {

  public AggregationBuilder getAggregationBuilder(ProvOpsAggregations agg) throws ProvenanceException {
    switch(agg) {
      case APP_USAGE: return artifactOpsAppAggregation();
      case APP_IDS: return appIdAggregation();
      default:
        throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.INTERNAL_ERROR, Level.WARNING,
          "unknown aggregation" + agg);
    }
  }

  public OpenSearchAggregationParser<?, ProvenanceException> getAggregationParser(ProvOpsAggregations agg)
      throws ProvenanceException {
    switch(agg) {
      case APP_USAGE:
        return artifactOpsAppAggregationParser();
      case APP_IDS:
        return appIdAggregationParser();
      default:
        throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.INTERNAL_ERROR, Level.WARNING,
            "unknown aggregation" + agg);
    }
  }

  private static final List<Provenance.FileOps> appAggregationOpsList = Arrays.asList(
      Provenance.FileOps.CREATE,
      Provenance.FileOps.DELETE,
      Provenance.FileOps.ACCESS_DATA,
      Provenance.FileOps.MODIFY_DATA
      );

  private AggregationBuilder artifactOpsAppAggregation() {
    TermsAggregationBuilder appAggregationBuilder = AggregationBuilders.terms("apps")
            .field(ProvParser.Fields.APP_ID.toString());

    for (Provenance.FileOps fileOp : appAggregationOpsList) {
      appAggregationBuilder.subAggregation(AggregationBuilders
          .filter(fileOp.toString(), QueryBuilders
              .termQuery(ProvOps.Fields.INODE_OPERATION.toString(), fileOp.toString()))
          .subAggregation(AggregationBuilders
              .topHits(fileOp.toString() + "_op")
              .sort(ProvOps.Fields.TIMESTAMP.toString(), SortOrder.DESC)
              .size(1)));
    }
    return AggregationBuilders.terms("artifacts")
        .field(ProvParser.Fields.ML_ID.toString())
        .subAggregation(appAggregationBuilder);
  }
  
  private OpenSearchAggregationParser<ProvOpsDTO, ProvenanceException> artifactOpsAppAggregationParser() {
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
          
          for (Provenance.FileOps fileOp : appAggregationOpsList) {
            Filter createFilter = appBucket.getAggregations().get(fileOp.toString());
            if (createFilter != null) {
              TopHits opHits = createFilter.getAggregations().get(fileOp.toString() + "_op");
              if (opHits.getHits().getHits().length == 1) {
                BasicOpenSearchHit hit = BasicOpenSearchHit.instance(opHits.getHits().getAt(0));
                ProvOpsDTO doc = ProvOpsParser.mlInstance(hit);
                opsItems.add(getMinifiedOp(doc));
                updateArtifact(artifact, doc);
                updateApp(app, doc);
              }
            }
          }
        }
      }
      return result;
    };
  }
  
  public void updateArtifact(ProvOpsDTO artifact, ProvOpsDTO doc) {
    if (artifact.getMlId() == null) {
      artifact.setMlId(doc.getMlId());
      artifact.setProjectName(doc.getProjectName());
      artifact.setProjectInodeId(doc.getProjectInodeId());
      artifact.setDatasetInodeId(doc.getDatasetInodeId());
      artifact.setDocSubType(doc.getDocSubType().upgradeIfPart());
    }
  }
  
  public void updateApp(ProvOpsDTO app, ProvOpsDTO doc) {
    if (app.getAppId() == null) {
      app.setAppId(doc.getAppId());
      app.setUserId(doc.getUserId());
    }
  }
  
  private ProvOpsDTO getMinifiedOp(ProvOpsDTO doc) {
    ProvOpsDTO minOp = new ProvOpsDTO();
    minOp.setInodeOperation(doc.getInodeOperation());
    minOp.setTimestamp(doc.getTimestamp());
    minOp.setReadableTimestamp(doc.getReadableTimestamp());
    return minOp;
  }

  private AggregationBuilder appIdAggregation() {
    return AggregationBuilders.terms("apps")
        .field(ProvParser.Fields.APP_ID.toString())
        .subAggregation(AggregationBuilders
            .topHits("recent_op")
            .sort(ProvOps.Fields.TIMESTAMP.toString(), SortOrder.DESC)
            .size(1));
  }

  private OpenSearchAggregationParser<ProvOpsDTO, ProvenanceException> appIdAggregationParser() {
    return (Aggregations aggregations) -> {
      List<ProvOpsDTO> result = new ArrayList<>();
      Terms apps = aggregations.get("apps");
      if (apps == null) {
        return result;
      }

      // Aggregation is bucketed by app. Each app has it's most recent operation as top hit (single element)
      for (Terms.Bucket appBucket : apps.getBuckets()) {
        TopHits recentOpTopHit = appBucket.getAggregations().get("recent_op");
        if (recentOpTopHit.getHits().getHits().length == 1) {
          // Return the most recent op document for the application
          BasicOpenSearchHit hit = BasicOpenSearchHit.instance(recentOpTopHit.getHits().getAt(0));
          result.add(ProvOpsParser.mlInstance(hit));
        }
      }

      return result;
    };
  }
}