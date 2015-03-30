'use strict';

angular.module('hopsWorksApp')

  .factory('AuthService', ['$http', function ($http) {
    var service = {
      isLoggedIn: false,

      session: function () {
        return $http.get('/api/auth/session')
          .then(function (response) {
            service.isLoggedIn = true;
            return response;
          });
      },

      login: function (user) {
        return $http.post('/api/auth/login', user)
          .then(function (response) {
            service.isLoggedIn = true;
            return response;
          });
      },
      logout: function () {
        return $http.Get('/api/auth/logout')
          .then(function (response) {
            service.isLoggedIn = false;
            return response;
          });
      }
    };
    return service;
  }]);
