'use strict';
/*
 * Service allowing fetching job history objects by type.
 */
angular.module('hopsWorksApp')

        .factory('BiobankingService', ['$http', function ($http) {
            var service = {
              /**
               * Get all the jobs defined in the project with given id.
               * @param {int} projectId
               * @returns {unresolved} A list of job objects.
               */
              getAllConsentsInProject: function (projectId) {
                return $http.get('/api/project/' + projectId + '/biobanking');
              },
              postConsentFormToProject: function (projectId, consents) {
                var req = {
                  method: 'POST',
                  url: '/api/project/' + projectId + '/biobanking/',
                  headers: {
                    'Content-Type': 'application/json'
                  },
                  data: consent
                };
                return $http(req);
              }
            };
            return service;
          }]);
