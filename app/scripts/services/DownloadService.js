'use strict';

angular.module('hopsWorksApp')

        .factory('DownloadService', ['$http', function ($http) {
            var service = {
              getFile: function (projectId, path) {
                path = path.replace(/\//g, '+');
                $http.get('/api/project/' + projectId + '/files/download?path=' + path, {responseType: 'arraybuffer'})
                        .success(function (data) {
                          var file = new Blob([data], {type: 'text'});
                          var fileURL = URL.createObjectURL(file);
                          window.open(fileURL);
                        });
              }
            };
            return service;
          }]);
