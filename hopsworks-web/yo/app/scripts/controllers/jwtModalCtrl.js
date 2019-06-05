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
    .controller('JWTModalCtrl', ['$uibModalInstance', 'growl', 'title', 'msg', 'token',
        function ($uibModalInstance, growl, title, msg, token) {

            var self = this;
            self.title = title;
            self.msg = msg;
            self.view = !(typeof token === "undefined");
            self.token = token;

            self.ok = function () {
                $uibModalInstance.close(self.token);
            };

            self.copyToClipboard = function () {
                growl.success("Token copied to clipboard.", {title: "Success", ttl: 5000});
            }

            self.cancel = function () {
                $uibModalInstance.dismiss('cancel');
            };

            self.reject = function () {
                $uibModalInstance.dismiss('reject');
            };

        }]);
