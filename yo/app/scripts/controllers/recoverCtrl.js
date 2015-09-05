'use strict';

angular.module('hopsWorksApp')
        .controller('RecoverCtrl', ['$location', 'AuthService', '$scope', 'SecurityQuestions',
          function ($location, AuthService, $scope, SecurityQuestions) {

            var self = this;
            self.working = false;
            self.securityQuestions = SecurityQuestions.getQuestions();
            self.user = {email: '',
              securityQuestion: '',
              securityAnswer: '',
              newPassword: '1234',
              confirmedPassword: '1234'};
            var empty = angular.copy(self.user);
            self.send = function () {
              self.successMessage = null;
              self.errorMessage = null;
              if ($scope.recoveryForm.$valid) {
                self.working = true;
                AuthService.recover(self.user).then(
                        function (success) {
                          self.user = angular.copy(empty);
                          $scope.recoveryForm.$setPristine();
                          self.working = false;
                          self.successMessage = success.data.successMessage;
                          //$location.path('/login');
                        }, function (error) {
                          self.working = false;
                          self.errorMessage = error.data.errorMsg;
                          console.log(self.errorMessage);
                });
              }
            };


          }]);
