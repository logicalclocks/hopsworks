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

'use strict'

angular.module('hopsWorksApp')
        .controller('ProfileCtrl', ['UserService', '$location', '$scope', 'md5', 'growl', '$uibModalInstance','$cookies',
          function (UserService, $location, $scope, md5, growl, $uibModalInstance, $cookies) {

            var self = this;
            self.working = false;
            self.credentialWorking = false;
            self.twoFactorWorking = false;
            self.noPassword = false;
            self.otp = $cookies.get('otp');
            self.emailHash = '';
            self.master = {};
            self.masterTwoFactor = {};
            self.user = {
              firstName: '',
              lastName: '',
              email: '',
              telephoneNum: '',
              registeredon: '',
              twoFactor: ''
            };

            self.loginCredes = {
              oldPassword: '',
              newPassword: '',
              confirmedPassword: ''
            };
            
            self.twoFactorAuth = {
              password: '',
              twoFactor: ''
            };

            self.profile = function () {
              UserService.profile().then(
                      function (success) {
                        self.user = success.data;
                        console.log("User: ", self.user);
                        self.emailHash = md5.createHash(self.user.email || '');
                        self.master = angular.copy(self.user);
                        self.twoFactorAuth.twoFactor = self.master.twoFactor;
                      },
                      function (error) {
                        self.errorMsg = error.data.errorMsg;
                      });
            };

            self.profile();

            self.updateProfile = function () {
              self.working = true;
              UserService.UpdateProfile(self.user).then(
                      function (success) {
                        self.working = false;
                        self.user = success.data;
                        self.master = angular.copy(self.user);
                        growl.success("Your profile is now saved.", {title: 'Success', ttl: 5000, referenceId: 1});
                        $scope.profileForm.$setPristine();
                      }, function (error) {
                        self.working = false;
                        self.errorMsg = error.data.errorMsg;
                        growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000, referenceId: 1});
              });
            };

            self.changeLoginCredentials = function () {
              self.credentialWorking = true;
              if ($scope.credentialsForm.$valid) {
                UserService.changeLoginCredentials(self.loginCredes).then(
                        function (success) {
                          self.credentialWorking = false;
                          growl.success("Your password is now updated.", {title: 'Success', ttl: 5000, referenceId: 1});
                        }, function (error) {
                          self.credentialWorking = false;
                          self.errorMsg = error.data.errorMsg;
                          growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000, referenceId: 1});
                });
              }
            };
            
            self.changeTwoFactor = function() {
              if (self.twoFactorAuth.twoFactor !== self.master.twoFactor) {
                self.twoFactorWorking = true;
                UserService.changeTwoFactor(self.twoFactorAuth).then(
                        function (success) {
                          self.twoFactorWorking = false;
                          self.twoFactorAuth.password = '';
                          if (success.data.QRCode !== undefined) {
                            self.close();
                            $location.path("/qrCode/" + success.data.QRCode);
                            $location.replace();
                          } else if (success.data.successMessage !== undefined) { 
                            self.master.twoFactor = false;
                            growl.success(success.data.successMessage, {title: 'Success', ttl: 5000, referenceId: 1});
                          }
                        }, function (error) {
                          self.twoFactorWorking = false;
                          self.errorMsg = error.data.errorMsg;
                          growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000, referenceId: 1});
                });
              }
            };
            
            self.getQR = function() {
                if (self.twoFactorAuth.password === undefined || 
                    self.twoFactorAuth.password === '') {
                    self.noPassword = true;
                    return;
                }
                self.noPassword = false;
                self.twoFactorWorking = true;
                UserService.getQR(self.twoFactorAuth.password).then(
                        function (success) {
                          self.twoFactorWorking = false;
                          self.twoFactorAuth.password = '';
                          if (success.data.QRCode !== undefined) {
                            self.close();
                            $location.path("/qrCode/" + success.data.QRCode);
                            $location.replace();
                          } 
                        }, function (error) {
                          self.twoFactorWorking = false;
                          self.errorMsg = error.data.errorMsg;
                          growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000, referenceId: 1});
                });
            };

            self.reset = function () {
              self.user = angular.copy(self.master);
              $scope.profileForm.$setPristine();
            };

            self.close = function () {
              $uibModalInstance.dismiss('cancel');
            };

          }]);
