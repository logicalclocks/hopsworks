'use strict';

angular.module('hopsWorksApp')
        .controller('MainCtrl', ['$cookies', '$location', 'AuthService', 'md5', 'ModalService',
          function ($cookies, $location, AuthService, md5, ModalService) {

            var self = this;
            self.email = $cookies['email'];
            self.emailHash = md5.createHash(self.email || '');

            self.logout = function () {
              AuthService.logout(self.user).then(
                      function (success) {
                        $location.url('/login');
                        delete $cookies.email;
                        localStorage.removeItem("SESSIONID");
                        sessionStorage.removeItem("SESSIONID");
                      }, function (error) {
                self.errorMessage = error.data.msg;
              });
            };

            self.profileModal = function () {
              ModalService.profile('md');
            };

          }]);
