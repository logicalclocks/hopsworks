/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
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
                          self.announcement = success.data.errorMsg;
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
                if (error.data !== undefined && error.data.errorCode === 120002) {
                  self.errorMessage = "";
                  self.emailHash = md5.createHash(self.user.email || '');
                  self.secondFactorRequired = true;
                } else if (error.data !== undefined &&
                        error.data !== null &&
                        error.data.errorMsg !== undefined &&
                        error.data.errorMsg !== null) {
                  self.errorMessage = error.data.errorMsg;
                }
              if (typeof error.data.usrMsg !== 'undefined') {
                  growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 8000});
              } else {
                  growl.error("", {title: error.data.errorMsg, ttl: 8000});
              }
              });
            };

            isAdminPasswordChanged();
            isFirstTime();
            getAnnouncement();

          }]);
