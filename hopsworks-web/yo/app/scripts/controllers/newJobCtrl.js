/**
 * Created by stig on 2015-07-27.
 * Controller for the jobs creation page.
 * 
 * As it stands, self controller contains a lot of logic concerning all different 
 * job types. It would be nicer to have these as Mixins in a different file. 
 * Guess that's a TODO.a
 */

'use strict';

angular.module('hopsWorksApp')
        .controller('NewJobCtrl', ['$routeParams', 'growl', 'JobService',
          '$location', 'ModalService', 'StorageService', '$scope', 'SparkService',
          'AdamService', 'FlinkService', 'TourService', 'HistoryService',
          'KafkaService', 'ProjectService', '$timeout',
          function ($routeParams, growl, JobService,
                  $location, ModalService, StorageService, $scope, SparkService,
                  AdamService, FlinkService, TourService, HistoryService,
                  KafkaService, ProjectService, $timeout) {

            var self = this;
            self.tourService = TourService;
            self.projectIsGuide = false;
            self.flinkjobtype;
            self.resourceType;
            //Set services as attributes
            self.ModalService = ModalService;
            self.growl = growl;
            self.projectId = $routeParams.projectID;
            ////////////////////////////////////////////////////////////////////
            //Kafka topics for this project
            self.topics = [];
            self.kafkaSelected = false;
            self.consumerGroups = [{id: 'group1', "name": "default"}];
            self.groupsSelected = false;
            self.showAdvanced = false;
            self.selectedTopics = [];
            self.projectName = "";
            self.getAllTopics = function () {
              if (self.kafkaSelected) {
                if (typeof self.runConfig.kafka !== "undefined" && 
                        typeof self.runConfig.kafka.topics !== "undefined") {
                  self.selectedTopics = self.runConfig.kafka.topics;
                }
                self.topics = [];
                return KafkaService.getProjectAndSharedTopics(self.projectId)
                .then(
                        function (success) {
                          var topics = success.data;
                          for (var i = 0; i < topics.length; i++) {
                            if (self.selectedTopics.length > 0) {
                              var found = false;
                              for (var j = 0; j < self.selectedTopics.length; j++) {
                                if (self.selectedTopics[j]['name'] === topics[i]['name']) {
                                  found = true;
                                  break;
                                }
                              }
                              self.topics.push({name: topics[i]['name'], ticked: found});
                            } else {
                              self.topics.push({name: topics[i]['name'], ticked: false});
                            }
                          }
                        }, function (error) {
                    growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000});
                });
              }
            };

            self.addGroup = function () {
              var newItemNo = self.consumerGroups.length + 1;
              self.consumerGroups.push({'id': 'group' + newItemNo});
            };

            self.removeGroup = function () {
              var lastItem = self.consumerGroups.length - 1;
              if (lastItem > 0) {
                self.consumerGroups.splice(lastItem);
              }
            };

            self.toggleKafka = function () {
              self.kafkaSelected = !self.kafkaSelected;
            };

            self.toggleAdvanced = function () {
              self.runConfig.kafka.advanced = !self.runConfig.kafka.advanced;
            };

            ////////////////////////////////////////////////////////////////////



            // keep the proposed configurations
            self.autoConfigResult;

            self.setResourceType = function (type) {
              self.resourceType = type;
            };

            //Set some (semi-)constants
            self.selectFileRegexes = {
              "SPARK": /.jar\b/,
              "FLINK": /.jar\b/,
              "LIBRARY": /.jar\b/,
              "ADAM": /[^]*/
            };
            self.selectFileErrorMsgs = {
              "SPARK": "Please select a JAR file.",
              "FLINK": "Please select a JAR file.",
              "LIBRARY": "Please select a JAR file.",
              "ADAM-FILE": "Please select a file.",
              "ADAM-FOLDER": "Please select a folder."
            };

            //Create variables for user-entered information
            self.jobtype; //Will hold the selection of which job to create.
            self.jobname; //Will hold the name of the job

            self.localResources = [];//Will hold extra libraries

            self.newJobName = self.projectId + "_newjob";

            self.phase = 0; //The phase of creation we are in.
            self.runConfig; //Will hold the job configuration
            self.sliderVisible = false;

            self.sliderOptions = {
              min: 1,
              max: 10,
              options: {
                floor: 0,
                ceil: 500
              },
              getPointerColor: function (value) {
                return '#4b91ea';
              }

            };

            self.refreshSlider = function () {
              $timeout(function () {
                $scope.$broadcast('rzSliderForceRender');
              });
            };

            self.toggleSlider = function () {
              self.sliderVisible = !self.sliderVisible;
              if (self.sliderVisible)
                self.refreshSlider();
            };

            self.setInitExecs = function () {
              if (self.sliderOptions.min >
                      self.runConfig.numberOfExecutorsInit) {
                self.runConfig.numberOfExecutorsInit =
                        parseInt(self.sliderOptions.min);
              } else if (self.sliderOptions.max <
                      self.runConfig.numberOfExecutorsInit) {
                self.runConfig.numberOfExecutorsInit =
                        parseInt(self.sliderOptions.max);
              }
            };

            self.sparkState = {//Will hold spark-specific state
              "selectedJar": null //The path to the selected jar
            };
            self.flinkState = {//Will hold flink-specific state
              "selectedJar": null //The path to the selected jar
            };
            self.adamState = {//Will hold ADAM-specific state
              "processparameter": null, //The parameter currently being processed
              "commandList": null, //The ADAM command list.
              "selectedCommand": null //The selected ADAM command
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
            this.accordion6 = {//Contains the pre-configuration and proposals for auto-configuration
              "isOpen": false,
              "visible": false,
              "value": "",
              "title": "Pre-Configuration"};

            this.undoable = false; //Signify if a clear operation can be undone.

            /**
             * Clear the current state (and allow for undo).
             * @returns {undefined}
             */
            self.clear = function () {
              var state = {
                "jobtype": self.jobtype,
                "jobname": self.jobname,
                "localResources": self.localResources,
                "phase": self.phase,
                "runConfig": self.runConfig,
                "sparkState": self.sparkState,
                "flinkState": self.flinkState,
                "adamState": self.adamState,
                "accordions": [self.accordion1, self.accordion2, self.accordion3, self.accordion4, self.accordion5, self.accordion6],
              };
              self.undoneState = state;
              self.undoable = true;
              self.jobtype = null;
              self.jobname = null;
              self.localResources = [];
              self.phase = 0;
              self.runConfig = null;
              self.sparkState = {
                "selectedJar": null //The path to the selected jar
              };
              self.flinkState = {
                "selectedJar": null //The path to the selected jar
              };
              self.adamState = {//Will hold ADAM-specific state
                "processparameter": null, //The parameter currently being processed
                "commandList": null, //The ADAM command list.
                "selectedCommand": null //The selected ADAM command
              };
              //Variables for front-end magic
              self.accordion1 = {//Contains the job name
                "isOpen": true,
                "visible": true,
                "value": "",
                "title": "Job name"};
              self.accordion2 = {//Contains the job type
                "isOpen": false,
                "visible": false,
                "value": "",
                "title": "Job type"};
              self.accordion3 = {// Contains the main execution file (jar, workflow,...)
                "isOpen": false,
                "visible": false,
                "value": "",
                "title": ""};
              self.accordion4 = {// Contains the job setup (main class, input variables,...)
                "isOpen": false,
                "visible": false,
                "value": "",
                "title": ""};
              self.accordion5 = {//Contains the configuration and creation
                "isOpen": false,
                "visible": false,
                "value": "",
                "title": "Configure and create"};
              self.accordion6 = {//Contains the pre-configuration and proposals for auto-configuration
                "isOpen": false,
                "visible": false,
                "value": "",
                "title": "Pre-Configuration"};
            };

            self.exitToJobs = function () {
              self.clear();
              StorageService.remove(self.newJobName);
              self.removed = true;
              $location.path('project/' + self.projectId + '/jobs');
            };

            self.undoClear = function () {
              if (self.undoneState !== null) {
                self.jobtype = self.undoneState.jobtype;
                self.jobname = self.undoneState.jobname;
                self.localResources = self.undoneState.localResources;
                self.phase = self.undoneState.phase;
                self.runConfig = self.undoneState.runConfig;
                self.sparkState = self.undoneState.sparkState;
                self.flinkState = self.undoneState.flinkState;
                self.adamState = self.undoneState.adamState;
                self.accordion1 = self.undoneState.accordions[0];
                self.accordion2 = self.undoneState.accordions[1];
                self.accordion3 = self.undoneState.accordions[2];
                self.accordion4 = self.undoneState.accordions[3];
                self.accordion5 = self.undoneState.accordions[4];
                self.accordion6 = self.undoneState.accordions[4];
              }
              self.unodeState = null;
              self.undoable = false;
            };

            self.kafkaGuideTransition = function () {
              if (angular.equals('producer', self.tourService
                .kafkaJobCreationState)) {
                  self.tourService.kafkaJobCreationState = "consumer";
              } else {
                self.tourService.kafkaJobCreationState = "producer";
              }
            };

            /**
             * Create the job.
             * @returns {undefined}
             */
            self.createJob = function () {
              if (self.kafkaSelected) {
                if (self.projectIsGuide) {
                  self.runConfig.kafka.topics = self.guideKafkaTopics;
                  if (angular.equals('producer', self.tourService
                  .kafkaJobCreationState)) {
                    // Go through again for the consumer. The state is
                    // toggled in newJob.html virtual step
                    self.tourService.currentStep_TourSeven = 0;
                    self.tourService.currentStep_TourSix = 0;
                    self.kafkaGuideTransition();
                  } else {
                    // We are done
                    self.tourService.currentStep_TourSeven = -1;
                    self.tourService.currentStep_TourSix = 1;
                    self.kafkaGuideTransition();
                  }
                }
                if (self.runConfig.kafka.topics === "undefined" || self.runConfig.kafka.topics.length === 0) {
                  growl.warning("Please select topic(s) first", {title: 'Warning', ttl: 5000});
                  return;
                } else {
                  if (self.runConfig.kafka.advanced) {
                    self.runConfig.kafka.consumergroups = self.consumerGroups;
                  } else {
                    delete self.runConfig.kafka.consumergroups;
                  }
                }
              } else {
                delete self.runConfig.kafka;
              }
              self.runConfig.appName = self.jobname;
              self.runConfig.flinkjobtype = self.flinkjobtype;
              self.runConfig.localResources = self.localResources;
              if (self.getJobType() === "SPARK" || self.getJobType() === "PYSPARK" || self.getJobType() === "ADAM") {
                self.runConfig.selectedMinExecutors = self.sliderOptions.min;
                self.runConfig.selectedMaxExecutors = self.sliderOptions.max;
              }
              if (self.tourService.currentStep_TourFour > -1) {
                //self.tourService.resetTours();
                self.tourService.currentStep_TourThree = 2;
                self.tourService.createdJobName = self.jobname;
              }
              JobService.createNewJob(self.projectId, self.getJobType(), self.runConfig).then(
                      function (success) {
                        $location.path('project/' + self.projectId + '/jobs');
                        StorageService.remove(self.newJobName);
                        self.removed = true;
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Error', ttl: 10000});
              });
            };

            /**
             * Callback method for when the user filled in a job name. Will then 
             * display the type selection.
             * @returns {undefined}
             */
            self.nameFilledIn = function () {
              // For Kafka tour
              if (self.projectIsGuide) {
                self.tourService.currentStep_TourSeven = 2;
              }
              if (self.phase === 0) {
                if (!self.jobname) {
                  var date = new Date().getTime() / 1000;
                  self.jobname = "Job-" + date;
                }
                self.phase = 1;
                self.accordion2.isOpen = true; //Open type selection
                self.accordion2.visible = true; //Display type selection
              }
              self.accordion1.value = " - " + self.jobname; //Edit panel title
              self.removed = false;
              self.undoneState = null; //Clear previous state.
              self.undoable = false;
              if (self.tourService.currentStep_TourFour > -1) {
                self.tourService.currentStep_TourFour = 2;
              }
            };

            self.guideSetJobName = function () {
              var jobState = self.tourService.kafkaJobCreationState;
              if (angular.equals('producer', jobState)) {
                self.jobname = "KafkaDemoProducer";
              } else {
                self.jobname = "KafkaDemoConsumer";
              }
            };

            /**
             * Callback method for when the user selected a job type. Will then 
             * display the file selection.
             * @returns {undefined}
             */
            self.jobTypeChosen = function () {
            // For Kafka tour
              if (self.projectIsGuide) {
                self.tourService.currentStep_TourSeven = 4;
              }
              self.phase = 2;
              self.accordion3.isOpen = true; //Open file selection
              var selectedType;
              switch (self.jobtype) { //Set the panel titles according to job type
                case 1:
                case 4:
                  self.accordion3.title = "App file (.jar, .py)";
                  self.accordion4.title = "Job details";
                  selectedType = "Spark";
                  break;
                case 2:
                  self.accordion3.title = "ADAM command";
                  self.accordion4.title = "Job arguments";
                  selectedType = "ADAM";
                  break;
                case 3:
                  self.accordion3.title = "JAR file";
                  self.accordion4.title = "Job details";
                  selectedType = "Flink";
                  break;
              }
              self.accordion1.isOpen = false; //Close job name panel
              self.accordion1.value = " - " + self.jobname; //Set job name panel title
              self.accordion3.visible = true; //Display file selection
              self.accordion2.value = " - " + selectedType; //Set job type panel title
              self.accordion2.isOpen = false; //Close job type panel
              self.accordion4.isOpen = false; //Close job setup
              self.accordion4.visible = false; //Hide job setup
              self.accordion5.visible = false; // Hide job configuration
              self.accordion6.visible = false; // Hide job pre-configuration
              self.accordion3.value = ""; //Reset selected file
              if (self.tourService.currentStep_TourFour > -1) {
                self.tourService.currentStep_TourFour = 4;
              }
            };


            /**
             * Get the String representation of the selected jobType.
             * @returns {String}
             */
            self.getJobType = function () {
              switch (self.jobtype) {
                case 1:
                  return "SPARK";
                case 2:
                  return "ADAM";
                case 3:
                  return "FLINK";
                case 4:
                  return "PYSPARK";
                default:
                  return null;
              }
            };

            self.jobTypeSpark = function () {
              self.jobtype = 1;
              self.jobTypeChosen();
            };

            self.chooseParameters = function () {
              if (!self.runConfig.mainClass && !self.runConfig.args) {
                  self.runConfig.mainClass = 'org.apache.spark.examples.SparkPi';
                  self.runConfig.args = '10';
              }
              // For Kafka tour
              if (self.projectIsGuide) {
                self.tourService.currentStep_TourSeven = 7;
              }

              if (self.tourService.currentStep_TourFour > -1) {
                self.tourService.currentStep_TourFour = 7;
              }
            };

            self.populateKafkaJobParameters = function () {
              self.runConfig.mainClass = 'io.hops.examples.spark.kafka.StreamingExample';
              var jobState = self.tourService.kafkaJobCreationState;
              if (angular.equals('producer', jobState)) {
                self.runConfig.args = 'producer';
              } else if (angular.equals('consumer', jobState)) {
                self.runConfig.args = "consumer /Projects/" +
                  self.projectName +"/Resources/Data";
              } else {
                self.runConfig.args = "Internal error, something went wrong. Select manually!"
              }
            };

            self.guideKafkaTopics = [];

            self.populateKafkaTopic = function () {
              self.accordion5.isOpen = true;
              self.accordion5.visible = true;

              self.kafkaSelected = true;
              self.getAllTopics(self.projectId).then(
                function(success) {
                  for (var i = 0; i < self.topics.length; i++) {
                    if (angular.equals(self.tourService.kafkaTopicName + "_"
                        + self.projectId, self.topics[i]['name'])) {
                      self.guideKafkaTopics.push(self.topics[i]);
                      break;
                    }

                  }
                }, function(error) {
                  console.log(">>> Something bad happened")
                }
              );
            };

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
              self.accordion6.visible = true; // Show job config
              self.accordion3.value = " - " + path; // Set file selection title
              self.accordion3.isOpen = false; //Close file selection
            };

            /**
             * Callback for when the job setup has been completed.
             * @returns {undefined}
             */
            self.jobDetailsFilledIn = function () {
              self.phase = 4;
            };

            /**
             * Callback for when the user selected a file.
             * @param {String} reason
             * @param {String} path
             * @returns {undefined}
             */
            self.onFileSelected = function (reason, path) {
              var filename = getFileName(path);
              switch (reason.toUpperCase()) {
                case "SPARK":
                case "PYSPARK":
                  self.sparkState.selectedJar = filename;
                  SparkService.inspectJar(self.projectId, path).then(
                          function (success) {
                            self.runConfig = success.data;
                            if(self.runConfig.type.toLowerCase() == "pyspark"){
                              self.jobtype = 4;
                            }
                            //Update the min/max spark executors based on 
                            //backend configuration 
                            if (typeof runConfig !== 'undefined') {
                              self.sliderOptions.options['floor'] = self.runConfig.
                                      minExecutors;
                              self.sliderOptions.options['ceil'] = self.runConfig.
                                      maxExecutors;
                            } else {
                              self.sliderOptions.options['floor'] = 1;
                              self.sliderOptions.options['ceil'] = 300;
                            }
                            self.mainFileSelected(filename);
                            // For Kafka tour
                            if (self.projectIsGuide) {
                              self.tourService.currentStep_TourSeven = 6;
                            }

                            if (self.tourService.currentStep_TourFour > -1) {
                              self.tourService.currentStep_TourFour = 6;
                            }

                          }, function (error) {
                    growl.error(error.data.errorMsg, {title: 'Error', ttl: 15000});
                  });
                  break;
                case "LIBRARY":
                  //Push the new library into the localresources array
                  self.localResources.push({
                    'name': filename,
                    'path': path,
                    'type': null,
                    'visibility': null,
                    'pattern': null
                  });
                  break;
                case "ADAM":
                  self.adamState.processparameter.value = path;
                  if (typeof runConfig != 'undefined') {
                    self.sliderOptions.options['floor'] = self.runConfig.minExecutors;
                    self.sliderOptions.options['ceil'] = self.runConfig.
                            maxExecutors;
                  } else {
                    self.sliderOptions.options['floor'] = 1;
                    self.sliderOptions.options['ceil'] = 300;
                  }
                  break;
                case "FLINK":
                  self.flinkState.selectedJar = filename;
                  FlinkService.inspectJar(self.projectId, path).then(
                          function (success) {
                            self.runConfig = success.data;
                            self.mainFileSelected(filename);
                          }, function (error) {
                    growl.error(error.data.errorMsg, {title: 'Error', ttl: 15000});
                  });
                  break;
              }
            };

            /**
             * Open a dialog for file selection.
             * @param {String} reason Goal for which the file is selected. (JobType or "LIBRARY").
             * @param {Object} parameter The Adam parameter to bind.
             * @returns {undefined}
             */
            this.selectFile = function (reason, parameter) {
              self.accordion6.visible = false;
              self.accordion6.isOpen = false;
              if (reason.toUpperCase() === "ADAM") {
                self.adamState.processparameter = parameter;
              }
              ModalService.selectFile('lg', self.selectFileRegexes[reason],
                      self.selectFileErrorMsgs["ADAM-FILE"]).then(
                      function (success) {
                        self.onFileSelected(reason, "hdfs://" + success);
                      }, function (error) {
                //The user changed their mind.
              });
            };
            /**
             * Open a dialog for directory selection.
             * @param {String} reason Goal for which the file is selected. (JobType or "LIBRARY").
             * @param {Object} parameter The Adam parameter to bind.
             * @returns {undefined}
             */
            this.selectDir = function (reason, parameter) {
              if (reason.toUpperCase() === "ADAM") {
                self.adamState.processparameter = parameter;
              }
              ModalService.selectDir('lg', self.selectFileRegexes[reason],
                      self.selectFileErrorMsgs["ADAM-FOLDER"]).then(
                      function (success) {
                        self.onFileSelected(reason, "hdfs://" + success);
                        if (reason.toUpperCase() === "ADAM") {
                          growl.info("Insert output file name", {title: 'Required', ttl: 5000});
                        }
                      }, function (error) {
                //The user changed their mind.
              });
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
             * Remove the given entry from the localResources list.
             * @param {type} lib
             * @returns {undefined}
             */
            this.removeLibrary = function (name) {
              var arlen = self.localResources.length;
              for (var i = 0; i < arlen; i++) {
                if (self.localResources[i].name === name) {
                  self.localResources.splice(i, 1);
                  return;
                }
              }
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
                "flinkState": self.flinkState,
                "accordion1": self.accordion1,
                "accordion2": self.accordion2,
                "accordion3": self.accordion3,
                "accordion4": self.accordion4,
                "accordion5": self.accordion5,
                "accordion6": self.accordion6
              };
              StorageService.store(self.newJobName, state);
            });
            /**
             * Init method: restore any previous state.
             * @returns {undefined}
             */
            var init = function () {
              var stored = StorageService.recover(self.newJobName);
              if (stored) {
                //Job information
                self.jobtype = stored.jobtype;
                self.jobname = stored.jobname;
                self.localResources = stored.localResources;
                self.phase = stored.phase;
                self.runConfig = stored.runConfig;
                if (self.runConfig) {
                  self.topics = [];
                  self.runConfig.schedule = null;
                  self.sliderOptions.options['floor'] = self.runConfig.minExecutors;
                  self.sliderOptions.options['ceil'] = self.runConfig.maxExecutors;
                  self.sliderOptions.min = self.runConfig.selectedMinExecutors;
                  self.sliderOptions.max = self.runConfig.selectedMaxExecutors;
                  //Load Kafka properties
                  if (typeof self.runConfig.kafka !== "undefined") {
                    self.kafkaSelected = true;
                    self.showAdvanced = self.runConfig.kafka.advanced;
                    if (typeof self.runConfig.kafka.consumergroups !== "undefined") {
                      self.groupsSelected = true;
                      self.consumerGroups = self.runConfig.kafka.consumergroups;
                    }
                    var storedTopics = self.runConfig.kafka.topics;
                    //Set Kafka topics is selected
                    KafkaService.getProjectAndSharedTopics(self.projectId).then(
                            function (success) {
                              var topics = success.data;
                              for (var i = 0; i < topics.length; i++) {
                                self.topics.push({name: topics[i]['name'], ticked: false});
                              }
                              if (typeof storedTopics !== "undefined") {
                                self.runConfig.kafka.topics = storedTopics;
                                //Set selected topics
                                //wait to fetch topics firsts
                                for (var i = 0; i < self.runConfig.kafka.topics.length; i++) {
                                  for (var z = 0; z < self.topics.length; z++) {
                                    if (self.topics[z]['name'] === self.runConfig.kafka.topics[i]['name']) {
                                      self.topics[z]['ticked'] = true;
                                      break;
                                    }
                                  }
                                }
                              }
                            }, function (error) {
                    });
                  }
                }
                if (self.jobtype === 1 || self.jobtype === 4) {
                  self.sparkState = stored.sparkState;
                } else if (self.jobtype === 2) {
                  self.adamState = stored.adamState;
                } else if (self.jobtype === 3) {
                  self.flinkState = stored.flinkState;
                }
                //GUI state
                self.accordion1 = stored.accordion1;
                self.accordion2 = stored.accordion2;
                self.accordion3 = stored.accordion3;
                self.accordion4 = stored.accordion4;
                self.accordion5 = stored.accordion5;
                self.accordion6 = stored.accordion6;
              }

              // Check if it's a guide project
              ProjectService.get({}, {'id': self.projectId}).$promise.then(
                function (success) {
                  self.projectName = success.projectName;
                  if (angular.equals(self.projectName.substr(0, 5), 'demo_')) {
                    self.tourService.currentStep_TourSeven = 0;
                    self.projectIsGuide = true;
                  }
                }, function (error) {
                  $location.path('/');
                });

              if (self.adamState.commandList === null) {
                self.getCommandList();
              }
            };

            init(); //Call upon create;
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
             * Creates a jobDetails object with the arguments typed by the user and send  
             * these attributes to the server. The server responds with the results from the 
             * heuristic search.
             * @returns {undefined}
             */
            this.autoConfig = function (filterValue) {
              self.isSpin = true;

              self.autoConfigResult = {};
              var jobDetails = {};
              jobDetails.className = self.runConfig.mainClass;
              jobDetails.selectedJar = self.sparkState.selectedJar;
              jobDetails.inputArgs = self.runConfig.args;
              jobDetails.jobType = self.getJobType();
              jobDetails.projectId = self.projectId;
              jobDetails.jobName = self.jobname;
              jobDetails.filter = filterValue;

              if (!angular.isUndefined(jobDetails.className) && !angular.isUndefined(jobDetails.inputArgs) &&
                      !angular.isUndefined(jobDetails.selectedJar) && !angular.isUndefined(jobDetails.jobType)) {

                self.configAlert = false;
                HistoryService.getHeuristics(jobDetails).then(
                        function (success) {
                          self.autoConfigResult = success.data;
                          console.log(self.autoConfigResult);
                        });
              } else {
                self.configAlert = true;
                self.isSpin = false;
              }
            };

            /**
             * Checks the value of the proposed configuration.
             * The function is used to initialized the checked radio button
             * @param {type} value
             * @returns {Boolean}
             */
            $scope.checkRadio = function (value) {
              if (value === "Minimal") {
                return true;
              } else
                return false;
            };

            /**
             * When the user changes configutaion (using the radio button) the 
             * runConfig values change.
             * @param {type} value
             * @returns {undefined}
             */
            $scope.selectConfig = function (value) {
              for (var i = 0; i < self.autoConfigResult.jobProposedConfig.length; i++) {
                var obj = self.autoConfigResult.jobProposedConfig[i];
                if (obj.configType === value) {
                  self.runConfig.amMemory = obj.amMemory;
                  self.runConfig.amVCores = obj.amVcores;
                  self.runConfig.amQueue = "default";
                  self.runConfig.numberOfExecutors = obj.numOfExecutors;
                  self.runConfig.executorCores = obj.executorCores;
                  self.runConfig.executorMemory = obj.executorMemory;
                }
              }
            };

          }]);


