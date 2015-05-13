'use strict';

angular.module('hopsWorksApp')
  .controller('RegCtrl', ['AuthService', '$location','$scope', function (AuthService, $location, $scope) {

    var self = this;

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
        self.successMessage=null;
        self.errorMessage=null;
        if ($scope.registerForm.$valid) {
            AuthService.register(self.newUser).then(
                function (success) {
                    self.user = angular.copy(empty);
                    $scope.registerForm.$setPristine();
                    self.successMessage = 'You have successfully created an account, ' +
                    'but you might need to wait until your account is activated ' +
                    'before you can login. ';
                    //$location.path('/login');
                }, function (error) {
                    self.errorMessage = error.data.errorMsg;
                });
        }
    };

  }]);