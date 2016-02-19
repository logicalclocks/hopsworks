'use strict';

angular.module('hopsWorksApp')
        .controller('LoginCtrl', ['$location', '$cookies', 'AuthService', 'BannerService',
          function ($location, $cookies, AuthService, BannerService) {

            var self = this;

            self.announcement = "";

            self.showAnnouncement = function () {
                if (self.announcement === ""){
                    return false;
                } else {
                    return true;
                }
            };

            var getAnnouncement = function () {
                BannerService.findBanner().then(
                    function (success) {
                        console.log(success);
                        if (success.data.status === 1){
                            self.otp = success.data.otp;
                            self.announcement = success.data.message;
                        }
                    }, function (error) {
                        self.announcement = '';
                    });
            };


            self.working = false;
            self.otp = $cookies['otp'];
            self.user = {email: '', password: '', otp:''};
            getAnnouncement();

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
