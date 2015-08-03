/**
 * Controller for the Adam jobs page.
 */

'use strict';

angular.module('hopsWorksApp')
        .controller('AdamCtrl', ['$routeParams', 'growl', 'AdamService', 'ModalService',
          function ($routeParams, growl, AdamService, ModalService) {

            //Set all the variables required to be a jobcontroller:
            //For fetching job history
            var self = this;
            this.projectId = $routeParams.projectID;
            this.jobType = 'ADAM';
            this.growl = growl;
            //For letting the user select a file
            this.ModalService = ModalService;
            this.selectFileRegex = /[^]*/; //matches anything
            this.selectFileErrorMsg = "Please select a file or folder.";
            this.onFileSelected = function (path) {
              //Set the path in the arguments.
              self.processparameter.value = path;
            };

            /**
             * Get a list of commands from the server.
             * @returns {undefined}
             */
            this.getCommandList = function () {
              AdamService.getCommandList(self.projectId).then(
                      function (success) {
                        self.commandList = success.data;
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Error', ttl: 15000});
              });
            };

            this.getCommandList();

            /**
             * Select a file from HDFS.
             * @param {boolean} isArgument True if the file is to be used as an 
             * argument, false if it serves as an option.
             * @param {string} name The name of the argument or option for which 
             * the file is selected.
             * @returns {undefined}
             */
            this.selectFile = function (parameter) {
              self.processparameter = parameter;
              selectFile(this);
            };

            /**
             * Select a command by sending the name to the server, gets an 
             * AdamJobConfiguration back.
             * @param {string} command
             * @returns {undefined}
             */
            this.selectCommand = function (command, phasekeeper) {
              self.selectedCommand = command;
              self.fileSelectionIsArgument = null;
              self.fileSelectionName = null;
              AdamService.getCommand(self.projectId, self.selectedCommand).then(
                      function (success) {
                        self.runConfig = success.data;
                        phasekeeper.mainFileSelected(self.selectedCommand);
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Error', ttl: 15000});
              });
            };

          }]);




