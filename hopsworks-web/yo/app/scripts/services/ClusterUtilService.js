/*jshint undef: false, unused: false, indent: 2*/
/*global angular: false */

'use strict';

angular.module('hopsWorksApp')
        .factory('ClusterUtilService', ['$http', function ($http) {
            return {
              getYarnmultiplicator: function () {
                return $http.get('/api/clusterUtilisation/multiplicator');
              }
            };
          }]);


