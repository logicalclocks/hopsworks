/*jshint undef: false, unused: false, indent: 2*/
/*global angular: false */

'use strict';

angular.module('hopsWorksApp')
        .factory('ClusterUtilService', ['$http', function ($http) {
            return {
              getYarnMetrics: function () {
                return $http.get('/api/clusterUtilisation/metrics');
              },
              getHdfsStatus: function () {
                return $http.get('/api/kmon/services/HDFS');
              },
              getYarnStatus: function () {
                return $http.get('/api/kmon/services/YARN');
              },
              getKafkaStatus: function () {
                return $http.get('/api/kmon/services/kafka');
              }
            };
          }]);


