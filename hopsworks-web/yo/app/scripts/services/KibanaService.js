'use strict';
/*
 * Service allowing fetching job history objects by type.
 */
angular.module('hopsWorksApp')

        .factory('KibanaService', ['$http', function ($http) {
            var service = {
              
              /**
               * Create a new index for a project in Kibana. 
               * @param {type} projectName
               * @returns {undefined}.
               */
              createIndex: function (projectName) {
                var req = {
                  method: 'POST',
                  url: '/kibana/elasticsearch/.kibana/index-pattern/'+projectName+'?op_type=create',
                  headers: {
                    'Content-Type': 'application/json;charset=utf-8',
                    'kbn-version':'4.6.4'
                  },
                  data: '{"title":"'+projectName+'"}'
                };
                return $http(req);
              }
              
            };
            return service;
          }]);
