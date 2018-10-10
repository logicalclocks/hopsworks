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
        .controller('ZeppelinCtrl', ['$scope', '$routeParams', '$route',
          'growl', 'ModalService', 'ZeppelinService', '$location',
          function ($scope, $routeParams, $route, growl, ModalService, ZeppelinService, $location) {

            var self = this;
            self.TRASH_FOLDER_ID = '~Trash';
            self.interpretersRefreshing = false;
            self.notesRefreshing = false;
            self.interpreters = [];
            self.tutorialNotes = [];
            self.notes = [];
            self.noteTree = null;
            self.flatFolderMap = {};
            self.currentFolder = null;
            self.pathArray = [];
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
                  setNotes(self.notes);
                  self.notesRefreshing = false;
                  stopLoading();
                }, function (error) {
                  self.notesRefreshing = false;
                  growl.warning(error.data.errorMsg, {title: 'Error', ttl: 5000, referenceId: 10});
                  stopLoading();
                });
              } else {
                ZeppelinService.notebooks().then(function (success) {
                  self.notes = success.data.body;
                  setNotes(self.notes);
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
                        $location.path('project/' + projectId + '/jobMonitor-app/' + appId + "/true/zeppelin");
                      }, function (error) {
                        growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000, referenceId: 10});
                      });
            };

            self.showSparkUI = function (sessionId) {
              ZeppelinService.getSparkAppId()
                      .then(function (success) {
                        var appId = success.data;
                        $location.path('project/' + projectId + '/jobMonitor-app/' + appId + "/");
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
            
            self.breadcrumbLen = function () {
              if (self.pathArray === undefined || self.pathArray === null) {
                return 0;
              }
              var displayPathLen = 10;
              if (self.pathArray.length <= displayPathLen) {
                return self.pathArray.length - 1;
              }
              return displayPathLen;
            };
            
            self.goToFolderIndex = function (index) {
              var newPathArray = self.pathArray.slice(0);
              newPathArray.splice(index, newPathArray.length - index);
              self.goToFolder(newPathArray);
            };
            
            self.goToFolder = function (path) {
              self.pathArray = path.slice(0);
              if (self.pathArray.length === 0) {
                self.home();
                return;
              }
              var id = path.join('/');
              self.currentFolder = self.flatFolderMap[id];
              if (self.currentFolder == null) {
                self.home();
              }
            };

            self.openNote = function (note) {
              if (note.children != null) { // must be !=
                self.currentFolder = note;
                self.pathArray.push(note.name);
              } else {
                 window.open(getLocationBase() + "/zeppelin/#/notebook/" + note.id);
              }
            };
            
            self.home = function () {
              self.pathArray = [];
              self.currentFolder = self.noteTree;
            };
            
            self.back = function () {
              self.pathArray.pop();
              self.goToFolder(self.pathArray);
            };
            
            self.moveToTrash = function (note) {
              if (note.children == null) {
                ZeppelinService.moveNoteToTrash(note.id);
              } else {
                ZeppelinService.moveFolderToTrash(note.id);
              }
            };
            
            self.deleteNote = function (note) {
              if (note.children == null) {
                ZeppelinService.deleteNote(note.id);
              } else {
                ZeppelinService.removeFolder(note.id);
              }
            };
            
            self.restore = function (note) {
              if (note.children == null) {
                ZeppelinService.restoreNote(note.id);
              } else {
                ZeppelinService.restoreFolder(note.id);
              }
            };
            
            self.restoreAll = function () {
              ZeppelinService.restoreAll();
            };
            
            self.emptyTrash = function () {
              ZeppelinService.emptyTrash();
            };

            self.openZeppelin = function () {
              window.open(getLocationBase() + "/zeppelin");
            };

            self.createNewNote = function () {
              var note;
              ModalService.noteCreate('md', '', '', self.interpreters).then(
                function (success) {
                  note = success.val;
                  ZeppelinService.createNotebook(note).then(function (success) {
                    growl.success("Notebook created successfully.", {title: 'Success', ttl: 5000, referenceId: 10});
                  }, function (error) {
                    growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000, referenceId: 10});
                  });
                }, function (error) { });
            };

            self.clearCache = function () {
              startLoading("Restarting zeppelin...");
              ZeppelinService.restart().then(function (success) {
                //stopLoading();
                $route.reload();
              }, function (error) {
                stopLoading();
                growl.info(error.data.errorMsg, {title: 'Error', ttl: 5000, referenceId: 10});
              });
            };

            self.selectInterpreter = function (interpreter) {
              self.selectedInterpreter = interpreter;
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
	      } else if (op === 'ERROR_INFO') {
		growl.error(payload.data.info, {title: 'Error', ttl: 5000, referenceId: 10});
              } else if (loaded && op === 'NOTES_INFO') {
                self.notes = payload.data.notes;
                setNotes(self.notes);
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
              } else if(event.code === 1000) {
                growl.warning(event.reason,
                            {title: 'Error', ttl: 5000, referenceId: 10});
                self.loading = false;
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
            
            // mostly taken from org/apache/zeppelin/zeppelin-web/src/components/note-list/note-list.factory.js
            var setNotes = function (notesList) {
              self.noteTree = {children: []};
              self.flatFolderMap = {};
              var deferred = _.reduce(notesList, function (root, note) {
                var noteName = note.name || note.id;
                var nodes = noteName.match(/([^\/][^\/]*)/g);

                // recursively add nodes
                addNode(root, nodes, note.id);

                return root;
              }, self.noteTree);
              $.when(deferred).done(function () {
                self.goToFolder(self.pathArray);
              });
            };

            var addNode = function (curDir, nodes, noteId) {
              if (nodes.length === 1) {
                curDir.children.push({
                  name: nodes[0],
                  id: noteId,
                  path: curDir.id ? curDir.id + '/' + nodes[0] : nodes[0],
                  isTrash: curDir.id ? curDir.id.split('/')[0] === self.TRASH_FOLDER_ID : false
                });
              } else {
                var node = nodes.shift();
                var dir = _.find(curDir.children, function (c) {
                  return c.name === node && c.children !== undefined;
                });
                if (dir !== undefined) { // found an existing dir
                  addNode(dir, nodes, noteId);
                } else {
                  var newDir = {
                    id: curDir.id ? curDir.id + '/' + node : node,
                    name: node,
                    hidden: true,
                    children: [],
                    isTrash: curDir.id ? curDir.id.split('/')[0] === self.TRASH_FOLDER_ID : false
                  };        
                  
                  // add the folder to flat folder map
                  self.flatFolderMap[newDir.id] = newDir;
                  
                  curDir.children.push(newDir);
                  addNode(newDir, nodes, noteId);
                }
              }
            };


          }]);
