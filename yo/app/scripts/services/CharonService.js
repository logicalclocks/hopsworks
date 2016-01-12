/*jshint undef: false, unused: false, indent: 2*/
/*global angular: false */

'use strict';

angular.module('hopsWorksApp')
    .factory('CharonService', ['$http', function ($http) {
        return function (id) {
          var services = {
            copyFromHdfsToCharon: function (op) {
              var regReq = {
                method: 'POST',
                url: '/api/project/' + id + '/charon/fromHDFS',
                headers: {
                  'Content-Type': 'application/json'
                },
                data: op
              };

              return $http(regReq);
            },
            copyFromCharonToHdfs: function (op) {
              var regReq = {
                method: 'POST',
                url: '/api/project/' + id + '/charon/toHDFS',
                headers: {
                  'Content-Type': 'application/json'
                },
                data: op
              };

              return $http(regReq);
            },
            getMySiteId: function () {
              return $http.get('/api/banner');
            },
          };
          return services;
        };
      }]);
