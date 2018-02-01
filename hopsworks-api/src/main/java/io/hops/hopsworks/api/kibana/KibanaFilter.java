/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.api.kibana;

/**
 * Enumeration that defines the filtered URIs of Kibana going through
 * HopsWorks proxy servlet.
 * <p>
 * <p>
 * List of requests (prefix is /hopsworks-api/kibana)
 * 1.
 * /elasticsearch/_mget?timeout=0&ignore_unavailable=true&preference=1484560870227
 * (filter request params)
 * 2.
 * /elasticsearch/.kibana/index-pattern/_search?fields=
 * (filter json response)
 * 3.
 * /elasticsearch/.kibana/_mapping/*
 * /field/_source?_=1484560870948 (do nothing)
 * 4.
 * /elasticsearch/_msearch?timeout=0&ignore_unavailable=true&preference=1484560870227
 * (filter request payload)
 * 5.
 * /elasticsearch/.kibana/index-pattern/demo_admin000
 * (filter uri)
 * 6.
 * /elasticsearch/logstash-*
 * /_mapping/field/*?_=1484561606214&ignore_unavailable
 * =false&allow_no_indices=false&include_defaults=true
 * (filter index in URI)
 * 7.
 * /elasticsearch/.kibana/search/_search?size=100
 * (filter saved searches in response, should be prefixed with projectId)
 * 8.
 * /elasticsearch/.kibana/visualization/_search?size=100
 * (similar 7)
 * 9.
 * /elasticsearch/.kibana/dashboard/_search?size=100
 * (similar to 7)
 * <p>
 * <p>
 */
public enum KibanaFilter {
  MGET,
  KIBANA_INDEXPATTERN_SEARCH, //DONE
  KIBANA_MAPPING_FIELD_SOURCE,
  MSEARCH, //DONE
  KIBANA_INDEXPATTERN, //DONE
  INDEXNAME,
  KIBANA_SEARCH,
  KIBANA_VISUALIZATION,
  KIBANA_DASHBOARD
}
