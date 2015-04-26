/*jshint undef: false, unused: false, indent: 2*/
/*global angular: false */

'use strict';

angular.module('metaUI')
        .factory('WSComm', function ($websocket, $rootScope, $q, $location) {

            // Keep all pending requests here until they get responses
            var callbacks = [];

            //generic
            var ws = $websocket("ws://" + $location.host() + ":19931/hop-dashboard/wspoint/test?evsav");

            //to work with the ethernet adapter
            //var ws = $websocket("ws://193.10.67.226:19931/MetaHops/metahops/test?evsav");
            //var ws = $websocket("ws://193.10.67.226:19931/hop-dashboard/wspoint/test?evsav");

            //to work with the wireless adapter
            //var ws = $websocket("ws://169.254.67.177:19931/MetaHops/metahops/test?evsav");

            //to work at home
            //var ws = $websocket("ws://192.168.1.42:19931/MetaHops/metahops/test?evsav");
            //var ws = $websocket("ws://192.168.1.42:19931/hop-dashboard/wspoint/test?evsav");

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
                    $rootScope.$apply(callbacks.shift().def.resolve(res));
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
        })
        .service('es', function (esFactory) {
            return esFactory({
                host: 'http://127.0.0.1:9200'
            });
        })
        .run(['$rootScope', 'WSComm', 'BoardDataFactory', 'BoardService', '$q',
            function ($rootScope, WSComm, BoardDataFactory, BoardService, $q) {

                //initialize the main screen
                BoardService.fetchTemplates()
                        .then(function (data) {

                            var data = JSON.parse(data.board);
                            $rootScope.templates = data.templates;

                            if ($rootScope.templates.length === 0)
                                return;

                            var templateid = $rootScope.templates[0].id;
                            $rootScope.templateId = templateid;
                            $rootScope.templateName = $rootScope.templates[0].name;

                            console.log("TEMPLATES RETRIEVED " + JSON.stringify($rootScope.templates));
                            BoardService.fetchTemplate(templateid)
                                    .then(function (response) {

                                        $rootScope.mainBoard = BoardService.mainBoard(JSON.parse(response.board));
                                        $rootScope.tabs = [];

                                        //LOAD STATIC BOARD
                                        //$rootScope.mainBoard = BoardService.mainBoard(BoardDataFactory.kanban);
                                        //console.log($rootScope.mainBoard);
                                    });
                        });
            }]);
