'use strict';
/*
 * Service responsible for communicating with the Spark backend.
 */
angular.module('hopsWorksApp')

        .factory('SparkService', ['$http', function ($http) {
            var service = {
              /**
               * Inspect the jar at the given path.
               * @param {int} projectId
               */
              inspectJar: function (projectId, path) {
                return $http.get('/api/project/' + projectId + '/jobs/spark/inspect/' + path);
              },
              runJob: function (projectId, runConfig) {
                var req = {
                  method: 'POST',
                  url: '/api/project/' + projectId + '/jobs/spark/run',
                  headers: {
                    'Content-Type': 'application/json'
                  },
                  data: runConfig
                }
                return $http(req);
              }
            };
            return service;
          }]);
