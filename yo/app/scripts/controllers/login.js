'use strict';

angular.module('hopsWorksApp')
        .controller('LoginCtrl', ['$location', '$cookies', 'AuthService',
          function ($location, $cookies, AuthService) {

            var self = this;
            self.working = false;
            self.otp = $cookies['otp'];
            self.user = {email: '', password: '', otp:''};

            self.login = function () {
              self.working = true;
              AuthService.login(self.user).then(
                      function (success) {
                        self.working = false;
                        $cookies.email = self.user.email;
                        $location.path('/');
			
                      }, function (error) {
                        self.working = false;
                        self.errorMessage = error.data.errorMsg;
                        console.log(self.errorMessage);
              });
            };

          }]);
