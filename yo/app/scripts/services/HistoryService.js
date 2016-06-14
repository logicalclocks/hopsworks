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
              
              getHeuristicsForJob: function (jobId) {
                console.log("You are in getHeuristicsForJob");  
                return $http.get('/api/history/heuristics/jobs/' + jobId);
              }
              
            };
            return service;
          }]);
