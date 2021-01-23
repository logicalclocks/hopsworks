/*
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.provenance.state;

import io.hops.hopsworks.common.provenance.core.elastic.ElasticAggregationParser;
import io.hops.hopsworks.common.provenance.core.elastic.ElasticHelper;
import io.hops.hopsworks.common.provenance.core.elastic.ProvElasticController;
import io.hops.hopsworks.common.provenance.ops.ProvStateAggregations;
import io.hops.hopsworks.common.provenance.util.ProvHelper;
import io.hops.hopsworks.common.provenance.util.functional.CheckedSupplier;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.ElasticException;
import io.hops.hopsworks.exceptions.ProvenanceException;
import org.elasticsearch.action.search.SearchRequest;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ProvStateControllerEE {

  @EJB
  private ProvElasticController client;
  @EJB
  private ProvStateElasticAggregations provStateElasticAggregations;

  public List<String> keywordAggregation() throws ProvenanceException {
    Map<ProvStateAggregations, ElasticAggregationParser<?, ProvenanceException>> aggregationParser = new HashMap<>();
    aggregationParser.put(ProvStateAggregations.FS_KEYWORDS, provStateElasticAggregations.keywordsAggregationParser());

    CheckedSupplier<SearchRequest, ProvenanceException> srF =
        ElasticHelper.baseSearchRequest(Settings.FEATURESTORE_INDEX, 0)
            .andThen(ElasticHelper.withAggregations(Arrays.asList(provStateElasticAggregations.keywordsAggregation())));

    try {
      return client.searchAggregations(srF.get(), aggregationParser)
          .get(ProvStateAggregations.FS_KEYWORDS);
    } catch (ElasticException e) {
      String msg = "provenance - elastic query problem";
      throw ProvHelper.fromElastic(e, msg, msg + " - Keyword aggregation");
    }
  }

}
