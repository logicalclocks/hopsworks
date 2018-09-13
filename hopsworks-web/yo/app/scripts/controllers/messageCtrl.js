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
    .controller('MessageCtrl', ['$scope','$cookies','$uibModalInstance','MessageService','RequestService','growl','md5','selected',
        function ($scope, $cookies, $uibModalInstance, MessageService, RequestService, growl, md5, selected) {

            var self = this;
            self.email = $cookies.get('email');
            self.refreshing = false;
            self.loading = false;
            self.loadingMsg = false;
            self.filterText;
            self.selectedMsg = selected;
            self.trash;
            self.newMsg = "";

            var getMessages = function () {//
                self.loading = true;
                self.refreshing = true;
                MessageService.getMessages().then(
                    function (success) {
                        self.messages = success.data;
                        self.loading = false;
                        self.refreshing = false;
                    }, function (error) {
                        self.loading = false;
                        self.refreshing = false;
                    });
            };
            var getTrash = function () {//
                self.loading = true;
                self.refreshing = true;
                MessageService.getTrash().then(
                    function (success) {
                        self.trash = success.data;
                        self.loading = false;
                        self.refreshing = false;
                    }, function (error) {
                        self.loading = false;
                        self.refreshing = false;
                    });
            };
            var select = function (id) {
                MessageService.markAsRead(id);
            };
            var init = function () {
                getMessages();
                getTrash();
            };
            init();
            self.refresh = function () {
                getMessages();
                getTrash();
            };

            self.deleteMessage = function (msg, index) {
                MessageService.deleteMessage(msg.id).then(
                    function (success) {
                        if (index > -1) {
                            self.trash.splice(index, 1);
                        }
                        if (self.selectedMsg.id === msg.id) {
                            self.selectedMsg = undefined;
                        }
                    }, function (error) {

                    });
            };
            self.emptyTrash = function () {
                MessageService.emptyTrash().then(
                    function (success) {
                        growl.success(success.data.successMessage, {title: 'Success', ttl: 2000, referenceId: 13});
                        getTrash();
                        self.selectedMsg = undefined;
                    }, function (error) {

                    });
            };
            self.reply = function () {
                if (self.newMsg !== "") {
                    MessageService.reply(self.selectedMsg.id, self.newMsg).then(
                        function (success) {
                           self.selectedMsg = success.data;
                           for (var i = 0; i < self.messages.length; i++) {
                               if (self.messages[i].id === self.selectedMsg.id) {
                                   self.messages[i] = self.selectedMsg;
                               }
                           }
                        self.newMsg = "";
                        }, function (error) {

                        });
                }
            };

            $scope.search = function(msg) {
                if (self.filterText === undefined || self.filterText === "") {
                    return true;
                }
                var name = msg.from.fname + ' ' + msg.from.lname;
                var nameRv = msg.from.lname + ' ' + msg.from.fname;
                if (msg.subject.indexOf(self.filterText) !== -1 ||
                    msg.content.indexOf(self.filterText) !== -1 ||
                    name.toLowerCase().indexOf(self.filterText.toLowerCase()) !== -1 ||
                    nameRv.toLowerCase().indexOf(self.filterText.toLowerCase()) !== -1 ||
                    msg.from.email.toLowerCase().indexOf(self.filterText.toLowerCase()) !== -1 ||
                    msg.path.toLowerCase().indexOf(self.filterText.toLowerCase()) !== -1) {
                    return true;
                }
                return false;
            };
            self.select = function(msg){
                msg.unread = false;
                self.selectedMsg = msg;
                select(msg.id);
            };
            self.moveToTrash = function (msg, index) {
                MessageService.moveToTrash(msg.id).then(
                    function (success) {
                        if (index > -1) {
                            self.messages.splice(index, 1);
                            self.trash.push(msg);
                        }
                    }, function (error) {

                    });
            };
            self.restore = function (msg) {
                MessageService.restoreFromTrash(msg.id).then(
                    function (success) {
                        getMessages();
                        getTrash();
                        msg.deleted = false;
                    }, function (error) {

                    });
            };

            self.emailMd5Hash = function (email) {
                return md5.createHash(email || '');
            };

            self.close = function () {
                $uibModalInstance.dismiss('cancel');
            };
        }]);
