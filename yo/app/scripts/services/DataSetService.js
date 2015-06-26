'use strict';

angular.module('hopsWorksApp')
        .factory('DataSetService', ['$http', function ($http) {
            return function (id) {
              var services = {
                getAll: function () {
                  return $http.get('/api/project/' + id + '/dataset/');
                },
                getDir: function (dirName) {
                  return $http.get('/api/project/' + id + '/dataset/' + dirName);
                },
                download: function (fileName) {
                  return $http.get('/api/project/' + id + '/dataset/download/' + fileName, {responseType: 'arraybuffer'});
                },
                createDataSetDir: function (dataSet) {
                  var regReq = {
                    method: 'POST',
                    url: '/api/project/' + id + '/dataset',
                    headers: {
                      'Content-Type': 'application/json'
                    },
                    data: dataSet
                  };


                  return $http(regReq);
                },
                removeDataSetDir: function (fileName) {
                  return $http.delete('/api/project/' + id + '/dataset/' + fileName);
                }
              }
              return services;
            };
          }]);
