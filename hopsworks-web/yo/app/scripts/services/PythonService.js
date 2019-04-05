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

/**
 * @ngdoc service
 * @name hopsWorksApp.PythonService
 * @description
 * # PythonService
 * Service in the hopsWorksApp.
 */
angular.module('hopsWorksApp')
        .factory('PythonService', ['$http', function ($http) {
            return {
              enabled: function (projectId) {
                return $http.get('/api/project/' + projectId + '/python/environments/enabled');
              },
              getLibraries: function (projectId, pythonVersion) {
                return $http.get('/api/project/' + projectId + '/python/environments/' + pythonVersion + '/libraries?expand=commands');
              },
              createEnvironmentFromVersion: function (projectId, version, pythonKernelEnabled) {
                return $http.post('/api/project/' + projectId + '/python/environments/' + version + '?action=create&pythonKernelEnable=' + pythonKernelEnabled);
              },
              createEnvironmentFromYml: function (projectId, environment) {
                var regReq = {
                  method: 'POST',
                  url: '/api/project/' + projectId + '/python/environments',
                  headers: {'Content-Type': 'application/json'},
                  data: environment,
                  dataType: "json"
                };
                return $http(regReq);
              },
              removeEnvironment: function (projectId, pythonVersion) {
                return $http.delete('/api/project/' + projectId + '/python/environments/' + pythonVersion);
              },
              getEnvironments: function (projectId) {
                return $http.get('/api/project/' + projectId + '/python/environments?expand=commands');
              },
              getEnvironmentCommands: function (projectId, version) {
                return $http.get('/api/project/' + projectId + '/python/environments/' + version + '/commands');
              },
              exportEnvironment: function (projectId, pythonVersion) {
                return $http.post('/api/project/' + projectId + '/python/environments/' + pythonVersion + '?action=export');
              },
              install: function (projectId, pythonVersion, data) {
                if(data.installType.toUpperCase() === 'PIP') {
                  var regReq = {
                    method: 'POST',
                    url: '/api/project/' + projectId + '/python/environments/' + pythonVersion + '/libraries/' + data.lib +
                    '?package_manager=' + data.installType + '&version=' + data.version + '&machine=' + data.machineType
                  };
                  return $http(regReq);
                } else {
                  var regReq = {
                    method: 'POST',
                    url: '/api/project/' + projectId + '/python/environments/' + pythonVersion + '/libraries/' + data.lib +
                    '?package_manager=' + data.installType + '&version=' + data.version + '&channel=' + data.channelUrl + '&machine=' + data.machineType
                  };
                  return $http(regReq);
                }
              },
              uninstall: function (projectId, version, lib) {
                return $http.delete('/api/project/' + projectId + '/python/environments/' + version + '/libraries/' + lib);
              },
              deleteLibraryCommands: function (projectId, version, library) {
                return $http.delete('/api/project/' + projectId + '/python/environments/' + version + '/libraries/' + library + '/commands');
              },
              retryInstallLibrary: function (projectId, version, library) {
                return $http.put('/api/project/' + projectId + '/python/environments/' + version + '/libraries/' + library);
              },
              search: function (projectId, selected) {
                if(selected.installType === 'PIP') {
                    return $http.get('/api/project/' + projectId + '/python/environments/' + selected.version + '/libraries/pip?query=' + selected.lib);
                } else {
                    return $http.get('/api/project/' + projectId + '/python/environments/' + selected.version + '/libraries/conda?query=' + selected.lib + '&channel=' + selected.channelUrl);
                }
              }
            };
          }]);
