/**
 * Controller for the Adam jobs page.
 */

'use strict';

angular.module('hopsWorksApp')
        .controller('AdamCtrl', ['$scope', '$routeParams', 'growl', 'JobHistoryService', '$interval', 'AdamService', 'ModalService',
          function ($scope, $routeParams, growl, JobHistoryService, $interval, AdamService, ModalService) {

            //Set all the variables required to be a jobcontroller:
            //For fetching job history
            var self = this;
            self.arguments = [];
            self.options = [];
            this.JobHistoryService = JobHistoryService;
            this.projectId = $routeParams.projectID;
            this.jobType = 'ADAM';
            this.growl = growl;
            //For letting the user select a file
            this.ModalService = ModalService;
            this.selectFileRegex = /[^]*/; //matches anything
            this.selectFileErrorMsg = "Please select a file or folder.";
            this.onFileSelected = function (path) {
              //Set the path in the arguments.
              if (self.fileSelectionIsArgument) {
                self.arguments[self.fileSelectionName] = path;
              } else {
                self.options[self.fileSelectionName] = path;
              }
              self.fileSelectionName = null;
            };
            //For job execution
            this.$interval = $interval;
            this.callExecute = function () {
              return AdamService.runJob(
                      self.projectId, self.runConfig);
            };
            this.onExecuteSuccess = function (success) {
              self.runConfig = null;
              self.arguments = [];
              self.options = [];
            };


            /*
             * Get all Spark job history objects for this project.
             */
            this.getAdamHistory = function () {
              getHistory(this);
            };

            this.getAdamHistory();

            /**
             * Get a list of commands from the server.
             * @returns {undefined}
             */
            this.getCommandList = function () {
              AdamService.getCommandList(self.projectId).then(
                      function (success) {
                        self.commandList = success.data.commands;
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
            this.selectFile = function (isArgument, name) {
              self.fileSelectionIsArgument = isArgument;
              self.fileSelectionName = name;
              selectFile(this);
            };

            this.execute = function () {
              //First: fill in the runConfig
              for( var x in this.runConfig.selectedCommand.arguments){
                var name = this.runConfig.selectedCommand.arguments[x].name;
                if(this.arguments[name]){
                  this.runConfig.selectedCommand.arguments[x].value = this.arguments[name];
                }
              }
              for( var x in this.runConfig.selectedCommand.options){
                var name = this.runConfig.selectedCommand.options[x].name;
                if(this.options[name]){
                  this.runConfig.selectedCommand.options[x].value = this.options[name];
                }
              }
              //if (self.fileSelectionIsArgument) {
//                var args = self.runConfig.selectedCommand.arguments;
//                var arg;
//                for(arg in args){
//                  if(args[arg]===self.fileSelectionName){
//                    self.runConfig.selectedCommand.arguments[arg].value = path;
//                  }
//                }
               // self.arguments[self.fileSelectionName] = path;
             // } else {
//                var opts = self.runConfig.selectedCommand.options;
//                var opt;
//                for(opt in opts){
//                  if(opts[opt]===self.fileSelectionName){
//                    self.runConfig.selectedCommand.options[opt].value = path;
//                  }
//                }
             //   self.options[self.fileSelectionName] = path;
              execute(this);
            };

            this.selectJob = function (job) {
              selectJob(this, job);
            };
            
            /**
             * Select a command by sending the name to the server, gets an 
             * AdamJobConfiguration back.
             * @param {string} command
             * @returns {undefined}
             */
            this.selectCommand = function () {
              self.fileSelectionIsArgument = null;
              self.fileSelectionName = null;
              AdamService.getCommand(self.projectId, this.selectedCommand).then(
                      function (success) {
                        self.runConfig = success.data;
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Error', ttl: 15000});
              });
            };

            /**
             * Close the poller if the controller is destroyed.
             */
            $scope.$on('$destroy', function () {
              $interval.cancel(this.poller);
            });

          }]);




