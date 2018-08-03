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


            self.showDefaultPassword = function () {
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
                      if (error.status === 503) {
                        self.announcement = "Hopsworks unavailable. Is MySQL down?"
                      } else if (error.status === 404) {
                        self.announcement = "Hopsworks unavailable. Is the Hopsworks App installed?"
                      } else {
                        self.adminPasswordChanged = false;
                        self.announcement = "Security risk: change the current default password for the 'admin@kth.se' account."
                      }
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
