/*
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 */
'use strict';

angular.module('hopsWorksApp')
        .factory('AuthGuardService', ['$cookies', '$location', '$rootScope', 'AuthService', 'HopssiteService', 'VariablesService', 'ProjectService',
          function ($cookies, $location, $rootScope,AuthService, HopssiteService, VariablesService, ProjectService) {
            var saveEmail = function (value) {
              $cookies.put("email", value);
            };
            var goToHome = function () {
              $location.path('/');
              $location.replace();
            };
            var goToLogin = function () {
              $location.path('/login');
              $location.replace();
            };
            var setAuth = function (success) {
              if (typeof success.data !== 'undefined') {
                  $cookies.put("otp", success.data.twofactor);
                  $cookies.put("ldap", success.data.ldap);
                  $cookies.put("krb", success.data.krb);
                  $cookies.put("openIdProviders", JSON.stringify(success.data.openIdProviders));//check undefined
              }
            };
            var checkIsAdmin = function () {
              var isAdmin = sessionStorage.getItem("isAdmin");
              if (isAdmin != 'true' && isAdmin != 'false') {
                AuthService.isAdmin().then(
                  function (success) {
                    sessionStorage.setItem("isAdmin", success.data.data.value);
                  }, function (error) {
                    sessionStorage.setItem("isAdmin", null);
                });
              }
            };
            var service = {
              guardSession: function($q) {
                var deferred = $q.defer();
                AuthService.session().then(
                    function (success) {
                      saveEmail(success.data.data.value);
                      checkIsAdmin();
                      return deferred.resolve(success);
                    },
                    function (err) {
                      AuthService.cleanSession();
                      AuthService.removeToken();
                      goToLogin();
                      return deferred.reject(err);
                    });
                return deferred.promise;
              },
              noGuard: function ($q) {
                AuthService.session().then(
                    function (success) {
                      saveEmail(success.data.data.value);
                      goToHome();
                      return $q.when(success);
                    },
                    function (err) {
                      VariablesService.getAuthStatus().then(
                        function (success) {
                          setAuth(success);
                          return $q.when(err);
                        }, function (error) {
                          return $q.when(err);
                      });
                    });
              },
              guardHopssite: function($q) {
                HopssiteService.getServiceInfo("dela").then(function (success) {
                    if (success.data.status === 1 ) {
                      $rootScope['isDelaEnabled'] = true;
                    } else {
                      $rootScope['isDelaEnabled'] = false;
                      goToHome();
                      return $q.reject();
                    }
                  }, function (error) {
                    $rootScope['isDelaEnabled'] = false;
                    goToHome();
                    return $q.reject(error);
                  });
              },
              guardProject: function($q, projectId) {
                ProjectService.checkProject({id: projectId}).$promise.then(
                  function (success) {
                    $cookies.put("projectID", success.data.value);
                  }, function (error) {
                    $cookies.remove("projectID");
                    goToHome();
                    return $q.reject(error);
                });
              }
            };
            return service;
          }]);


