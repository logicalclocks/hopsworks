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
              
              getDetailsForJob: function (jobId) {
                return $http.get('/api/history/details/jobs/' + jobId);
              },
       
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
