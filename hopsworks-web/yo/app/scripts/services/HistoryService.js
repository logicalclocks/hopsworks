/*
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
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
