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
              authType: '' 
            };
            self.QR = $routeParams.QR;
            var empty = angular.copy(self.user);
            self.register = function () {
              self.successMessage = null;
              self.errorMessage = null;
              if ($scope.registerForm.$valid) {
                self.working = true;
                AuthService.register(self.newUser).then(
                        function (success) {
                          self.user = angular.copy(empty);
                          $scope.registerForm.$setPristine();
                          self.successMessage = success.data.successMessage;
                          self.working = false;
                          if (newUser.authType === 'Mobile') {
                            $location.path('/qrCode/' + success.data);
                          };
                          //$location.path('/login');
                        }, function (error) {
                          self.working = false;
                          self.errorMessage = error.data.errorMsg;
                });
              }
            };

          }]);
