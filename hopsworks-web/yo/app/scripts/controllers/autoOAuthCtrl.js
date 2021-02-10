/*
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
 */

'use strict';

angular.module('hopsWorksApp')
        .controller('autoOAuthCtrl', ['$location', '$cookies', '$http', 'AuthService', 'VariablesService',
          function ($location, $cookies, $http, AuthService, VariablesService) {
            var self = this;
            
            self.oauthLogin = function (providerName) {
                AuthService.oauthLoginURI(providerName);
                $cookies.put("providerName", providerName);
            };
            
            self.manualLogin = function() {
              $location.url('/login');
            }

            var checkRemoteAuth = function () {
              VariablesService.getAuthStatus().then(function (success) {
                $cookies.put("openIdProviders", JSON.stringify(success.data.openIdProviders));//check undefined
                self.openIdProviders = JSON.parse($cookies.get("openIdProviders"));
                if (self.openIdProviders.length > 0) {
                  self.autoLog = true;
                  self.oauthLogin(self.openIdProviders[0].providerName);
                } else {
                  self.manualLogin()
                }
              }, function (error) {
                self.manualLogin()
              });
            };
            
            
            checkRemoteAuth();
          }]);
