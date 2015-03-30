'use strict';

angular.module('hopsWorksApp')
  .factory('PingService', ['$http',
    function ($http) {
      return {
        ping: function () {
          return $http.post('/api/auth/ping');
        }
      };
    }]);
