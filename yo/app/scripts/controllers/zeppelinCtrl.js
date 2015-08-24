'use strict';

angular.module('hopsWorksApp')
        .controller('ZeppelinCtrl', ['$routeParams',
          'growl', 'ModalService', 'ZeppelinService',
          function ($routeParams, growl, ModalService, ZeppelinService) {

            var self = this;
            self.interpreters = [];
            self.tutorialNotes = [];
            self.notes = [];
            self.transition = false;
            var projectId = $routeParams.projectID;
            var statusMsgs = ['stopped    ', 'running    ', 'stopping...', 'starting...'];

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
              ZeppelinService.notebooks(projectId).then(function (success) {
                self.notes = success.data.body;
              }, function (error) {
                console.log(error);
              });
            };

            var getTutorialNotes = function () {
              ZeppelinService.tutorialNotebooks().then(function (success) {
                self.tutorialNotes = success.data.body;
              }, function (error) {
                console.log(error);
              });
            };

            var refresh = function () {
              ZeppelinService.interpreters().then(function (success) {
                for (var i in self.interpreters) {
                  var interpreter = success.data.body;
                  if (self.interpreters[i].statusMsg === statusMsgs[0] || 
                          self.interpreters[i].statusMsg === statusMsgs[1]) {
                    self.interpreters[i].interpreter = interpreter[self.interpreters[i].interpreter.name];
                    self.interpreters[i].statusMsg = statusMsgs[(interpreter[self.interpreters[i].interpreter.name].notRunning ? 0 : 1)];
                  } 
                }
              }, function (error) {
                console.log(error);
              });
            };

            var init = function () {
              getInterpreterStatus();
              getNotesInProject();
              getTutorialNotes();
            };

            init();

            self.changeState = function (interpreter, index) {
              if (!interpreter.interpreter.notRunning) {
                //start
                self.transition = true;
                interpreter.statusMsg = statusMsgs[3];
                toggleForwardIndeterminate(index);
                ZeppelinService.startInterpreter(projectId, interpreter.interpreter.id)
                        .then(function (success) {
                          toggleForwardIndeterminate(index);
                          interpreter.interpreter = success.data.body;
                          interpreter.statusMsg = statusMsgs[(success.data.body.notRunning ? 0 : 1)];
                          self.transition = false;
                        }, function (error) {
                          toggleForwardIndeterminate(index);
                          getInterpreterStatus();
                          self.transition = false;
                          growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000, referenceId: 10});
                        });

              } else {
                //stop
                self.transition = true;
                interpreter.statusMsg = statusMsgs[2];
                toggleForwardIndeterminate(index);
                ZeppelinService.stopInterpreter(interpreter.interpreter.id)
                        .then(function (success) {
                          toggleForwardIndeterminate(index);
                          interpreter.interpreter = success.data.body;
                          interpreter.statusMsg = statusMsgs[(success.data.body.notRunning ? 0 : 1)];
                          self.transition = false;
                        }, function (error) {
                          toggleForwardIndeterminate(index);
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
              //getTutorialNotes();//not nessesery b/c tutorials do not change.
            };
            self.openNote = function (note) {
              window.open(getLocationBase() + "/zeppelin/#/notebook/" + note.id);
            };

            self.openZeppelin = function () {
              window.open(getLocationBase() + "/zeppelin");
            };

            self.createNewNote = function () {
              ZeppelinService.createNotebook(projectId).then(function (success) {
                console.log(success);
                self.notes.push(success.data.body);
                growl.success("Notebook created successfully. To rename open it and double click on the name.", {title: 'Success', ttl: 5000, referenceId: 10});
              }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000, referenceId: 10});
              });
            };

            var toggleForwardIndeterminate = function (index) {
              $("input[name=" + index + "]").bootstrapSwitch('toggleIndeterminate', true);
              $("input[name=" + index + "]").bootstrapSwitch('toggleDisabled', true);
            };

            window.onbeforeunload = function () {
              if (!self.transition) {
                return;
              } else {
                return "Are you sure you want to reload this page? You will lose any interpreter status progress.";
              }

            };

          }]);
