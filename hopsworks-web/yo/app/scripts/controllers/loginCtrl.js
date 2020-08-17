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
    .controller('LoginCtrl', ['$location', '$cookies', '$http', '$rootScope', 'growl', 'TourService', 'AuthService', 'BannerService', 'md5', 'ModalService', 'VariablesService',
        function ($location, $cookies, $http, $rootScope, growl, TourService, AuthService, BannerService, md5, ModalService, VariablesService) {

            var self = this;

            self.announcement = "";
            self.secondFactorRequired = false;
            self.firstTime = false;
            self.adminPasswordChanged = true;
            self.tourService = TourService;
            self.working = false;
            self.user = {email: '', password: '', otp: ''};
            self.emailHash = md5.createHash(self.user.email || '');
            self.otp = $cookies.get('otp');
            self.ldapEnabled = $cookies.get('ldap') === 'true';
            self.krbEnabled = $cookies.get("krb") === 'true';
            self.registerDisabled = $cookies.get("registerDisabled") === 'true';
            self.loginDisabled = $cookies.get("loginDisabled") === 'true';
            self.openIdProviders = [];

            var getErrorMsg = function (error, msg) {
                var errorMsg = "";
                if (error.data !== undefined &&
                    error.data !== null &&
                    error.data.errorMsg !== undefined &&
                    error.data.errorMsg !== null) {
                    errorMsg = error.data.errorMsg;
                }
                return (typeof error.data !== 'undefined' &&
                    error.data !== undefined &&
                    error.data !== null &&
                    typeof error.data.usrMsg !== 'undefined')? error.data.usrMsg : msg + errorMsg;
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
                        self.announcement = "Security risk: change the current default password for the default admin account."
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
                        AuthService.saveToken(success.headers('Authorization'));
                        $cookies.put("email", self.user.email);
                        $location.path('/');
                    }, function (error) {
                        self.working = false;
                        if (error.data !== undefined && error.data.errorCode === 120002) {
                            self.errorMessage = "";
                            self.emailHash = md5.createHash(self.user.email || '');
                            self.secondFactorRequired = true;
                        } else {
                            self.errorMessage = getErrorMsg(error, "");
                        }
                    });
            };

            var krbLogin = function (user) {
                self.working = true;
                self.errorMessage = '';
                AuthService.krbLogin(user).then(function (success) {
                    self.working = false;
                    $cookies.put("email", success.data.data.value);
                    $location.path('/');
                }, function (error) {
                    self.working = false;
                    console.log("Login error: ", error);
                    if (error !== undefined && error.status === 412) {
                        self.errorMessage = '';
                        ModalService.remoteUserConsent('sm', error.data).then(function (success) {
                            if (success.val.consent) {
                                user.chosenEmail = success.val.chosenEmail;
                                user.consent = success.val.consent;
                                krbLogin(user);
                            } else {
                                user = {chosenEmail: '', consent: ''};
                            }
                        }, function (error) {
                            user = {chosenEmail: '', consent: ''};
                        });
                    } else {
                        self.errorMessage = getErrorMsg(error, 'Sorry, could not log you in with Kerberos. ');
                    }
                });
            };

            self.krbLogin = function () {
                var user = {chosenEmail: '', consent: ''};
                krbLogin(user);
            };

            self.oauthLogin = function (providerName) {
                AuthService.oauthLoginURI(providerName);
            };

            self.resetOAuthLoginErrorMsg = function () {
                $rootScope.oauthLoginErrorMsg = undefined;
            }

            var checkRemoteAuth = function () {
                VariablesService.getAuthStatus().then( function (success) {
                    $cookies.put("otp", success.data.twofactor);
                    $cookies.put("ldap", success.data.ldap);
                    $cookies.put("krb", success.data.krb);
                    $cookies.put("loginDisabled", success.data.loginDisabled);
                    $cookies.put("registerDisabled", success.data.registerDisabled);
                    $cookies.put("openIdProviders", JSON.stringify(success.data.openIdProviders));//check undefined
                    self.otp = $cookies.get('otp');
                    self.ldapEnabled = $cookies.get('ldap') === 'true';
                    self.krbEnabled = $cookies.get("krb") === 'true';
                    self.registerDisabled = $cookies.get("registerDisabled") === 'true';
                    self.loginDisabled = $cookies.get("loginDisabled") === 'true';
                    self.openIdProviders = JSON.parse($cookies.get("openIdProviders"));
                }, function (error) {
                });
            };
            checkRemoteAuth();

            isAdminPasswordChanged();
            isFirstTime();
            getAnnouncement();

        }]);
