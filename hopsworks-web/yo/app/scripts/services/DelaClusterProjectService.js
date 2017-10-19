'use strict';

angular.module('hopsWorksApp')

        .factory('DelaClusterProjectService', ['$http', function ($http) {
            return function (id) {
              var service = {
                shareWithClusterByInodeId: function (inodeId) {
                  var payload = {"id": inodeId};
                  return $http({
                    method: 'POST',
                    url: '/api/project/' + id + '/delacluster',
                    headers: {
                      'Content-Type': 'application/json'
                    },
                    data: payload
                  });
                },
                unshareFromCluster: function (inodeId) {
                  return $http({
                    method: 'DELETE',
                    url: '/api/project/' + id + '/delacluster/' + inodeId
                  });
                }
              };
              return service;
            };
          }]);
