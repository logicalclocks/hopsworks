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
 * Service allowing fetching topic history objects by type.
 */
angular.module('hopsWorksApp')

        .factory('TfServingService', ['$http', function ($http) {
            return {

              /**
               * Get all the tfservings
               * @param {int} projectId
               * @returns {unresolved} A list of tf serving objects.
               */
              getAllServings: function (projectId) {
                return $http.get('/api/project/' + projectId + '/tfserving');
              },
              /**
               * Create a new Serving Tf server in the given project, of the given type.
               * @param {type} projectId
               * @param {type} servingDetails The configuration of the newly created topic.
               * @returns {undefined} The newly created tf server object.
               */
              createNewServing: function (projectId, servingConfig) {
                var req = {
                  method: 'POST',
                  url: '/api/project/' + projectId + '/tfserving/',
                  headers: {
                    'Content-Type': 'application/json'
                  },
                  data: servingConfig
                };
                return $http(req);
              },
              deleteServing: function (projectId, servingId) {
               return $http.delete('/api/project/' + projectId + '/tfserving/' + servingId);
              },
              stopServing: function (projectId, servingId) {
                             return $http.post('/api/project/' + projectId + '/tfserving/stop/' + servingId);
                            },
              startServing: function (projectId, servingId) {
                             return $http.post('/api/project/' + projectId + '/tfserving/start/' + servingId);
                            },
              changeVersion: function (projectId, servingConfig) {
              var req = {
                                method: 'PUT',
                                url: '/api/project/' + projectId + '/tfserving/version/',
                                headers: {
                                  'Content-Type': 'application/json'
                                },
                                data: servingConfig
                              };
                              return $http(req);
                            },
              getLogs: function (projectId, servingId) {
                              return $http.get('/api/project/' + projectId + '/tfserving/logs/' + servingId);
              }

            };
          }]);