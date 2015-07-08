'use strict';
/*
 * Service responsible for communicating with the Adam backend.
 */
angular.module('hopsWorksApp')

        .factory('AdamService', ['$http', function ($http) {
            var service = {
              /**
               * Request a list of all available commands.
               * @param {int} projectId
               */
              getCommandList: function (projectId) {
                return $http.get('/api/project/' + projectId + '/jobs/adam/commands');
              },
              /**
               * Get the details of the given command, i.e. arguments and options.
               * @param {type} projectId
               * @param {type} commandname
               * @returns {unresolved}
               */
              getCommand: function (projectId, commandname) {
                return $http.get('/api/project/' + projectId + '/jobs/adam/commands/' + commandname);
              },
              runJob: function (projectId, runConfig) {
                var req = {
                  method: 'POST',
                  url: '/api/project/' + projectId + '/jobs/adam/run',
                  headers: {
                    'Content-Type': 'application/json'
                  },
                  data: runConfig
                }
                return $http(req);
              }
            };
            return service;
          }]);


