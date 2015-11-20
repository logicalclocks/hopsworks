'use strict';
/*
 * Service allowing fetching job history objects by type.
 */
angular.module('hopsWorksApp')

    .factory('BiobankingService', ['$http', function ($http) {
        return function (id) {
          var services = {
            /**
             * Get all the jobs defined in the project with given id.
             * @param {int} projectId
             * @returns {unresolved} A list of job objects.
             */
            getAllConsentsInProject: function () {
              return $http.get('/api/project/' + id + '/biobanking');
            },
            registerConsents: function (consent) {
              var req = {
                method: 'POST',
                url: '/api/project/' + id + '/biobanking',
                headers: {
                  'Content-Type': 'application/json'
                },
                data: consent
              };
              return $http(req);
            }
          };
          return services;
        };
      }]);
