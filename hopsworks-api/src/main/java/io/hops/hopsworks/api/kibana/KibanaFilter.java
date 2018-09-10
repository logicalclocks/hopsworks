/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
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
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.hops.hopsworks.api.kibana;

/**
 * Enumeration that defines the filtered URIs of Kibana going through
 * Hopsworks proxy servlet.
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
  KIBANA_SAVED_OBJECTS_API,
  ELASTICSEARCH_SEARCH,//elasticsearch/*/_search
  LEGACY_SCROLL_START,//Used when exporting from kibana management tab
  KIBANA_DEFAULT_INDEX
}