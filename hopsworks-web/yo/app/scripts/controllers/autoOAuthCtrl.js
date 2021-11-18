/*
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
 */

'use strict';

angular.module('hopsWorksApp')
        .controller('autoOAuthCtrl', ['$location', '$http', 'AuthService', 'VariablesService', 'StorageService',
          function ($location, $http, AuthService, VariablesService, StorageService) {
            var self = this;
            
            self.oauthLogin = function (providerName) {
              AuthService.oauthLoginURI(providerName);
              StorageService.store("providerName", providerName);
            };
            
            self.manualLogin = function() {
              $location.url('/login');
            }

            var checkRemoteAuth = function () {
              VariablesService.getAuthStatus().then(function (success) {
                StorageService.store("openIdProviders", success.data.openIdProviders);
                self.openIdProviders = StorageService.get("openIdProviders");
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
