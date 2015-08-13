/*jshint undef: false, unused: false, indent: 2*/
/*global angular: false */

'use strict';

angular.module('hopsWorksApp')
        .factory('WSComm', function ($websocket, $rootScope, $q, $location) {

          // Keep all pending requests here until they get responses
          var callbacks = [];
          var projectID = 10241;

          //generic
          var ws = $websocket("ws://" + $location.host() + ":" + $location.port() + "/hopsworks/wspoint/" + projectID);

          var collection = [];

          ws.onMessage(function (event) {
            processMessage(JSON.parse(event.data));
          });

          ws.onError(function (event) {
            console.log('connection Error', event);
          });

          ws.onClose(function (event) {
            console.log('connection closed', event);
          });

          ws.onOpen(function () {
            console.log('connection open');
          });
          // setTimeout(function() {
          //   ws.close();
          // }, 500)

          var processMessage = function (data) {

            try {
              var board = data.message;
              var status = data.status;
              var response = {status: data.status, board: data.message};
              //console.log('sender: ' + data.sender + ' message: ' + data.message);

              //since the data arrived its time to resolve the defer
              $rootScope.$apply(callbacks.shift().def.resolve(response));
            } catch (e) {
              var res = {sender: 'anonymous', message: e};

              //$rootScope.$apply(callbacks.shift().def.resolve(res));
              console.log('ErrorLocal:');
              console.log(res);
            }
          };

          return {
            ws: ws,
            status: function () {
              return ws.readyState;
            },
            send: function (message) {
              var defer = $q.defer();
              callbacks.push({
                def: defer,
                timeStamp: new Date()
              });
              if (angular.isString(message)) {
                ws.send(message);
              }
              else if (angular.isObject(message)) {
                ws.send(JSON.stringify(message));
              }
              return defer.promise;
            }
          };
        });
