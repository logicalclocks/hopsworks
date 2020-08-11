/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.provenance.ops;

import io.hops.hopsworks.common.provenance.core.ProvParser;
import io.hops.hopsworks.common.provenance.core.Provenance;
import io.hops.hopsworks.common.provenance.core.elastic.ElasticAggregation;
import io.hops.hopsworks.common.provenance.core.elastic.ElasticAggregationParser;
import io.hops.hopsworks.exceptions.ProvenanceException;
import io.hops.hopsworks.restutils.RESTCodes;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.sort.SortOrder;

import java.util.logging.Level;

public class ProvOpsElastic {
  
  public enum Aggregations implements ElasticAggregation {
  
  }
  
  public static AggregationBuilder getAggregationBuilder(Aggregations agg)
    throws ProvenanceException {
    switch(agg) {
      default:
        throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.INTERNAL_ERROR, Level.WARNING,
          "unknown aggregation" + agg);
    }
  }
  
  public static ElasticAggregationParser<?, ProvenanceException> getAggregationParser(Aggregations agg)
    throws ProvenanceException {
    switch(agg) {
      default:
        throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.INTERNAL_ERROR, Level.WARNING,
          "unknown aggregation" + agg);
    }
  }
  
  private static AggregationBuilder appArtifactFootprintABuilder() {
    return
      AggregationBuilders.terms("artifacts")
        .field(ProvParser.Fields.ML_ID.toString())
        .subAggregation(
          AggregationBuilders.terms("files")
            .field(ProvParser.Fields.INODE_ID.toString())
            .subAggregation(AggregationBuilders
              .filter("create", QueryBuilders.termQuery(ProvOpsParser.Fields.INODE_OPERATION.toString(),
                Provenance.FileOps.CREATE.toString()))
              .subAggregation(AggregationBuilders
                .topHits("create_op")
                .sort(ProvOpsParser.Fields.TIMESTAMP.toString(), SortOrder.ASC)
                .size(1)))
            .subAggregation(AggregationBuilders
              .filter("delete", QueryBuilders.termQuery(ProvOpsParser.Fields.INODE_OPERATION.toString(),
                Provenance.FileOps.ACCESS_DATA.toString()))
              .subAggregation(AggregationBuilders
                .topHits("delete_op")
                .sort(ProvOpsParser.Fields.TIMESTAMP.toString(), SortOrder.ASC)
                .size(1)))
            .subAggregation(AggregationBuilders
              .filter("read", QueryBuilders.termQuery(ProvOpsParser.Fields.INODE_OPERATION.toString(),
                Provenance.FileOps.ACCESS_DATA.toString()))
              .subAggregation(AggregationBuilders
                .topHits("first_read")
                .sort(ProvOpsParser.Fields.TIMESTAMP.toString(), SortOrder.ASC)
                .size(1)))
            .subAggregation(AggregationBuilders
              .filter("append", QueryBuilders.termQuery(ProvOpsParser.Fields.INODE_OPERATION.toString(),
                Provenance.FileOps.ACCESS_DATA.toString()))
              .subAggregation(AggregationBuilders
                .topHits("first_append")
                .sort(ProvOpsParser.Fields.TIMESTAMP.toString(), SortOrder.ASC)
                .size(1))));
  }
}