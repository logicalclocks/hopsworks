'use strict';

angular.module('hopsWorksApp')

        .factory('DelaProjectService', ['$http', function ($http) {
            return function (id) {
              var service = {
                publishByInodeId: function (inodeId) {
                  var payload = {"id": inodeId};
                  return $http({
                    method: 'POST',
                    url: '/api/project/' + id + '/dela/uploads',
                    headers: {
                      'Content-Type': 'application/json'
                    },
                    data: payload
                  });
                },
                cancel: function (publicDSId, cleanVal) {
                  return $http({
                    method: 'POST',
                    url: '/api/project/' + id + '/dela/transfers/' + publicDSId + '/cancel?clean=cleanVal'
                  });
                },
                downloadMetadata: function (publicDSId, json) {
                  return $http({
                    method: 'POST',
                    url: '/api/project/' + id + '/dela/downloads/' + publicDSId + '/manifest',
                    headers: {
                      'Content-Type': 'application/json'
                    },
                    data: json
                  });
                },
                downloadHdfs: function (publicDSId, json) {
                  return $http({
                    method: 'POST',
                    url: '/api/project/' + id + '/dela/downloads/' + publicDSId + '/hdfs',
                    headers: {
                      'Content-Type': 'application/json'
                    },
                    data: json
                  });
                },
                downloadKafka: function (publicDSId, json) {
                  return $http({
                    method: 'POST',
                    url: '/api/project/' + id + '/dela/downloads/'+ publicDSId + '/kafka',
                    headers: {
                      'Content-Type': 'application/json'
                    },
                    data: json
                  });
                },
                datasetsInfo: function () {
                  return $http({
                    method: 'GET',
                    url: '/api/project/' + id + '/dela/transfers'
                  });
                },
                getDetails: function (publicDSId) {
                  return $http({
                    method: 'GET',
                    url: '/api/project/' + id + '/dela/transfers/' + publicDSId
                  });
                },
                getManifest: function (publicDSId) {
                  return $http({
                    method: 'GET',
                    url: '/api/project/' + id + '/dela/transfers/' + publicDSId + '/manifest'
                  });
                }
              };
              return service;
            };
          }]);
