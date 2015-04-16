'use strict';

angular.module('hopsWorksApp')
  .controller('MainCtrl',['$cookies', '$location', 'AuthService', 'md5',
    function ($cookies, $location, AuthService, md5) {

      var self = this;
      self.email = $cookies['email'];
      self.emailHash = md5.createHash(self.email || '');

      self.logout = function () {
        AuthService.logout(self.user).then(
          function (success) {
            $location.url('/login');
            delete $cookies.email;
          }, function (error) {
            self.errorMessage = error.data.msg;
          });
      };


    }]);
