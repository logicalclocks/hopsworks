/*
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
                return $http.get('/api/project/' + projectId + '/python/environments');
              },
              getLibraries: function (projectId, pythonVersion) {
                return $http.get('/api/project/' + projectId + '/python/environments/' + pythonVersion + '/libraries?expand=commands');
              },
              getLibrary: function (projectId, pythonVersion, lib) {
                return $http.get('/api/project/' + projectId + '/python/environments/' + pythonVersion + '/libraries/' + lib);
              },
              createEnvironmentFromVersion: function (projectId, version) {
                return $http.post('/api/project/' + projectId + '/python/environments/' + version + '?action=create');
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
              deleteEnvironmentCommands: function (projectId, pythonVersion) {
                return $http.delete('/api/project/' + projectId + '/python/environments/' + pythonVersion + '/commands');
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
                return $http.put('/api/project/' + projectId + '/python/environments/' + version + '/libraries/' + library + '/commands');
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
