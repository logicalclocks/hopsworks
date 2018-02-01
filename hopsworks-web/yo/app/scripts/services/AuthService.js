/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
 */

/*jshint undef: false, unused: false, indent: 2*/
/*global angular: false */

'use strict';

angular.module('hopsWorksApp')
        .factory('AuthService', ['$http', 'TransformRequest', function ($http, TransformRequest) {
            var service = {
                
              isAdmin: function () {
                return $http.get('/api/auth/isAdmin');
              },
              session: function () {
                return $http.get('/api/auth/session');
              },
              login: function (user) {
                return $http.post('/api/auth/login', TransformRequest.jQueryStyle(user));
              },
              ldapLogin: function (user) {
                return $http.post('/api/auth/ldapLogin', TransformRequest.jQueryStyle(user));
              },
              validatePassword: function (user) {
                return $http.post('/api/auth/validatePassword', TransformRequest.jQueryStyle(user));
              },
              logout: function () {
                return $http.get('/api/auth/logout')
                        .then(function (response) {
                          return response;
                        });
              },
              recover: function (user) {
                return $http.post('/api/auth/recoverPassword', TransformRequest.jQueryStyle(user));
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
              }
            };
            return service;
          }]);
