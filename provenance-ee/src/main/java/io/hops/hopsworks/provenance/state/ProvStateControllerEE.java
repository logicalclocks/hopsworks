/*
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.provenance.state;

import io.hops.hopsworks.common.provenance.core.opensearch.OpenSearchAggregationParser;
import io.hops.hopsworks.common.provenance.core.opensearch.OpenSearchHelper;
import io.hops.hopsworks.common.opensearch.OpenSearchClientController;
import io.hops.hopsworks.common.provenance.state.ProvStateAggregations;
import io.hops.hopsworks.common.provenance.util.ProvHelper;
import io.hops.hopsworks.common.provenance.util.functional.CheckedSupplier;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.OpenSearchException;
import io.hops.hopsworks.exceptions.ProvenanceException;
import org.opensearch.action.search.SearchRequest;

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
  private OpenSearchClientController client;
  @EJB
  private ProvStateOpenSearchAggregations provStateElasticAggregations;

  public List<String> keywordAggregation() throws ProvenanceException {
    Map<ProvStateAggregations, OpenSearchAggregationParser<?, ProvenanceException>> aggregationParser = new HashMap<>();
    aggregationParser.put(ProvStateAggregations.FS_KEYWORDS, provStateElasticAggregations.keywordsAggregationParser());

    CheckedSupplier<SearchRequest, ProvenanceException> srF =
        OpenSearchHelper.baseSearchRequest(Settings.FEATURESTORE_INDEX, 0)
        .andThen(OpenSearchHelper.withAggregations(Arrays.asList(provStateElasticAggregations.keywordsAggregation())));

    try {
      return client.searchAggregations(srF.get(), aggregationParser)
          .get(ProvStateAggregations.FS_KEYWORDS);
    } catch (OpenSearchException e) {
      String msg = "provenance - opensearch query problem";
      throw ProvHelper.fromOpenSearch(e, msg, msg + " - Keyword aggregation");
    }
  }

}
