/**
 * Created by stig on 2015-07-27.
 * Controller for the jobs creation page.
 * 
 * As it stands, this controller contains a lot of logic concerning all different 
 * job types. It would be nicer to have these as Mixins in a different file. 
 * Guess that's a TODO.
 */

'use strict';

angular.module('hopsWorksApp')
        .controller('NewJobCtrl', ['$routeParams', 'growl', 'JobService',
          '$location', 'ModalService', 'StorageService', '$scope', 'SparkService',
          'CuneiformService', 'AdamService',
          function ($routeParams, growl, JobService,
                  $location, ModalService, StorageService, $scope, SparkService,
                  CuneiformService, AdamService) {

            var self = this;
            //Set services as attributes 
            this.ModalService = ModalService;
            this.growl = growl;

            //Set some (semi-)constants
            this.selectFileRegexes = {
              "SPARK": /.jar\b/,
              "LIBRARY": /.jar\b/,
              "CUNEIFORM": /.cf\b/,
              "ADAM": /[^]*/
            };
            this.selectFileErrorMsgs = {
              "SPARK": "Please select a JAR file.",
              "LIBRARY": "Please select a JAR file.",
              "CUNEIFORM": "Please select a Cuneiform workflow. The file should have the extension '.cf'.",
              "ADAM": "Please select a file or folder."
            };
            this.projectId = $routeParams.projectID;

            //Create variables for user-entered information
            this.jobtype; //Will hold the selection of which job to create.
            this.jobname; //Will hold the name of the job
            this.localResources = {"entry": []}; //Will hold extra libraries
            this.phase = 0; //The phase of creation we are in.
            this.runConfig; //Will hold the job configuration
            this.sparkState = {//Will hold spark-specific state
              "selectedJar": null //The path to the selected jar
            };
            this.adamState = {//Will hold ADAM-specific state
              "processparameter": null, //The parameter currently being processed
              "commandList": null, //The ADAM command list.
              "selectedCommand": null //The selected ADAM command
            };
            this.schedule = {
              "unit": "hour",
              "number": 1,
              "addition": "",
              "startDate": ""
            };

            //Variables for front-end magic
            this.accordion1 = {//Contains the job name
              "isOpen": true,
              "visible": true,
              "value": "",
              "title": "Job name"};
            this.accordion2 = {//Contains the job type
              "isOpen": false,
              "visible": false,
              "value": "",
              "title": "Job type"};
            this.accordion3 = {// Contains the main execution file (jar, workflow,...)
              "isOpen": false,
              "visible": false,
              "value": "",
              "title": ""};
            this.accordion4 = {// Contains the job setup (main class, input variables,...)
              "isOpen": false,
              "visible": false,
              "value": "",
              "title": ""};
            this.accordion5 = {//Contains the configuration and creation
              "isOpen": false,
              "visible": false,
              "value": "",
              "title": "Configure and create"};
            this.accordion6 = {//Contains the schedule
              "isOpen": false,
              "visible": false,
              "value": "",
              "title": "Set a schedule (optional)"};


            /**
             * Create the job.
             * @param {type} type
             * @param {type} config
             * @returns {undefined}
             */
            this.createJob = function () {
              self.runConfig.appName = self.jobname;
              self.runConfig.localResources = self.localResources;
              self.schedule.startDate = $('#scheduleDatePicker').data("DateTimePicker").date().valueOf();
              JobService.createNewJob(self.projectId, self.getJobType(), self.runConfig).then(
                      function (success) {
                        $location.path('project/' + self.projectId + '/jobs');
                        StorageService.remove(self.projectId + "newjob");
                        self.removed = true;
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Error', ttl: 15000});
              });
            };

            /**
             * Callback method for when the user filled in a job name. Will then 
             * display the type selection.
             * @returns {undefined}
             */
            this.nameFilledIn = function () {
              if (self.phase == 0) {
                self.phase = 1;
                self.accordion2.isOpen = true; //Open type selection
                self.accordion2.visible = true; //Display type selection
              }
              self.accordion1.value = " - " + self.jobname; //Edit panel title
              self.removed = false;
            };

            /**
             * Callback method for when the user selected a job type. Will then 
             * display the file selection.
             * @returns {undefined}
             */
            this.jobTypeChosen = function () {
              self.phase = 2;
              self.accordion3.isOpen = true; //Open file selection
              var type;
              switch (self.jobtype) { //Set the panel titles according to job type
                case 0:
                  self.accordion3.title = "Workflow file";
                  self.accordion4.title = "Input variables";
                  type = "Cuneiform";
                  break;
                case 1:
                  self.accordion3.title = "JAR file";
                  self.accordion4.title = "Job details";
                  type = "Spark";
                  break;
                case 2:
                  self.accordion3.title = "ADAM command";
                  self.accordion4.title = "Job arguments";
                  type = "ADAM";
                  break;
              }
              self.accordion1.isOpen = false; //Close job name panel
              self.accordion1.value = " - " + self.jobname; //Set job name panel title
              self.accordion3.visible = true; //Display file selection
              self.accordion2.value = " - " + type; //Set job type panel title
              self.accordion2.isOpen = false; //Close job type panel
              self.accordion4.isOpen = false; //Close job setup
              self.accordion4.visible = false; //Hide job setup
              self.accordion5.visible = false; // Hide job configuration
              self.accordion6.visible = false; //Hide schedule
              self.accordion3.value = ""; //Reset selected file
            };

            /**
             * Get the String representation of the selected jobType.
             * @returns {String}
             */
            this.getJobType = function () {
              switch (self.jobtype) {
                case 0:
                  return "CUNEIFORM";
                case 1:
                  return "SPARK";
                case 2:
                  return "ADAM";
                default:
                  return null;
              }
            }

            /**
             * Callback method for when the main job file has been selected.
             * @param {type} path
             * @returns {undefined}
             */
            this.mainFileSelected = function (path) {
              self.phase = 3;
              self.accordion4.isOpen = true; // Open job setup
              self.accordion4.visible = true; // Show job setup
              self.accordion5.visible = true; // Show job config
              self.accordion3.value = " - " + path; // Set file selection title
              self.accordion3.isOpen = false; //Close file selection
              self.accordion6.visible = true; //Show schedule
            };

            /**
             * Callback for when the job setup has been completed.
             * @returns {undefined}
             */
            this.jobDetailsFilledIn = function () {
              self.phase = 4;
            };

            // Methods for schedule updating
            this.updateNumberOfScheduleUnits = function () {
              self.schedule.addition = self.schedule.number == 1 ? "" : "s";
            };

            /**
             * Open a dialog for file selection.
             * @param {String} reason Goal for which the file is selected. (JobType or "LIBRARY").
             * @param {Object} (Optional) The Adam parameter to bind.
             * @returns {undefined}
             */
            this.selectFile = function (reason, parameter) {
              if (reason.toUpperCase() == "ADAM") {
                self.adamState.processparameter = parameter;
              }
              ModalService.selectFile('lg', self.selectFileRegexes[reason],
                      self.selectFileErrorMsgs[reason]).then(
                      function (success) {
                        self.onFileSelected(reason, success);
                      }, function (error) {
                //The user changed their mind.
              });
            };

            /**
             * Callback for when the user selected a file.
             * @param {String} reason
             * @param {String} path
             * @returns {undefined}
             */
            this.onFileSelected = function (reason, path) {
              var filename = getFileName(path);
              switch (reason.toUpperCase()) {
                case "SPARK":
                  self.sparkState.selectedJar = filename;
                  SparkService.inspectJar(self.projectId, path).then(
                          function (success) {
                            self.runConfig = success.data;
                            self.mainFileSelected(filename);
                          }, function (error) {
                    growl.error(error.data.errorMsg, {title: 'Error', ttl: 15000});
                  });
                  break;
                case "LIBRARY":
                  self.localResources.entry.push({"key": filename, "value": path});
                  break;
                case "CUNEIFORM":
                  CuneiformService.inspectStoredWorkflow(self.projectId, path).then(
                          function (success) {
                            self.runConfig = success.data;
                            self.mainFileSelected(filename);
                          }, function (error) {
                    growl.error(error.data.errorMsg, {title: 'Error', ttl: 15000});
                  });
                  break;
                case "ADAM":
                  self.adamState.processparameter.value = path;
                  break;
              }
            };

            /**
             * Get a list of ADAM commands from the server.
             * @returns {undefined}
             */
            this.getCommandList = function () {
              AdamService.getCommandList(self.projectId).then(
                      function (success) {
                        self.adamState.commandList = success.data;
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Error', ttl: 15000});
              });
            };

            /**
             * Init method: restore any previous state.
             * @returns {undefined}
             */
            var init = function () {
              var stored = StorageService.recover(self.projectId + "newjob");
              if (stored) {
                //Job information
                self.jobtype = stored.jobtype;
                self.jobname = stored.jobname;
                self.localResources = stored.localResources;
                self.phase = stored.phase;
                self.runConfig = stored.runConfig;
                if (self.jobtype == 1) {
                  self.sparkState = stored.sparkState;
                } else if (self.jobtype == 2) {
                  self.adamState = stored.adamState;
                }
                self.schedule = stored.schedule;
                //GUI state
                self.accordion1 = stored.accordion1;
                self.accordion2 = stored.accordion2;
                self.accordion3 = stored.accordion3;
                self.accordion4 = stored.accordion4;
                self.accordion5 = stored.accordion5;
                self.accordion6 = stored.accordion6;
              }
              if (self.adamState.commandList == null) {
                self.getCommandList();
              }

            };
            init(); //Call upon create

            /**
             * Select an ADAM command by sending the name to the server, gets an 
             * AdamJobConfiguration back.
             * @param {string} command
             * @returns {undefined}
             */
            this.selectCommand = function (command) {
              self.adamState.selectedCommand = command;
              AdamService.getCommand(self.projectId, self.adamState.selectedCommand).then(
                      function (success) {
                        self.runConfig = success.data;
                        self.mainFileSelected(self.adamState.selectedCommand);
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Error', ttl: 15000});
              });
            };

            /**
             * Save state upon destroy.
             */
            $scope.$on('$destroy', function () {
              if (self.removed) {
                //The state was removed explicitly; do not add again.
                return;
              }
              var state = {
                "jobtype": self.jobtype,
                "jobname": self.jobname,
                "localResources": self.localResources,
                "phase": self.phase,
                "runConfig": self.runConfig,
                "sparkState": self.sparkState,
                "adamState": self.adamState,
                "schedule": self.schedule,
                "accordion1": self.accordion1,
                "accordion2": self.accordion2,
                "accordion3": self.accordion3,
                "accordion4": self.accordion4,
                "accordion5": self.accordion5,
                "accordion6": self.accordion6
              };
              StorageService.store(self.projectId + "newjob", state);
            });

          }]);


