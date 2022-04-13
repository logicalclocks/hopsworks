/*
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.provenance.state;

import io.hops.hopsworks.common.featurestore.xattr.dto.FeaturestoreXAttrsConstants;
import io.hops.hopsworks.common.provenance.core.opensearch.OpenSearchAggregationParser;
import io.hops.hopsworks.exceptions.ProvenanceException;
import org.opensearch.search.aggregations.AggregationBuilder;
import org.opensearch.search.aggregations.AggregationBuilders;
import org.opensearch.search.aggregations.Aggregations;
import org.opensearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.opensearch.search.aggregations.bucket.nested.ParsedNested;
import org.opensearch.search.aggregations.bucket.terms.ParsedStringTerms;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.stream.Collectors;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ProvStateOpenSearchAggregations {

  private static final String KEYWORD_AGG_TOP_NAME = "keyword_nested";
  private static final String KEYWORD_FIELD = "xattr.keywords.keyword";

  public AggregationBuilder keywordsAggregation() {
    return AggregationBuilders.nested(KEYWORD_AGG_TOP_NAME, FeaturestoreXAttrsConstants.OPENSEARCH_XATTR)
      .subAggregation(AggregationBuilders.terms(FeaturestoreXAttrsConstants.KEYWORDS).field(KEYWORD_FIELD)
    );
  }

  public OpenSearchAggregationParser<String, ProvenanceException> keywordsAggregationParser() {
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
