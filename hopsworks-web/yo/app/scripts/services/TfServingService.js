'use strict';
/*
 * Service allowing fetching topic history objects by type.
 */
angular.module('hopsWorksApp')

        .factory('TfServingService', ['$http', function ($http) {
            return {

              /**
               * Get all the tfservings
               * @param {int} projectId
               * @returns {unresolved} A list of tf serving objects.
               */
              getAllServings: function (projectId) {
                return $http.get('/api/project/' + projectId + '/tfserving');
              },
              /**
               * Create a new Serving Tf server in the given project, of the given type.
               * @param {type} projectId
               * @param {type} servingDetails The configuration of the newly created topic.
               * @returns {undefined} The newly created tf server object.
               */
              createNewServing: function (projectId, servingConfig) {
                var req = {
                  method: 'POST',
                  url: '/api/project/' + projectId + '/tfserving/',
                  headers: {
                    'Content-Type': 'application/json'
                  },
                  data: servingConfig
                };
                return $http(req);
              },
              deleteServing: function (projectId, servingId) {
               return $http.delete('/api/project/' + projectId + '/tfserving/' + servingId);
              },
              stopServing: function (projectId, servingId) {
                             return $http.post('/api/project/' + projectId + '/tfserving/stop/' + servingId);
                            },
              startServing: function (projectId, servingId) {
                             return $http.post('/api/project/' + projectId + '/tfserving/start/' + servingId);
                            },
              changeVersion: function (projectId, servingConfig) {
              var req = {
                                method: 'PUT',
                                url: '/api/project/' + projectId + '/tfserving/version/',
                                headers: {
                                  'Content-Type': 'application/json'
                                },
                                data: servingConfig
                              };
                              return $http(req);
                            },
              getLogs: function (projectId, servingId) {
                              return $http.get('/api/project/' + projectId + '/tfserving/logs/' + servingId);
              }

            };
            return service;
          }]);