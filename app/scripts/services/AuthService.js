'use strict';

angular.module('hopsWorksApp')

  .factory('AuthService', ['$http', 'TransformRequest', function ($http, TransformRequest) {
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
        return $http.post('/api/auth/login', TransformRequest.jQueryStyle(user))
          .then(function (response) {
            service.isLoggedIn = true;
            return response;
          });
      },
      logout: function () {
        return $http.get('/api/auth/logout')
          .then(function (response) {
            service.isLoggedIn = false;
            return response;
          });
      },
      register: function (user) {

        var regReq = {
          method: 'POST',
          url: '/api/auth/register',
          headers: {
            'Content-Type': 'application/json'
          },
          data: user
        }


        return $http(regReq).then(function (response) {
            service.isLoggedIn = false;
            return response;
          });
      }
    };
    return service;
  }]);
