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
