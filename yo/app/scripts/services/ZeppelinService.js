'use strict';

angular.module('hopsWorksApp')
        .factory('ZeppelinService', ['$http', '$websocket', function ($http, $websocket) {
            var websocketCalls = {};
            websocketCalls.ws = $websocket(getZeppelinWsBaseURL());
            websocketCalls.ws.reconnectIfNotNormalClose = true;

            websocketCalls.sendNewEvent = function (data) {
              console.log('Send >> %o, %o', data.op, data);
              websocketCalls.ws.send(JSON.stringify(data));
            };

            websocketCalls.isConnected = function () {
              return (websocketCalls.ws.socket.readyState === 1);
            };

            return {
              websocket: function () {
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
              }
            };
          }]);

