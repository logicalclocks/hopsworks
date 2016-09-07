'use strict'

angular.module('hopsWorksApp')
        .controller('ProfileCtrl', ['UserService', '$location', '$scope', 'md5', 'growl', '$modalInstance','$cookies',
          function (UserService, $location, $scope, md5, growl, $modalInstance, $cookies) {

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
              $modalInstance.dismiss('cancel');
            };

          }]);
