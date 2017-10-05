'use strict';

angular.module('hopsWorksApp')
        .factory('RequestInterceptorService', ['$location', '$q', 
            function ($location, $q) {
            return {
              request: function (config) {

                var RESOURCE_SERVER = getApiLocationBase(); 
                var RESOURCE_NAME = 'api';
                var KIBANA_NAME = "kibana";

                var isApi = config.url.indexOf(RESOURCE_NAME);
                var isKibana = config.url.startsWith("/"+KIBANA_NAME);
                var isCross = config.url.indexOf('http');
                
                if ((isApi !== -1 || isKibana) && isCross === -1) {
                  config.url = RESOURCE_SERVER + config.url;
                  return config || $q.when(config);
                } else {
                  return config;
                }

              }
            };
          }]);
