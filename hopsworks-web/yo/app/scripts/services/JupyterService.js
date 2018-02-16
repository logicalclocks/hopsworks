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

