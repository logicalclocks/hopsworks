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

'use strict'

angular.module('hopsWorksApp')
    .controller('SshKeysCtrl', ['UserService', '$location', '$scope', 'growl', '$uibModalInstance',
        function (UserService, $location, $scope, growl, $uibModalInstance) {

            var self = this;
            self.working = false;

            $scope.keys = [];

            self.key = {
                name: '',
                publicKey: ''
            };


            var idx = function (obj) {
                return obj.name;
            };

            $scope.status = {};

            self.addSshKey = function () {
                self.working = true;
                self.key.status = false;
                UserService.addSshKey(self.key).then(
                //UserService.addSshKey(self.key.name, self.key.publicKey).then(
                    function (success) {
                        growl.success("Your ssh key has been successfully added.", {
                            title: 'Success',
                            ttl: 5000,
                            referenceId: 1
                        });
                        $scope.keys.push(self.key);
                        $scope.sshKeysForm.$setPristine();
                        $scope.keys[$scope.keys.length - 1].status = false;
                        //var name = self.key.name;
                        //$scope.status[idx(self.key)] = false;
                        self.working = false;
                    },
                    function (error) {
                        self.errorMsg = error.data.errorMsg;
                        growl.error("Failed to add your ssh key: " + self.errorMsg, {
                            title: 'Error',
                            ttl: 5000,
                            referenceId: 1
                        });
                        $scope.sshKeysForm.$setPristine();
                        self.working = false;
                    });
            };


            self.removeSshKey = function (keyName) {
                self.working = true;
                UserService.removeSshKey(keyName).then(
                    function (success) {
                        self.working = false;
                        self.getSshKeys();
                        growl.success("Your ssh key has been successfully removed.", {
                            title: 'Success',
                            ttl: 5000,
                            referenceId: 1
                        });
                    }, function (error) {
                        self.working = false;
                        self.errorMsg = error.data.errorMsg;
                        growl.error("Failed to remove your ssh key.", {title: 'Error', ttl: 5000, referenceId: 1});
                    });
            };

            self.getSshKeys = function () {
                UserService.getSshKeys().then(
                    function (success) {
                        $scope.keys = success.data;
                    });
            }, function (error) {
                self.errorMsg = error.data.errorMsg;
                growl.info("No ssh keys registered: " + error.data.errorMsg, {
                    title: 'Info',
                    ttl: 5000,
                    referenceId: 1
                });
            };


            self.reset = function () {
                $scope.sshKeysForm.$setPristine();
            };

            self.close = function () {
                $uibModalInstance.dismiss('cancel');
            };


            self.getSshKeys();

        }
    ]
);
