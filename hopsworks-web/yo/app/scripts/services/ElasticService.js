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
                },
                /**
                 * Search under a dataset hitting hitting 'dataset' index
                 * 
                 * @param {type} projectId
                 * @param {type} datasetName
                 * @param {type} searchTerm
                 * @returns {unresolved}
                 */
                datasetSearch: function (projectId, datasetName, searchTerm) {
                  return $http.get('/api/elastic/datasetsearch/' + projectId + '/' + datasetName + '/' + searchTerm);
                }
              };
              return services;
            };
          }]);

