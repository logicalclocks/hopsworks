'use strict';

angular.module('hopsWorksApp')
        .controller('ZeppelinCtrl', ['$scope', '$routeParams','$route',
          'growl', 'ModalService', 'ZeppelinService','$location',
          function ($scope, $routeParams, $route, growl, ModalService, ZeppelinService, $location) {

            var self = this;
            self.interpretersRefreshing = false;
            self.notesRefreshing = false;
            self.interpreters = [];
            self.tutorialNotes = [];
            self.notes = [];
            self.transition = false;
            self.collapse = false;
            self.loading = false;
            self.loadingText = "";
            $scope.tgState = true;
            self.selectedInterpreter;
            var projectId = $routeParams.projectID;
            var statusMsgs = ['stopped    ', "running    ", 'stopping...', 'restarting...'];
            var loaded = false;

            self.deselect = function () {
              self.selected = null;
              refresh();
              getNotesInProject(null, false);
            };
            
            var startLoading = function (label) {
              self.loading = true;
              self.loadingText = label;
            };
            
            var stopLoading = function () {
              self.loading = false;
              self.loadingText = "";
            };

            var getInterpreterStatus = function (loading) {
              self.interpreters = [];
              if (loading) {
                startLoading(loading);
              }
              var interpreter;
              ZeppelinService.interpreters().then(function (success) {
                console.log('Receive interpreters<< %o', success);
                for (var k in success.data.body) {
                  interpreter = {interpreter: success.data.body[k], 
                    statusMsg: statusMsgs[(success.data.body[k].notRunning ? 0 : 1)]};
                  self.interpreters.push(interpreter);
                }
                stopLoading();
              }, function (error) {
                growl.warning(error.data.errorMsg + " Try reloading the page.", 
                {title: 'Error', ttl: 5000, referenceId: 10});
                stopLoading();
              });
            };

            var getNotesInProject = function (loading, reload) {
              self.notesRefreshing = true;
              if (loading) {
                 startLoading(loading);
              }
              if (reload) {
                ZeppelinService.reloadedNotebooks().then(function (success) {
                  self.notes = success.data.body;
                  self.notesRefreshing = false;
                  stopLoading();
                }, function (error) {
                  self.notesRefreshing = false;
                  growl.warning(error.data.errorMsg + " Try reloading the page.",
                          {title: 'Error', ttl: 5000, referenceId: 10});
                  stopLoading();
                });
              } else {
                ZeppelinService.notebooks().then(function (success) {
                  self.notes = success.data.body;
                  self.notesRefreshing = false;
                  stopLoading();
                }, function (error) {
                  self.notesRefreshing = false;
                  growl.warning(error.data.errorMsg + " Try reloading the page.",
                          {title: 'Error', ttl: 5000, referenceId: 10});
                  stopLoading();
                });
              }
            };

            self.display = function (group) {
              if (group.indexOf("spark") > -1)
                return true;
              if (group.indexOf("flink") > -1)
                return true;
              if (group.indexOf("python") > -1)
                return true;
              if (group.indexOf("angular") > -1)
                return true;
              if (group.indexOf("livy") > -1) 
                return true;
              if (group.indexOf("md") > -1) 
                return true;
              return false;
            };

            var refresh = function () {
              self.interpretersRefreshing = true;
              ZeppelinService.interpreters().then(function (success) {
                for (var i in self.interpreters) {
                  var interpreter = success.data.body;
                  if (self.interpreters[i].statusMsg === statusMsgs[0] ||
                          self.interpreters[i].statusMsg === statusMsgs[1]) {
                    self.interpreters[i].interpreter = interpreter[self.interpreters[i].interpreter.name];
                    self.interpreters[i].statusMsg = statusMsgs[(interpreter[self.interpreters[i].interpreter.name].notRunning ? 0 : 1)];
                  }
                }
                self.interpretersRefreshing = false;
              }, function (error) {
                self.interpretersRefreshing = false;
              });
            };

            var init = function () {
              startLoading("Connecting to zeppelin...");
              ZeppelinService.websocket().sendNewEvent({op: 'LIST_NOTES'});
              self.connectedStatus = ZeppelinService.websocket().isConnected();
              $scope.tgState = true;
            };
            
            var load = function () {
              self.connectedStatus = ZeppelinService.websocket().isConnected();
              getInterpreterStatus("Loading interpreters...");
              getNotesInProject("Loading notebooks...", true);
              $scope.tgState = true;
            };
            
            init();

            self.stopInterpreter = function (interpreter) {
              if (!interpreter.interpreter.notRunning) {
                self.transition = true;
                interpreter.statusMsg = statusMsgs[2];
                ZeppelinService.restartInterpreter(interpreter.interpreter.id)
                        .then(function (success) {
                          interpreter.interpreter = success.data.body;
                          interpreter.statusMsg = statusMsgs[(success.data.body.notRunning ? 0 : 1)];
                          self.transition = false;
                        }, function (error) {
                          getInterpreterStatus();
                          self.transition = false;
                          growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000, referenceId: 10});
                        });
              }
            };
            
            self.stopLivySession = function (interpreter, sessionId) {
                self.transition = true;
                interpreter.statusMsg = statusMsgs[2];
                ZeppelinService.stopLivySession(interpreter.interpreter.id, sessionId)
                        .then(function (success) {
                          interpreter.interpreter = success.data.body;
                          interpreter.statusMsg = statusMsgs[(success.data.body.notRunning ? 0 : 1)];
                          self.transition = false;
                        }, function (error) {
                          getInterpreterStatus();
                          self.transition = false;
                          growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000, referenceId: 10});
                        });
            };

            self.showLivyUI = function (sessionId) {
              ZeppelinService.getLivySessionAppId(sessionId)
                      .then(function (success) {
                        var appId = success.data;
                        $location.path('project/' + projectId + '/jobMonitor-app/' + appId + "/true");
                      }, function (error) {
                        growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000, referenceId: 10});
                      });
            };
            
            self.showSparkUI = function (sessionId) {
              ZeppelinService.getSparkAppId()
                      .then(function (success) {
                        var appId = success.data;
                        $location.path('project/' + projectId + '/jobMonitor-app/' + appId +"/" );
                      }, function (error) {
                        growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000, referenceId: 10});
                      });
            };
            
            self.refreshInterpreters = function () {
              refresh();
            };

            self.refreshDashboard = function () {
              getNotesInProject(null, false);
            };
            self.openNote = function (note) {
              window.open(getLocationBase() + "/zeppelin/#!/notebook/" + note.id);
            };

            self.openZeppelin = function () {
              window.open(getLocationBase() + "/zeppelin");
            };

            self.createNewNote = function () {
              var noteName;
              ModalService.noteName('md', '', '', '').then(
                      function (success) {
                        noteName = success.val;
                        ZeppelinService.createNotebook(noteName).then(function (success) {
                          self.notes.push(success.data.body);
                          growl.success("Notebook created successfully.", 
                          {title: 'Success', ttl: 5000, referenceId: 10});
                        }, function (error) {
                          growl.error(error.data.errorMsg, 
                          {title: 'Error', ttl: 5000, referenceId: 10});
                        });
                      },
                      function (error) {
                      });

            };
            
            self.deleteNote = function (note) {
              ZeppelinService.deleteNotebook(note.id).then(function (success) {                
                growl.success("Notebook deleted.",
                        {title: 'Success', ttl: 5000, referenceId: 10});
              }, function (error) {
                growl.error(error.data.message,
                        {title: 'Error', ttl: 5000, referenceId: 10});
              });
            };
            
            self.clearCache = function () {
              startLoading("Restarting zeppelin...");
              ZeppelinService.restart().then( function (success) {
                  //stopLoading();
                  $route.reload();
                }, function (error) {
                  stopLoading();
                  growl.info(error.data.errorMsg, {title: 'Error', ttl: 5000, referenceId: 10});                  
                });
            };
            
            self.selectInterpreter = function(interpreter) {
              self.selectedInterpreter = interpreter;
              console.log(interpreter);
            };

            ZeppelinService.websocket().ws.onMessage(function (event) {
              var payload;
              if (event.data) {
                payload = angular.fromJson(event.data);
              }
              console.log('Receive << %o, %o', payload.op, payload);
              var op = payload.op;
              //var data = payload.data;
              if (op === 'CREATED_SOCKET') {
                load();
                loaded = true;
              } else if (loaded && op === 'NOTES_INFO') {
                getNotesInProject(null, true);
              } 
            });
            
            ZeppelinService.websocket().ws.onOpen(function () {
              console.log('Websocket created');
            });

            ZeppelinService.websocket().ws.onError(function (event) {
              console.log('error message: ', event);
              self.connectedStatus = false;
            });

            ZeppelinService.websocket().ws.onClose(function (event) {
              self.connectedStatus = false;
              console.log('close message: ', event);              
              //close code should be 1012 (service restart) but chrome shows 
              //closed abnormally (1006) this might cause problem when the socket 
              //is closed but not restarted 
              if (event.code === 1012) {
                startLoading("Restarting zeppelin...");
                $route.reload();
              }
            }); 
            
            $scope.$on("$destroy", function () {
              console.log('closeing ws');
              ZeppelinService.wsDestroy();
              loaded = false;
            });
            
            //refresh interpreter status when we return to zeppelin dashbord. 
            window.onfocus = function () {
              if (loaded) {
                refresh();                
              }
            };
          }]);
