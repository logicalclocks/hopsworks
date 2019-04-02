/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
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
 */
'use strict';

angular.module('hopsWorksApp')
    .controller('OAuthCallbackCtrl', ['$location', '$cookies', '$http', '$rootScope', 'growl', 'AuthService', 'ModalService',
        function ($location, $cookies, $http, $rootScope, growl, AuthService, ModalService) {

            var self = this;
            self.working = false;
            self.user = {code: $location.search()['code'], state: $location.search()['state'], chosenEmail: '', consent: ''};
            var oidp = $cookies.get("openIdProviders");
            self.openIdProviders = oidp? JSON.parse(oidp) : undefined;
            $rootScope.oauthLoginErrorMsg = undefined;
            var login = function (user) {
                AuthService.oauthLogin(user).then(function (success) {
                    self.working = false;
                    AuthService.saveToken(success.headers('Authorization'));
                    $cookies.put("email", success.data.data.value);
                    $location.path('/');
                }, function (error) {
                    if (error !== undefined && error.status === 412) {
                        self.errorMessage = '';
                        ModalService.remoteUserConsent('sm', error.data).then(function (success) {
                            if (success.val.consent) {
                                user.chosenEmail = success.val.chosenEmail;
                                user.consent = success.val.consent;
                                login(user);
                            } else {
                                user = {code: '', state: '', chosenEmail: '', consent: ''};
                                $location.path('/login');
                            }
                            console.log("success", success);
                        }, function (error) {
                            user = {code: '', state: '', chosenEmail: '', consent: ''};
                            $location.path('/login');
                        });
                    } else {
                        $rootScope.oauthLoginErrorMsg = (typeof error.data.usrMsg !== 'undefined')? error.data.usrMsg : "";
                        $location.path('/login');
                    }
                })
            }
            if (self.openIdProviders !== undefined && self.openIdProviders.length > 0 && self.user.code !== undefined &&
                self.user.state !== undefined) {
                login(self.user);
            } else {
                $rootScope.oauthLoginErrorMsg = "No login state found."
                $location.path('/login');
            }

        }]);