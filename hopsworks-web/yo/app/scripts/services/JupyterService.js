'use strict';

angular.module('hopsWorksApp')
        .factory('JupyterService', ['$http', function ($http) {
            return {
              getAll: function (projectId) {
                return $http.get('/api/project/' + projectId + '/jupyter');
              },
              running: function (projectId) {
                return $http.get('/api/project/' + projectId + '/jupyter/running');
              },
              settings: function (projectId) {
                return $http.get('/api/project/' + projectId + '/jupyter/settings');
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
              livySessions: function (projectId) {
                return $http.get('/api/project/' + projectId + '/jupyter/livy/sessions');
              },
//              getLivySessionAppId: function (projectId, sessionId) {
//                return $http.get('/api/project/'+ projectId + '/jupyter/livy/sessions/appId/' + sessionId);
//              },
              stopAdmin: function (projectId, hdfsUsername) {
                return $http.get('/api/project/' + projectId + '/jupyter/stopAdmin/' + hdfsUsername);
              },
              stopDataOwner: function (projectId, hdfsUsername) {
                return $http.get('/api/project/' + projectId + '/jupyter/stopDataOwner/' + hdfsUsername);
              },
              stop: function (projectId) {
                return $http.get('/api/project/' + projectId + '/jupyter/stop');
              },
              convertIPythonNotebook: function (projectId, fileName) {
                return $http.get('/api/project/' + projectId + '/jupyter/convertIPythonNotebook/' + fileName);
              }

            };
          }]);

