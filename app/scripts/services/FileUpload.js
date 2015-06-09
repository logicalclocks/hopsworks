'use strict';

angular.module('hopsWorksApp')

        .factory('FileUpload', ['$http', function ($http) {
            return function (file, projectId, path) {
              var fd = new FormData();
              var uploadUrl = '/api/project/' + projectId + '/dataset' + path;
              fd.append('file', file);
              $http.post(uploadUrl, fd, {
                transformRequest: angular.identity,
                headers: {'Content-Type': 'multipart/form-data'}
              })
            };
          }]);

