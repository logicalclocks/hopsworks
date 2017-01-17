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
                    index: function(projectId){
                      return $http.get('/api/project/'+ projectId + '/pythonDeps')
                    },
                    install: function (projectId, data) {
                        var regReq = {
                            method: 'POST',
                            url: '/api/project/' + projectId + '/pythonDeps',
                            headers: {'Content-Type': 'application/json'},
                            data: data,
                            dataType: "json"
                        }
                        return $http(regReq)
                    },
                    remove: function (projectId, data) {
                        var regReq = {
                            method: 'DELETE',
                            url: '/api/project/' + projectId + '/pythonDeps',
                            headers: {'Content-Type': 'application/json'},
                            data: data,
                            dataType: "json"
                        }
                        return $http(regReq)
                    },
                    clone: function(projectId, projName){
                      return $http.get('/api/project/'+ projectId + '/pythonDeps/clone/' + projName)
                    }
                }
            }]);
