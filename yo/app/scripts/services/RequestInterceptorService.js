'use strict';

angular.module('hopsWorksApp')
        .factory('RequestInterceptorService', ['$location', '$q', 
            function ($location, $q) {
            return {
              request: function (config) {

                var RESOURCE_SERVER = "http://" + $location.host() + ":" + $location.port() + "/hopsworks"; 
                var RESOURCE_NAME = 'api';

                var isApi = config.url.indexOf(RESOURCE_NAME);

                if (isApi != -1) {
                  config.url = RESOURCE_SERVER + config.url;
                  return config || $q.when(config);
                } else {
                  return config;
                }

              }
            };
          }]);
