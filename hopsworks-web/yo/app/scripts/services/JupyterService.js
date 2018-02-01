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

angular.module('hopsWorksApp')
        .factory('JupyterService', ['$http', function ($http) {
            return {
              getAll: function (projectId) {
                return $http.get('/api/project/' + projectId + '/jupyter');
              },
              running: function (projectId) {
                return $http.get('/api/project/' + projectId + '/jupyter/running');
              },
              settings: function (projectId) {
                return $http.get('/api/project/' + projectId + '/jupyter/settings');
              },
              start: function (projectId, sparkConfig) {
                var req = {
                  method: 'POST',
                  url: '/api/project/' + projectId + '/jupyter/start',
                  headers: {
                    'Content-Type': 'application/json'
                  },
                  data: sparkConfig
                };
                return $http(req);
              },
              livySessions: function (projectId) {
                return $http.get('/api/project/' + projectId + '/jupyter/livy/sessions');
              },
              stopAdmin: function (projectId, hdfsUsername) {
                return $http.get('/api/project/' + projectId + '/jupyter/stopAdmin/' + hdfsUsername);
              },
              stopDataOwner: function (projectId, hdfsUsername) {
                return $http.get('/api/project/' + projectId + '/jupyter/stopDataOwner/' + hdfsUsername);
              },
              stop: function (projectId) {
                return $http.get('/api/project/' + projectId + '/jupyter/stop');
              },
              convertIPythonNotebook: function (projectId, fileName) {
                return $http.get('/api/project/' + projectId + '/jupyter/convertIPythonNotebook/' + fileName);
              }

            };
          }]);

