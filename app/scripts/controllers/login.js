'use strict';

angular.module('hopsWorksApp')
  .controller('LoginCtrl', ['$location', 'AuthService',
    function ($location, AuthService) {

      var self = this;
      self.user = {email: '', password: ''};

      self.login = function () {
        AuthService.login(self.user).then(
          function (success) {
            $location.path('/');
        }, function (error) {
            self.errorMessage = error.data.msg;
        });
      };

    }]);
