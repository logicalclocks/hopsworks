'use strict';

angular.module('hopsWorksApp')
        .factory('JupyterService', ['$http', function ($http) {
            return {
              getAll: function (projectId) {
                return $http.get('/api/project/' + projectId + '/jupyter');
              },
              get: function (projectId) {
                return $http.put('/api/project/' + projectId + '/jupyter/running' );
              },
              delete: function (projectId) {
                return $http.delete('/api/project/' + projectId + '/jupyter/stop');
              },
              start: function (projectId) {
                return $http.get('/api/project/' + projectId + '/jupyter/start' );
              },
              stop: function (projectId) {
                return $http.delete('/api/project/' + projectId + '/jupyter/stop' );
              }              
            };
          }]);

