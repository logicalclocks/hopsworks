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
    .factory('MessageService',['$http', function ($http) {
        return {
            getMessages: function () {
                return $http.get('/api/message/');
            },
            getTrash: function () {
                return $http.get('/api/message/deleted');
            },
            emptyTrash: function () {
                return $http.delete('/api/message/empty');
            },
            markAsRead: function (msgId) {
                return $http.put('/api/message/markAsRead/'+msgId);
            },
            getUnreadCount: function () {
                return $http.get('/api/message/countUnread');
            },
            reply: function (msgId, msg) {
                var regReq = {
                    method: 'POST',
                    url: '/api/message/reply/' + msgId,
                    headers: {
                        'Content-Type': 'text/plain'
                    },
                    data: msg
                };
                return $http(regReq);
            },
            moveToTrash: function (msgId) {
                return $http.put('/api/message/moveToTrash/' + msgId);
            },
            restoreFromTrash: function (msgId) {
                return $http.put('/api/message/restoreFromTrash/' + msgId);
            },
            deleteMessage: function (msgId) {
                return $http.delete('/api/message/' + msgId);
            }
        };

    }]);
