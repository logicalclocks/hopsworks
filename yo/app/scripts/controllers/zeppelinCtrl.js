'use strict';

angular.module('hopsWorksApp')
        .controller('ZeppelinCtrl', ['$scope','$routeParams',
          'growl', 'ModalService', 'ZeppelinService',
          function ($scope, $routeParams, growl, ModalService, ZeppelinService) {

            var self = this;
            self.interpretersRefreshing = false;
            self.notesRefreshing = false;
            self.interpreters = [];
            self.tutorialNotes = [];
            self.notes = [];
            self.transition = false;
            $scope.tgState = true;
            var projectId = $routeParams.projectID;
            var statusMsgs = ['stopped    ', 'running    ', 'stopping...', 'restarting...'];
            
            self.deselect = function () {
              self.selected = null;
              refresh();
              getNotesInProject();
            };

            var getInterpreterStatus = function () {
              self.interpreters = [];
              ZeppelinService.interpreters().then(function (success) {
                for (var k in success.data.body) {
                  self.interpreters.push({interpreter: success.data.body[k], statusMsg: statusMsgs[(success.data.body[k].notRunning ? 0 : 1)]});
                }
              }, function (error) {
                console.log(error);
              });
            };

            var getNotesInProject = function () {
              self.notesRefreshing = true;
              ZeppelinService.notebooks().then(function (success) {
                self.notes = success.data.body;
                self.notesRefreshing = false;
              }, function (error) {
                self.notesRefreshing = false;
                console.log(error);
              });
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
                console.log(error);
              });
            };

            var init = function () {
              getInterpreterStatus();
              getNotesInProject();
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

            self.refreshInterpreters = function () {
              refresh();
            };

            self.refreshDashboard = function () {
              getNotesInProject();
            };
            self.openNote = function (note) {
              window.open(getLocationBase() + "/zeppelin/#/notebook/" + note.id);
            };

            self.openZeppelin = function () {
              window.open(getLocationBase() + "/zeppelin");
            };

            self.createNewNote = function () {
              var noteName;
              ModalService.noteName('md','','','').then(
                        function (success) {
                          noteName = success.val;
                          ZeppelinService.createNotebook(noteName).then(function (success) {
                            self.notes.push(success.data.body);
                            growl.success("Notebook created successfully.", {title: 'Success', ttl: 5000, referenceId: 10});
                          }, function (error) {
                            growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000, referenceId: 10});
                          });
                        },
                        function(error) {
                });
              
            };

          }]);
