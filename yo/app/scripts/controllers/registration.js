'use strict';

angular.module('hopsWorksApp')
        .controller('RegCtrl', ['AuthService', '$location', '$scope', 'SecurityQuestions', '$routeParams', '$cookies',
          function (AuthService, $location, $scope, SecurityQuestions, $routeParams, $cookies) {
          
            var self = this;
            self.securityQuestions = SecurityQuestions.getQuestions();
            self.working = false;
            self.otp = $cookies['otp'];
            self.newUser = {
              firstName: '',
              lastName: '',
              email: '',
              telephoneNum: '',
              chosenPassword: '',
              repeatedPassword: '',
              securityQuestion: '',
              securityAnswer: '',
              ToS: '',
              authType: '',
              orgName: '',
              dep: '',
              street: '',
              city: '',
              postCode: '',
              country: ''
            };
            
            self.userEmail ='';
            
            self.QR = $routeParams.QR;
            var empty = angular.copy(self.user);
            self.register = function () {
              self.successMessage = null;
              self.errorMessage = null;
              if ($scope.registerForm.$valid) {
                self.working = true;
                console.log(self.otp);
                if (self.otp === 'false') {
                  AuthService.register(self.newUser).then(
                          function (success) {
                            self.user = angular.copy(empty);
                            $scope.registerForm.$setPristine();
                            
                            self.successMessage = success.data.successMessage;
                            
                            self.working = false;
                            
                            //$location.path('/login');
                          }, function (error) {
                             self.working = false;
                             self.errorMessage = error.data.errorMsg;
                  });
                }else  if (self.newUser.authType === 'Mobile') {
                  AuthService.register(self.newUser).then(
                          function (success) {
                            self.user = angular.copy(empty);
                         
                            $scope.registerForm.$setPristine();
                            self.successMessage = success.data.successMessage;
                            self.working = false;
                            $location.path("/qrCode/" + success.data.QRCode);
                            $location.replace();
                            //$location.path('/login');
                          }, function (error) {
                    self.working = false;
                    self.errorMessage = error.data.errorMsg;
                  });
                }else if (self.newUser.authType === 'Yubikey') {
                  AuthService.registerYubikey(self.newUser).then(
                          function (success) {
                            self.user = angular.copy(empty);
                            $scope.registerForm.$setPristine();
                            self.successMessage = success.data.successMessage;
                            self.working = false;
                            self.userEmail= success.data.userEmail;
                            $location.path("/yubikey");
                            $location.replace();
                            //$location.path('/login');
                          }, function (error) {
                    self.working = false;
                    self.errorMessage = error.data.errorMsg;
                  });
                }
                ;
              }
              ;

            };
            self.countries = getAllCountries();
          }]);
