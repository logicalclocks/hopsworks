'use strict';

angular.module('hopsWorksApp')
        .controller('RegCtrl', ['AuthService', '$location', '$scope', 'SecurityQuestions', function (AuthService, $location, $scope, SecurityQuestions) {

            var self = this;
            self.securityQuestions = SecurityQuestions.getQuestions();

            self.newUser = {
              firstName: '',
              lastName: '',
              email: '',
              telephoneNum: '',
              chosenPassword: '',
              repeatedPassword: '',
              securityQuestion: '',
              securityAnswer: '',
              ToS: ''
            };
            var empty = angular.copy(self.user);
            self.register = function () {
              self.successMessage = null;
              self.errorMessage = null;
              if ($scope.registerForm.$valid) {
                AuthService.register(self.newUser).then(
                        function (success) {
                          self.user = angular.copy(empty);
                          $scope.registerForm.$setPristine();
                          self.successMessage = success.data.successMessage;
                          //$location.path('/login');
                        }, function (error) {
                  self.errorMessage = error.data.errorMsg;
                })
              }
            };

          }]);
