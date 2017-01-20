'use strict';
/*
 * Service responsible for communicating with the Flink backend.
 */
angular.module('hopsWorksApp')

        .factory('FlinkService', ['$http', function ($http) {
            var service = {
              /**
               * Inspect the jar at the given path.
               * @param {int} projectId
               */
              inspectJar: function (projectId, path) {
                return $http.get('/api/project/' + projectId + '/jobs/flink/inspect/' + path);
              }
            };
            return service;
          }]);
