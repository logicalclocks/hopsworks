/*jshint undef: false, unused: false, indent: 2*/
/*global angular: false */

'use strict';

angular.module('hopsWorksApp')
        .factory('AuthService', ['$http', 'TransformRequest', function ($http, TransformRequest) {
            var service = {
              session: function () {
                return $http.get('/api/auth/session');
              },
              login: function (user) {
                return $http.post('/api/auth/login', TransformRequest.jQueryStyle(user));
              },
              logout: function () {
                return $http.get('/api/auth/logout')
                        .then(function (response) {
                          return response;
                        });
              },
              recover: function (user) {
                return $http.post('/api/auth/forgotPassword', TransformRequest.jQueryStyle(user));
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


                return $http(regReq);
              },
              registerYubikey: function (user) {

                var regReq = {
                  method: 'POST',
                  url: '/api/auth/registerYubikey',
                  headers: {
                    'Content-Type': 'application/json'
                  },
                  data: user
                };


                return $http(regReq);
              }
            };
            return service;
          }]);
