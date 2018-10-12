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
            var checkeIsAdmin = function () {
              var isAdmin = sessionStorage.getItem("isAdmin");
              if (isAdmin === null) {
                AuthService.isAdmin().then(
                  function (success) {
                    sessionStorage.setItem("isAdmin", success.data === 'true');
                  }, function (error) {
                    sessionStorage.setItem("isAdmin", false);
                });
              }
            };
            var service = {
              guardSession: function($q) {
                AuthService.session().then(
                    function (success) {
                      saveEmail(success.data.data.value);
                      checkeIsAdmin();
                    },
                    function (err) {
                      AuthService.cleanSession();
                      AuthService.removeToken();
                      goToLogin();
                      return $q.reject(err);
                    });
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
                          $cookies.put("otp", success.data.twofactor);
                          $cookies.put("ldap", success.data.ldap);
                        }, function (error) {
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


