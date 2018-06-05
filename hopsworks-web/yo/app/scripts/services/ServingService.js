/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
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
 */

'use strict';
/*
 * Service allowing fetching topic history objects by type.
 */
angular.module('hopsWorksApp')
        .factory('ServingService', ['$http', function ($http) {
            return {

              /**
               * Get UI configuration form the backend
               * @returns {HttpPromise}
               */
              getConfiguration: function() {
                return $http.get('/api/servingconf');
              },

              /**
               * Get all the tfservings
               * @param {int} projectId
               * @returns {unresolved} A list of tf serving objects.
               */
              getAllServings: function (projectId) {
                return $http.get('/api/project/' + projectId + '/serving');
              },

              /**
               * Get information about a serving object
               * @param {int} projectId
               */
              getServing: function(projectId, servingId) {
                return $http.get('/api/project/' + projectId + '/serving/' + servingId);
              },

              /**
               * Create a new Serving Tf server in the given project, of the given type.
               * @param {type} projectId
               * @param {type} servingDetails The configuration of the newly created topic.
               * @returns {undefined} The newly created tf server object.
               */
              createOrUpdate: function (projectId, servingConfig) {
                var req = {
                  method: 'PUT',
                  url: '/api/project/' + projectId + '/serving/',
                  headers: {
                    'Content-Type': 'application/json'
                  },
                  data: servingConfig
                };
                return $http(req);
              },

              /**
               * Start or stop a serving instance.
               */
              startOrStop: function (projectId, servingId, action) {
                var req = {
                  method: 'POST',
                  url: '/api/project/' + projectId + '/serving/' + servingId + '?action=' + action
                };
                return $http(req);
              },

              deleteServing: function (projectId, servingId) {
               return $http.delete('/api/project/' + projectId + '/serving/' + servingId);
              },

              transformGraph: function (projectId, servingId, transformGraph) {
                var req = {
                  method: 'POST',
                  url: '/api/project/' + projectId + '/tfserving/transform/' + servingId,
                  headers: {
                    'Content-Type': 'application/json'
                  },
                  data: transformGraph
                };
                return $http(req);
              },
            };
          }]);
