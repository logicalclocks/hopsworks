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
