'use strict';

angular.module('hopsWorksApp')
        .controller('RegCtrl', ['AuthService', '$location', '$scope', 'SecurityQuestions', '$routeParams', function (AuthService, $location, $scope, SecurityQuestions, $routeParams) {

            var self = this;
            self.securityQuestions = SecurityQuestions.getQuestions();
            self.working = false;
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
              orgName : '',
              dep: '',
              street:'',
              city:'',
              postCode:'',
              country:''
            };
            self.QR = $routeParams.QR;
            var empty = angular.copy(self.user);
            self.register = function () {
              self.successMessage = null;
              self.errorMessage = null;
              if ($scope.registerForm.$valid) {
                self.working = true;
                if (self.newUser.authType === 'Mobile') {
                      AuthService.register(self.newUser).then(
                        function (success) {
                          self.user = angular.copy(empty);
                          $scope.registerForm.$setPristine();
                          self.successMessage = success.data.successMessage;
                          self.working = false;
                          $location.path("/qrCode/"+ success.data.QRCode);
                          $location.replace();
                          
                        //$location.path('/login');
                        }, function (error) {
                          self.working = false;
                          self.errorMessage = error.data.errorMsg;
                });
              };
              if (self.newUser.authType === 'Yubikey') {
                    AuthService.registerYubikey(self.newUser).then(
                        function (success) {
                          self.user = angular.copy(empty);
                          $scope.registerForm.$setPristine();
                          self.successMessage = success.data.successMessage;
                          self.working = false;
                          $location.path("/yubikey");
                          $location.replace();
                        //$location.path('/login');
                        }, function (error) {
                          self.working = false;
                          self.errorMessage = error.data.errorMsg;
                });
              };
              };
                
            };
            self.countries = getAllCountries();
          }]);
