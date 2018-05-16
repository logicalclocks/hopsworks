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

/**
 * @ngdoc service
 * @name hopsWorksApp.PythonDepsService
 * @description
 * # PythonDepsService
 * Service in the hopsWorksApp.
 */
angular.module('hopsWorksApp')
        .factory('PythonDepsService', ['$http', function ($http) {
            return {
              index: function (projectId) {
                return $http.get('/api/project/' + projectId + '/pythonDeps');
              },
              enable: function (projectId, version, pythonKernel) {
                return $http.get('/api/project/' + projectId + '/pythonDeps/enable/' + version + "/" + pythonKernel);
              },
              enableYml: function (projectId, data) {
                var regReq = {
                                  method: 'POST',
                                  url: '/api/project/' + projectId + '/pythonDeps/enableYml',
                                  headers: {'Content-Type': 'application/json'},
                                  data: data,
                                  dataType: "json"
                                };
                return $http(regReq);
              },
              destroyAnaconda: function (projectId) {
                return $http.get('/api/project/' + projectId + '/pythonDeps/destroyAnaconda');
              },
              enabled: function (projectId) {
                return $http.get('/api/project/' + projectId + '/pythonDeps/enabled');
              },
              installed: function (projectId) {
                return $http.get('/api/project/' + projectId + '/pythonDeps/installed');
              },
              failedCondaOps: function (projectId) {
                return $http.get('/api/project/' + projectId + '/pythonDeps/failedCondaOps');
              },
              retryFailedCondaOps: function (projectId) {
                return $http.get('/api/project/' + projectId + '/pythonDeps/retryFailedCondaOps');
              },
              status: function (projectId) {
                return $http.get('/api/project/' + projectId + '/pythonDeps/status');
              },
              environmentTypes: function (projectId) {
               return $http.get('/api/project/' + projectId + '/pythonDeps/environmentTypes');
              },
              exportEnvironment: function (projectId) {
                return $http.get('/api/project/' + projectId + '/pythonDeps/export');
              },
              install: function (projectId, data) {
                var regReq = {
                  method: 'POST',
                  url: '/api/project/' + projectId + '/pythonDeps/install',
                  headers: {'Content-Type': 'application/json'},
                  data: data,
                  dataType: "json"
                };
                return $http(regReq);
              },
              installOneHost: function (projectId, host, data) {
                var regReq = {
                  method: 'POST',
                  url: '/api/project/' + projectId + '/pythonDeps/installOneHost/' + host,
                  headers: {'Content-Type': 'application/json'},
                  data: data,
                  dataType: "json"
                };
                return $http(regReq);
              },
              uninstall: function (projectId, data) {
                var regReq = {
                  method: 'POST',
                  url: '/api/project/' + projectId + '/pythonDeps/remove',
                  headers: {'Content-Type': 'application/json'},
                  data: data,
                  dataType: "json"
                };
                return $http(regReq);
              },
              clearCondaOps: function (projectId, data) {
                var regReq = {
                  method: 'POST',
                  url: '/api/project/' + projectId + '/pythonDeps/clearCondaOps',
                  headers: {'Content-Type': 'application/json'},
                  data: data,
                  dataType: "json"
                };
                return $http(regReq);
              },
              search: function (projectId, data) {
                var regReq = {
                  method: 'POST',
                  url: '/api/project/' + projectId + '/pythonDeps/search',
                  headers: {'Content-Type': 'application/json'},
                  data: data,
                  dataType: "json"
                };
                return $http(regReq);
              },
              clone: function (projectId, projName) {
                return $http.get('/api/project/' + projectId + '/pythonDeps/clone/' + projName);
              }
            };
          }]);
