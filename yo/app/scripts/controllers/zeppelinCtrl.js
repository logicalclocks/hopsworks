'use strict';

angular.module('hopsWorksApp')
        .controller('ZeppelinCtrl', ['$location', '$routeParams',
          'growl', 'ModalService', 'ZeppelinService',
          function ($location, $routeParams, growl, ModalService, ZeppelinService) {

            var self = this;
            self.interpreters = [];
            self.tutorialNotes = [];
            self.notes = [];
            var projectId = $routeParams.projectID;

            ZeppelinService.settings().then(function (success) {
              for (var k in success.data.body) {
                self.interpreters.push({setting: success.data.body[k], status: true});
              }
              console.log(self.interpreters);
            }, function (error) {
              console.log(error);
            });

            ZeppelinService.notebooks(projectId).then(function (success) {
              self.notes = success.data.body;
              console.log(success);
            }, function (error) {
              console.log(error);
            });
            
            ZeppelinService.tutorialNotebooks().then(function (success) {
              self.tutorialNotes = success.data.body;
              console.log(success);
            }, function (error) {
              console.log(error);
            });

            self.changeState = function (interpreter, index) {
              self.index = index;
              self.interpreter = interpreter;
              if (interpreter.status) {
                //stop
                toggleForwardIndeterminate(index);
                ZeppelinService.stopInterpreter(interpreter.setting.id)
                        .then(function (success) {
                          toggleForwardIndeterminate(index);
                        }, function (error) {
                          toggleBackIndeterminate(interpreter, index);
                          growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000, referenceId: 10});
                        });
              } else {
                //start
                toggleForwardIndeterminate(index);
                ZeppelinService.startInterpreter(interpreter.setting.id)
                        .then(function (success) {
                          toggleForwardIndeterminate(index);
                        }, function (error) {
                          toggleBackIndeterminate(interpreter, index);
                          growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000, referenceId: 10});
                        });
              }
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

            var toggleBackIndeterminate = function (interpreter, index) {
              $("input[name=" + index + "]").bootstrapSwitch('toggleIndeterminate', true);
              $("input[name=" + index + "]").bootstrapSwitch('toggleDisabled', true);
              $("input[name=" + index + "]").bootstrapSwitch('toggleState', true);
              interpreter.status = !interpreter.status;
            };
          }]);
