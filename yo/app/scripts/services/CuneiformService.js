'use strict';
/*
 * Service responsible for communicating with the Cuneiform backend.
 */
angular.module('hopsWorksApp')

        .factory('CuneiformService', ['$http', function ($http) {
            var service = {
              /**
               * Inspects the workflow at the given path.
               * @param {int} projectId
               */
              inspectStoredWorkflow: function (projectId, path) {
                return $http.get('/api/project/' + projectId + '/jobs/cuneiform/inspect/' + path);
              }
            };
            return service;
          }]);
