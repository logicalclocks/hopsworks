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
        .factory('ZeppelinService', ['$http', '$websocket', function ($http, $websocket) {
            var websocketCalls = {};
            var setUp = function () {            
              websocketCalls.ws = $websocket(getZeppelinWsBaseURL() + getCookie('projectID'));
              websocketCalls.ws.reconnectIfNotNormalClose = true;

              websocketCalls.sendNewEvent = function (data) {
                data.principal = 'anonymous';
                data.ticket = 'anonymous';
                data.roles = '';
                console.log('Send >> %o, %o, %o, %o, %o', data.op, data.principal, data.ticket, data.roles, data);
                websocketCalls.ws.send(JSON.stringify(data));
              };

              websocketCalls.isConnected = function () {
                return (websocketCalls.ws.socket.readyState === 1);
              };
            };
            var destroy = function () {
              if (websocketCalls.isConnected) {
               websocketCalls.ws.close();
              }
              websocketCalls = {};
            };
            setUp();
            return {
              wsSetup: function () {
                setUp();
              },
              wsDestroy: function () {
                destroy();
              },
              websocket: function () {
                if (angular.equals(websocketCalls, {}) || 
                   !websocketCalls.isConnected) {
                  setUp();
                }
                return websocketCalls;
              },
              moveNoteToTrash: function (noteId) {
                websocketCalls.sendNewEvent({op: 'MOVE_NOTE_TO_TRASH', data: {id: noteId}});
              },
              moveFolderToTrash: function (folderId) {
                websocketCalls.sendNewEvent({op: 'MOVE_FOLDER_TO_TRASH', data: {id: folderId}});
              },
              restoreNote: function (noteId) {
                websocketCalls.sendNewEvent({op: 'RESTORE_NOTE', data: {id: noteId}});
              },
              restoreFolder: function (folderId) {
                websocketCalls.sendNewEvent({op: 'RESTORE_FOLDER', data: {id: folderId}});
              },
              restoreAll: function () {
                websocketCalls.sendNewEvent({op: 'RESTORE_ALL'});
              },
              deleteNote: function (noteId) {
                websocketCalls.sendNewEvent({op: 'DEL_NOTE', data: {id: noteId}});
              },
              removeFolder: function (folderId) {
                websocketCalls.sendNewEvent({op: 'REMOVE_FOLDER', data: {id: folderId}});
              },
              emptyTrash: function () {
                websocketCalls.sendNewEvent({op: 'EMPTY_TRASH'});
              },
              settings: function () {
                return $http.get('/api/zeppelin/'+ getCookie('projectID') + '/interpreter/setting');
              },
              restartInterpreter: function (settingId) {
                return $http.put('/api/zeppelin/'+ getCookie('projectID') + '/interpreter/setting/restart/' + settingId);
              },
              notebooks: function () {
                return $http.get('/api/zeppelin/'+ getCookie('projectID') + '/notebook/');
              },
              reloadedNotebooks: function () {
                return $http.get('/api/zeppelin/'+ getCookie('projectID') + '/notebook/reloadedNotebookList');
              },
              deleteNotebook: function (noteId) {
                return $http.delete('/api/zeppelin/'+ getCookie('projectID') + '/notebook/' + noteId);
              },
              createNotebook: function (note) {
                var regReq = {
                  method: 'POST',
                  url: '/api/zeppelin/'+ getCookie('projectID') + '/notebook/new',
                  headers: {
                    'Content-Type': 'application/json'
                  },
                  data: note
                };
                return $http(regReq);
              },
              interpreters: function () {
                return $http.get('/api/zeppelin/'+ getCookie('projectID') + '/interpreter/interpretersWithStatus');
              },
              restart: function () {
                return $http.get('/api/zeppelin/'+ getCookie('projectID') + '/interpreter/restart');
              },
              stopLivySession: function (settingId, sessionId) {
                return $http.delete('/api/zeppelin/'+ getCookie('projectID') + '/interpreter/livy/sessions/delete/' + settingId + '/' + sessionId);
              },
              getLivySessionAppId: function (sessionId) {
                return $http.get('/api/zeppelin/'+ getCookie('projectID') + '/interpreter/livy/sessions/appId/' + sessionId);
              },
              getSparkAppId: function () {
                return $http.get('/api/zeppelin/'+ getCookie('projectID') + '/interpreter/spark/appId');
              }
            };
          }]);

