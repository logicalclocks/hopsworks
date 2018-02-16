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

/*jshint undef: false, unused: false, indent: 2*/
/*global angular: false */

'use strict';

angular.module('hopsWorksApp')
        .factory('WSComm', function ($websocket, $rootScope, $q, $location, $cookies) {

          // Keep all pending requests here until they get responses
          var callbacks = [];
          var projectID = $cookies.get('projectID');
          var isopen=false;
          //generic
          var ws;

          function init(){ 
             if(projectID !== $cookies.get("projectID")){
                 projectID = $cookies.get("projectID");
                 isopen=false;
             }
             if(!isopen) {                 
                ws = $websocket(getMetaDataWsBaseURL() + projectID);
                ws.onMessage(function (event) {
                  processMessage(JSON.parse(event.data));
                });

                ws.onError(function (event) {
                  console.log('connection Error', event);
                  isopen=false;
                });

                ws.onClose(function (event) {
                  console.log('connection closed', event);
                  isopen=false;
                });

                ws.onOpen(function () {
                  console.log('connection open');
                  isopen=true;
                }); 
            }
          };
          

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
              init();
              return ws.readyState;
            },
            send: function (message) {
             
              /*
               * whenever users send a message they get back a promise object, 
               * that means they are able to know when they get back their response
               */
              init();
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
