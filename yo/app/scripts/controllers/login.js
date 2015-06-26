'use strict';

angular.module('hopsWorksApp')
        .controller('LoginCtrl', ['$location', '$cookies', 'AuthService',
          function ($location, $cookies, AuthService) {

            var self = this;

            self.user = {email: '', password: ''};

            self.login = function () {
              AuthService.login(self.user).then(
                      function (success) {
                        $cookies.email = self.user.email;
                        $location.path('/');
                      }, function (error) {
                self.errorMessage = error.data.errorMsg;
                console.log(self.errorMessage);
              });
            };

          }]);
