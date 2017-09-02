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
                return $http.get('/api/project/' + projectId + '/pythonDeps')
              },
              enable: function (projectId, version, pythonKernel) {
                return $http.get('/api/project/' + projectId + '/pythonDeps/enable/' + version + "/" + pythonKernel)
              },
              enabled: function (projectId) {
                return $http.get('/api/project/' + projectId + '/pythonDeps/enabled')
              },
              installed: function (projectId) {
                return $http.get('/api/project/' + projectId + '/pythonDeps/installed')
              },
              install: function (projectId, data) {
                var regReq = {
                  method: 'POST',
                  url: '/api/project/' + projectId + '/pythonDeps/install',
                  headers: {'Content-Type': 'application/json'},
                  data: data,
                  dataType: "json"
                }
                return $http(regReq)
              },
              installOneHost: function (projectId, host, data) {
                var regReq = {
                  method: 'POST',
                  url: '/api/project/' + projectId + '/pythonDeps/installOneHost/' + host,
                  headers: {'Content-Type': 'application/json'},
                  data: data,
                  dataType: "json"
                }
                return $http(regReq)
              },
              uninstall: function (projectId, data) {
                var regReq = {
                  method: 'POST',
                  url: '/api/project/' + projectId + '/pythonDeps/remove',
                  headers: {'Content-Type': 'application/json'},
                  data: data,
                  dataType: "json"
                }
                return $http(regReq)
              },
              upgrade: function (projectId, data) {
                var regReq = {
                  method: 'POST',
                  url: '/api/project/' + projectId + '/pythonDeps/upgrade',
                  headers: {'Content-Type': 'application/json'},
                  data: data,
                  dataType: "json"
                }
                return $http(regReq)
              },
              search: function (projectId, data) {
                var regReq = {
                  method: 'POST',
                  url: '/api/project/' + projectId + '/pythonDeps/search',
                  headers: {'Content-Type': 'application/json'},
                  data: data,
                  dataType: "json"
                }
                return $http(regReq)
              },
              status: function (projectId, data) {
                var regReq = {
                  method: 'POST',
                  url: '/api/project/' + projectId + '/pythonDeps/status',
                  headers: {'Content-Type': 'application/json'},
                  data: data,
                  dataType: "json"
                }
                return $http(regReq)
              },
              clone: function (projectId, projName) {
                return $http.get('/api/project/' + projectId + '/pythonDeps/clone/' + projName)
              }
            }
          }]);
