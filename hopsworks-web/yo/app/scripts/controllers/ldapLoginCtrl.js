/*
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

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


