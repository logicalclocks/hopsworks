'use strict';

angular.module('hopsWorksApp')
  .factory('PingService', ['$http',
    function ($http) {
      return {
        ping: function () {
          return $http.get('/api/admin/ping');
        }
      };
    }]);
