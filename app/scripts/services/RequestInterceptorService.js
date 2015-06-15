'use strict';

angular.module('hopsWorksApp')
  .factory('RequestInterceptorService', ['$q', function ($q) {
    return {
      request: function (config) {

        var RESOURCE_SERVER = 'http://193.10.67.226:19931/hopsworks';
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
