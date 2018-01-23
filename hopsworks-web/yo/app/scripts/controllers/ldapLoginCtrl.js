'use strict';

angular.module('hopsWorksApp')
  .controller('LdapLoginCtrl', ['$location', '$cookies', 'AuthService', 'BannerService', 'ModalService',
    function ($location, $cookies, AuthService, BannerService, ModalService) {

      var self = this;

      self.announcement = "";
      self.ldapEnabled = $cookies.get('ldap') === 'true';
      
      var getAnnouncement = function () {
        BannerService.findBanner().then(
          function (success) {
            if (success.data.status === 1) {
              self.announcement = success.data.message;
            }
          }, function (error) {
            self.announcement = '';
        });
      };
      
      self.working = false;
      self.user = {username: '', password: '', chosenEmail: '', consent: ''};
      getAnnouncement();
      
      self.login = function () {
        self.working = true;
        AuthService.ldapLogin(self.user).then(function (success) {
            self.working = false;
            $cookies.put("email", success.data);
            $location.path('/');
          }, function (error) {
            self.working = false;
            console.log("Login error: ", error);
            if (error !== undefined && error.status === 412) {
              self.errorMessage = "";
              ModalService.ldapUserConsent('sm', error.data).then(function (success) {
                if (success.val.consent) {
                  self.user.chosenEmail = success.val.chosenEmail;
                  self.user.consent = success.val.consent;
                  self.login();
                } else {
                  self.user = {username: '', password: '', chosenEmail: '', consent: ''};
                }
              }, function (error) {
                self.user = {username: '', password: '', chosenEmail: '', consent: ''};
              });
            } else if (error.data !== undefined && error.data !== null && 
                       error.data.errorMsg !== undefined && error.data.errorMsg !== null) {
              self.errorMessage = error.data.errorMsg;
            }
        });
      };

    }]);


