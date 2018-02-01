/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
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


