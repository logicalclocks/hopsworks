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
        .controller('LoginCtrl', ['$location', '$cookies', 'growl', 'TourService', 'AuthService', 'BannerService', 'md5', 
          function ($location, $cookies, growl, TourService, AuthService, BannerService, md5) {

            var self = this;

            self.announcement = "";
            self.secondFactorRequired = false;
            self.firstTime = false;
            self.adminPasswordChanged = true;
            self.tourService = TourService;
            self.working = false;
            self.otp = $cookies.get('otp');
            self.user = {email: '', password: '', otp: ''};
            self.emailHash = md5.createHash(self.user.email || '');
            self.getTours = function () {
              self.tours = [
                {'name': 'Login', 'tip': 'The password for the admin account is: admin'},
              ];
            };
            self.ldapEnabled = $cookies.get('ldap') === 'true';


            self.showDefaultPassword = function() {
              if (self.firstTime === false || self.adminPasswordChanged === true ||
                      self.user.email !== 'admin@kth.se') {
                return false;
              }
              return true;
            };

            var getAnnouncement = function () {
              BannerService.findBanner().then(
                      function (success) {
                        console.log(success);
                        self.otp = success.data.otp;
                        if (success.data.status === 1) {
                          self.announcement = success.data.message;
                        }
                      }, function (error) {
                self.announcement = '';
              });
            };

            var isFirstTime = function () {
              BannerService.isFirstTime().then(
                      function (success) {
                        self.firstTime = true;
                        self.user.email = "admin@kth.se";
//                        self.user.password = "admin";
                        self.user.toursState = 0;
                      }, function (error) {
                self.firstTime = false;
              });
            };
            var isAdminPasswordChanged = function () {
              BannerService.hasAdminPasswordChanged().then(
                      function (success) {
                        self.adminPasswordChanged = true;
                      }, function (error) {
                self.adminPasswordChanged = false;
                self.announcement = "Security risk: change the current default password for the 'admin@kth.se' account."
              });
            };
            self.enterAdminPassword = function () {
              self.user.password = "admin";
              self.tourService.resetTours();
            };

            self.login = function () {
              self.working = true;
              AuthService.login(self.user).then(
                      function (success) {
                        if (self.firstTime == true) {
                          BannerService.firstSuccessfulLogin().then(
                                  function (success) {
                                    self.firstTime = false;
                                  }, function (error) {
                            // todo ?
                          });
                        }
                        self.working = false;
                        self.secondFactorRequired = false;
                        $cookies.put("email", self.user.email);
                        $location.path('/');
                      }, function (error) {
                self.working = false;
                if (error.data !== undefined && error.data.statusCode === 417 &&
                        error.data.errorMsg === "Second factor required.") {
                  self.errorMessage = "";
                  self.emailHash = md5.createHash(self.user.email || '');
                  self.secondFactorRequired = true;
                } else if (error.data !== undefined && error.data.statusCode === 412 &&
                        error.data.errorMsg === "First time login") {
                  self.user.email = "admin@kth.se";
                  self.user.password = "admin";
                  self.login();
                  // self.turnOffFirstTimeLogin()
                  // Set
                } else if (error.data !== undefined &&
                        error.data !== null &&
                        error.data.errorMsg !== undefined &&
                        error.data.errorMsg !== null) {
                  self.errorMessage = error.data.errorMsg;
                }
                growl.error(error.data.errorMsg, {title: 'Cannot Login at this moment. Does your Internet work?', ttl: 4000});
              });
            };

            isAdminPasswordChanged();
            isFirstTime();
            getAnnouncement();

          }]);
