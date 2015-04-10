'use strict';

angular.module('hopsWorksApp')
  .controller('LoginCtrl', ['$location', '$cookieStore', 'AuthService',
    function ($location, $cookieStore, AuthService) {

      var self = this;

      self.user = {email: '', password: ''};

      self.login = function () {
        AuthService.login(self.user).then(
          function (success) {
            $location.path('/');
            $cookieStore.put('email', self.user.email);
        }, function (error) {
            self.errorMessage = error.data.msg;
        });
      };

    }]);
