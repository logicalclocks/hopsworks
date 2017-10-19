'use strict';
angular.module('hopsWorksApp')
        .factory('DelaClusterService', ['$http', function ($http) {
            var service = {
              getLocalPublicDatasets: function() {
                return $http({
                  method: 'GET',
                  url: '/api/delacluster',
                  isArray: true});         
              }
            };
            return service;
          }]);
