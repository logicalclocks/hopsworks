'use strict';

angular.module('hopsWorksApp')
        .factory('ZeppelinService', ['$http', '$websocket', function ($http, $websocket) {
            var websocketCalls = {};
            var setUp = function () {            
              websocketCalls.ws = $websocket(getZeppelinWsBaseURL());
              websocketCalls.ws.reconnectIfNotNormalClose = true;

              websocketCalls.sendNewEvent = function (data) {
                console.log('Send >> %o, %o', data.op, data);
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
              settings: function () {
                return $http.get('/api/interpreter/setting');
              },
              restartInterpreter: function (settingId) {
                return $http.put('/api/interpreter/setting/restart/' + settingId);
              },
              notebooks: function () {
                return $http.get('/api/notebook/');
              },
              createNotebook: function (noteName) {
                var regReq = {
                  method: 'POST',
                  url: '/api/notebook/new',
                  headers: {
                    'Content-Type': 'application/json'
                  },
                  data: noteName
                };
                return $http(regReq);
              },
              interpreters: function () {
                return $http.get('/api/interpreter/interpretersWithStatus');
              },
              restart: function () {
                return $http.get('/api/interpreter/restart');
              },
              stopLivySession: function (settingId, sessionId) {
                return $http.delete('/api/interpreter/livy/sessions/delete/' + settingId + '/' + sessionId);
              }
            };
          }]);

