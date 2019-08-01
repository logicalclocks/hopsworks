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
    .controller('ApiKeyCtrl', ['$uibModalInstance', 'growl', 'key',
        function ($uibModalInstance, growl, key) {
            var self = this;
            self.key = key;
            self.showKey = typeof self.key !== 'undefined' && typeof self.key.key !== 'undefined';
            self.copied = false;
            self.showWarning = false;

            self.save = function () {
                $uibModalInstance.close(self.key);
            };

            self.onCopy = function () {
                self.copied = true;
                self.showWarning = false;
                growl.success("Copied to clipboard.", {title: 'Success', ttl: 5000, referenceId: 1});
            };

            self.close = function () {
                if (!self.copied) {
                    self.showWarning = true;
                    self.copied = true;
                    return;
                }
                $uibModalInstance.dismiss('cancel');
            };

        }]);