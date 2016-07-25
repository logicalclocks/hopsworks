'use strict';
/*
 * Service allowing fetching the history objects.
 */
angular.module('hopsWorksApp')

        .factory('HistoryService', ['$http', function ($http) {
            var service = {
              /**
               * Get all the History Records.
               * @returns {unresolved} A list of objects in the history server.
               */
              getAllHistoryRecords: function (projectId) {
                return $http.get('/api/history/all/' + projectId);
              },
              
              /**
               * Get the details of a job - history page
               * @returns {unresolved} Specific details for a particular job
               */
              getDetailsForJob: function (jobId) {
                return $http.get('/api/history/details/jobs/' + jobId);
              },
       
              /**
               * POST request with the arguments of a job durin the configuration phase
               * @returns {unresolved} 
               */
              getHeuristics: function (jobDetails) { 
                var req = {
                  method: 'POST',
                  url: '/api/history/heuristics',
                  headers: {
                    'Content-Type': 'application/json'
                  },
                  data: jobDetails
                };
                return $http(req);
              }
              
            };
            return service;
          }]);
