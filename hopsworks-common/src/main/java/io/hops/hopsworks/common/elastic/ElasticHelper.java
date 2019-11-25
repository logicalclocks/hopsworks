/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.common.elastic;

import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.restutils.RESTCodes;
import org.elasticsearch.index.query.QueryBuilder;

import java.util.logging.Level;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.fuzzyQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchPhraseQuery;
import static org.elasticsearch.index.query.QueryBuilders.prefixQuery;
import static org.elasticsearch.index.query.QueryBuilders.wildcardQuery;

public class ElasticHelper {
  public static QueryBuilder fullTextSearch(String key, String term) {
    return boolQuery()
      .should(matchPhraseQuery(key, term.toLowerCase()))
      .should(prefixQuery(key, term.toLowerCase()))
      .should(fuzzyQuery(key, term.toLowerCase()))
      .should(wildcardQuery(key, String.format("*%s*", term.toLowerCase())));
  }
  
  public static void checkPagination(Settings settings, Integer offset, Integer limit) throws ServiceException {
    if(offset == null) {
      offset = 0;
    }
    if(offset < 0) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.ELASTIC_QUERY_ERROR, Level.INFO,
        "malformed - offset cannot be negative");
    }
    if(limit != null) {
      if(0 > limit || limit > settings.getElasticDefaultScrollPageSize()) {
        throw new ServiceException(RESTCodes.ServiceErrorCode.ELASTIC_QUERY_ERROR, Level.INFO,
          "malformed - limit not between 0 and ELASTIC_DEFAULT_SCROLL_PAGE_SIZE:"
            + settings.getElasticDefaultScrollPageSize());
      }
    }
  }
}
