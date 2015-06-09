'use strict';

angular.module('hopsWorksApp')

        .factory('ProjectHistoryService', ['$http', function ($http) {
            var service = {
              getByUser: function () {
                return $http.get('/api/history');
              },
              getByProjectId: function (id) {
                return $http.get('/api/history/id');
              }
            };
            return service;
          }]);
