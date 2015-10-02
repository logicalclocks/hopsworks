/*jshint undef: false, unused: false, indent: 2*/
/*global angular: false */

'use strict';

angular.module('hopsWorksApp')
        .factory('WSComm', function ($websocket, $rootScope, $q, $location, $cookies) {

          // Keep all pending requests here until they get responses
          var callbacks = [];
          var projectID = $cookies.projectID;

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
              var response = {status: data.status, board: data.message};
              //console.log('sender: ' + data.sender + ' message: ' + data.message);

              /*
               * Whenever a message arrives resolve the defer, so the client gets back their promise.
               * If there is no defer to resolve, it means the message arrived as a consequence of the action of another user
               * connected to the same project (template changing actions) so just emit the changed template
               */
              var defer = callbacks.shift();
              if(!angular.isUndefined(defer)){
                $rootScope.$apply(defer.def.resolve(response));
              }
              else{
                $rootScope.$emit("template.change", response);
              }
            } catch (e) {
              var res = {sender: 'anonymous', message: e};

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
              /*
               * whenever users send a message they get back a promise object, 
               * that means they are able to know when they get back their response
               */
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
