/*jshint undef: false, unused: false, indent: 2*/
/*global angular: false */

'use strict';

angular.module('hopsWorksApp')
        .service('ElasticService', ['$http', 'TransformRequest', function ($http, TransformRequest) {
            var service = {
              search: function (query, index) {
                console.log(JSON.stringify(query));
                var searchReq = {
                  method: 'POST',
                  //hops server
                  //url: 'http://193.10.66.222:9200/project/' + index + '/_search',
                  url: 'http://193.10.66.125:9200/project/' + index + '/_search',
                  contentType: 'application/x-www-form-urlencoded',
                  data: JSON.stringify(query)
                };

                return $http(searchReq);
              },
              query: function (index, parentName, searchTerm) {
                if (index === "parent") {
                  var parentQuery = {
                    filter: {
                      query: {
                        //combine the results of a prefix and a fuzzy query
                        bool: {
                          must: {
                            //matches names with the given prefix
                            match_phrase_prefix: {
                              name: {
                                query: searchTerm,
                                slop: 0
                              }
                            }
                          },
                          should: {
                            fuzzy_like_this_field: {
                              name: {
                                like_text: searchTerm
                              }
                            }
                          }
                        }
                      }
                    },
                    size: 10,
                    from: 0
                  };
                  return parentQuery;
                }
                else if (index === "child") {
                  var childQuery = {
                    query: {
                      filtered: {
                        query: {
                          has_parent: {
                            type: "parent",
                            query: {
                              match: {
                                name: parentName
                              }
                            }
                          }
                        },
                        filter: {
                          query: {
                            //combine the results of a prefix and a fuzzy query
                            bool: {
                              should:[
                                  {
                                      fuzzy_like_this_field : {
                                          name : {
                                              like_text : searchTerm
                                          }
                                      }
                                  },
                                  {
                                      fuzzy_like_this:{
                                          like_text: searchTerm,
                                          fields: ["name", "EXTENDED_METADATA"],
                                          fuzziness: 0.4
                                      }
                                  }
                              ]
                            }
                          }
                        }
                      }
                    },
                    size: 10,
                    from: 0
                  };
                  return childQuery;
                }
              }
            };
            return service;
          }]);


