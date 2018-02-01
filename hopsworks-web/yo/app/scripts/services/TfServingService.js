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
            return service;
          }]);