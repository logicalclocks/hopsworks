package io.hops.kibana;

/**
 * Enumeration that defines the filtered URIs of Kibana going through
 * HopsWorks proxy servlet.
 * <p>
 * <p>
 * List of requests
 * 1.
 * http://tkak2.sics.se:8080/hopsworks/kibana/elasticsearch/_mget?timeout=0&ignore_unavailable=true&preference=1484560870227
 * (filter request params)
 * 2.
 * http://tkak2.sics.se:8080/hopsworks/kibana/elasticsearch/.kibana/index-pattern/_search?fields=
 * (filter json response)
 * 3.
 * "http://tkak2.sics.se:8080/hopsworks/kibana/elasticsearch/.kibana/_mapping/*
 * /field/_source?_=1484560870948 (do nothing)
 * 4.
 * http://tkak2.sics.se:8080/hopsworks/kibana/elasticsearch/_msearch?timeout=0&ignore_unavailable=true&preference=1484560870227
 * (filter request payload)
 * 5.
 * http://tkak2.sics.se:8080/hopsworks/kibana/elasticsearch/.kibana/index-pattern/demo_admin000
 * (filter uri)
 * 6. http://tkak2.sics.se:8080/hopsworks/kibana/elasticsearch/logstash-*
 * /_mapping/field/*?_=1484561606214&ignore_unavailable=false&allow_no_indices=false&include_defaults=true
 * (filter index in URI)
 * 7.
 * http://tkak2.sics.se:8080/hopsworks/kibana/elasticsearch/.kibana/search/_search?size=100
 * (filter saved searches in response, should be prefixed with projectId)
 * 8.
 * http://tkak2.sics.se:8080/hopsworks/kibana/elasticsearch/.kibana/visualization/_search?size=100
 * (similar 7)
 * 9.
 * http://tkak2.sics.se:8080/hopsworks/kibana/elasticsearch/.kibana/dashboard/_search?size=100
 * (similar to 7)
 * <p>
 * <p>
 */
public enum KibanaFilter {
  MGET,
  KIBANA_INDEXPATTERN_SEARCH, //DONE
  KIBANA_MAPPING_FIELD_SOURCE,
  MSEARCH,
  KIBANA_INDEXPATTERN, //DONE
  INDEXNAME,
  KIBANA_SEARCH,
  KIBANA_VISUALIZATION,
  KIBANA_DASHBOARD
}
