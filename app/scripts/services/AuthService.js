'use strict';

angular.module('hopsWorksApp')

  .factory('AuthService', ['$http', 'TransformRequest', function ($http, TransformRequest) {
    var service = {

      session: function () {
        return $http.get('/api/auth/session')
          .then(function (response) {
            return response;
          });
      },

      login: function (user) {
        return $http.post('/api/auth/login', TransformRequest.jQueryStyle(user))
          .then(function (response) {
            return response;
          });
      },

      logout: function () {
        return $http.get('/api/auth/logout')
          .then(function (response) {
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
        };


        return $http(regReq).then(function (response) {
          return response;
        });
      }
    };
    return service;
  }]);
