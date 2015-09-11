/*jshint undef: false, unused: false, indent: 2*/
/*global angular: false */

'use strict';

angular.module('hopsWorksApp')
        .factory('ElasticService', ['$http', function ($http) {
            return function () {
              var services = {
                /**
                 * Do a global search hitting two indices: 'project' and 'datasets'
                 * 
                 * @param {type} searchTerm
                 * @returns {unresolved}
                 */
                globalSearch: function (searchTerm) {
                  console.log("loooking for " + searchTerm);
                  return $http.get('/api/elastic/globalsearch/' + searchTerm);
                },
                /**
                 * Search under a project hitting hitting 'project' index
                 * 
                 * @param {type} projectName
                 * @param {type} searchTerm
                 * @returns {unresolved}
                 */
                projectSearch: function (projectName, searchTerm) {
                  return $http.get('/api/elastic/projectsearch/' + projectName + '/' + searchTerm);
                }
              };
              return services;
            };
          }]);

