'use strict';
/*
 * Service allowing fetching job history objects by type.
 */
angular.module('hopsWorksApp')

        .factory('JobHistoryService', ['$http', function ($http) {
            var service = {
              /**
               * Gets the stored jobhistory objects for the given projectId and type.
               * @param {int} projectId
               * @param {string} type, capitalised service name.
               * @returns {unresolved} A list of jobhistory objects.
               */
              getByProjectAndType: function (projectId, type) {
                return $http.get('/api/project/' + projectId + '/jobs/history/' + type.toUpperCase());
              }
            };
            return service;
          }]);
