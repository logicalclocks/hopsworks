/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
 */

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
               * Get the configuration details of a job - history page
               * @returns {unresolved} The configuratio attributes for a particular job
               */
              getConfigurationForJob: function (jobId) {
                return $http.get('/api/history/config/jobs/' + jobId);
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
