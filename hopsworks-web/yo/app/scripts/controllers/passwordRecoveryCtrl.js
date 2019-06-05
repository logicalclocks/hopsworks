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
    .controller('PasswordRecoveryCtrl', ['$location', 'AuthService', function ($location, AuthService) {
            var self = this;
            self.loading = false;
            self.user = {key: decodeURIComponent($location.search()['key']), newPassword:'', confirmPassword: ''};
            self.errorMessage = '';
            self.successMessage = '';
            self.send = function () {
                self.loading = true;
                self.errorMessage = '';
                self.successMessage = '';
                AuthService.passwordRecovery(self.user).then( function (success) {
                        self.loading = false;
                        self.successMessage = success.data.successMessage;
                    }, function (error) {
                        self.loading = false;
                        self.errorMessage = typeof error.data.usrMsg !== 'undefined'? error.data.usrMsg : error.data.errorMsg;
                    });
            };

        }]);