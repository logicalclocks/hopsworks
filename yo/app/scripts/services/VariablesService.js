'use strict';

angular.module('hopsWorksApp')
        .factory('VariablesService', ['$http', function ($http) {
            var service = {

              getAllVariables: function () {
                return $http.get('/api/variables/all');
              },
              getTwofactor: function () {
                return $http.get('/api/variables/twofactor');
              },
              getVariable: function (id) {
                return $http.get('/api/variables/' + id);
              }
            };
            return service;
          }]);
