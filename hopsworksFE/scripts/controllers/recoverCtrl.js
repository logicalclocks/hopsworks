'use strict';

angular.module('hopsWorksApp')
    .controller('RecoverCtrl', ['$location', 'AuthService','$scope',
        function ($location, AuthService, $scope) {

            var self = this;
            self.user = {email: '',
                         securityQuestion: '',
                         securityAnswer: '',
                         newPassword:'1234',
                         confirmedPassword:'1234'};
            var empty = angular.copy(self.user);
            self.send = function () {
                self.successMessage=null;
                self.errorMessage=null;
                if ($scope.recoveryForm.$valid) {
                    AuthService.recover(self.user).then(
                        function (success) {
                            self.user = angular.copy(empty);
                            $scope.recoveryForm.$setPristine();
                            self.successMessage = 'Your password have been reset successfully.';
                            //$location.path('/login');
                        }, function (error) {
                            self.errorMessage = error.data.errorMsg;
                            console.log(self.errorMessage);
                        });
                }
            };
        }]);
