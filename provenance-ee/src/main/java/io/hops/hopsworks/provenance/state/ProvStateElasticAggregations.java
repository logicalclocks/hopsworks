/*
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.provenance.state;

import io.hops.hopsworks.common.featurestore.xattr.dto.FeaturestoreXAttrsConstants;
import io.hops.hopsworks.common.provenance.core.elastic.ElasticAggregationParser;
import io.hops.hopsworks.exceptions.ProvenanceException;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.stream.Collectors;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ProvStateElasticAggregations {

  private static final String KEYWORD_AGG_TOP_NAME = "keyword_nested";
  private static final String KEYWORD_FIELD = "xattr.keywords.keyword";

  public AggregationBuilder keywordsAggregation() {
    return AggregationBuilders.nested(KEYWORD_AGG_TOP_NAME, FeaturestoreXAttrsConstants.ELASTIC_XATTR).subAggregation(
        AggregationBuilders.terms(FeaturestoreXAttrsConstants.KEYWORDS).field(KEYWORD_FIELD)
    );
  }

  public ElasticAggregationParser<String, ProvenanceException> keywordsAggregationParser() {
    return (Aggregations aggregations) -> {
      ParsedStringTerms terms =
          ((ParsedNested) aggregations.get(KEYWORD_AGG_TOP_NAME)).getAggregations()
              .get(FeaturestoreXAttrsConstants.KEYWORDS);

      return terms.getBuckets().stream()
          .map(MultiBucketsAggregation.Bucket::getKeyAsString)
          .collect(Collectors.toList());
    };
  }
}
