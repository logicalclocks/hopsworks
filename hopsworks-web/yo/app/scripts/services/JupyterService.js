'use strict';

angular.module('hopsWorksApp')
        .factory('JupyterService', ['$http', function ($http) {
            return {
              getAll: function (projectId) {
                return $http.get('/api/project/' + projectId + '/jupyter');
              },
              get: function (projectId) {
                return $http.get('/api/project/' + projectId + '/jupyter/running' );
              },
              start: function (projectId, sparkConfig) {
                var req = {
                  method: 'POST',
                  url: '/api/project/' + projectId + '/jupyter/start',
                  headers: {
                    'Content-Type': 'application/json'
                  },
                  data: sparkConfig
                };
                return $http(req);
              },
              stopAdmin: function (projectId, hdfsUsername) {
                return $http.get('/api/project/' + projectId + '/jupyter/stopAdmin/' + hdfsUsername );
              },
              stopDataOwner: function (projectId, hdfsUsername) {
                return $http.get('/api/project/' + projectId + '/jupyter/stopDataOwner/' + hdfsUsername );
              },
              stop: function (projectId) {
                return $http.get('/api/project/' + projectId + '/jupyter/stop' );
              }              
            };
          }]);

