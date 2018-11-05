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
        .controller('NewJobCtrl', ['$routeParams', 'growl', 'JobService',
          '$location', 'ModalService', 'StorageService', '$scope', 'SparkService',
          'FlinkService', 'TourService', 'HistoryService', 'KafkaService', 'ProjectService', 'PythonDepsService', '$timeout',
          function ($routeParams, growl, JobService,
                  $location, ModalService, StorageService, $scope, SparkService, FlinkService, TourService,
                  HistoryService, KafkaService, ProjectService, PythonDepsService, $timeout) {

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
            self.outputTopics = [];
            self.kafkaSelected = false;
            self.settingsSelected = false;
            self.consumerGroups = [{id: 'group1', "name": "default"}];
            self.groupsSelected = false;
            self.showAdvanced = false;
            self.selectedTopics = [];
            self.projectName = "";
            self.tfOnSpark = false;
            self.getAllTopics = function () {
              if (self.kafkaSelected) {
                if (typeof self.runConfig.kafka !== "undefined" &&
                        typeof self.runConfig.kafka.topics !== "undefined") {
                  self.selectedTopics = self.runConfig.kafka.topics;
                }

                return KafkaService.getProjectAndSharedTopics(self.projectId)
                        .then(
                                function (success) {
                                  self.topics = [];
                                  var topics = success.data;
                                  for (var i = 0; i < topics.length; i++) {
                                    if (self.selectedTopics !== "undefined" && self.selectedTopics.length > 0) {
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
                                if (typeof error.data.usrMsg !== 'undefined') {
                                    growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 5000});
                                } else {
                                    growl.error("", {title: error.data.errorMsg, ttl: 5000});
                                }
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
            self.toggleSettings = function () {
              self.settingsSelected = !self.settingsSelected;
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
              "PYSPARK": /.py\b/
            };
            self.selectFileErrorMsgs = {
              "SPARK": "Please select a JAR file.",
              "FLINK": "Please select a JAR file.",
              "PYSPARK": "Please select a file."
            };

            //Create variables for user-entered information
            self.jobtype; //Will hold the selection of which job to create.
            self.jobname; //Will hold the name of the job

            self.localResources = [];//Will hold extra libraries

            self.newJobName = self.projectId + "_newjob";

            self.phase = 0; //The phase of creation we are in.
            self.runConfig; //Will hold the job configuration
            self.sliderVisible = false;

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
              self.runConfig.numberOfGpusPerExecutor = 0;
            };

            self.dynExecChangeListener = function() {
                self.setInitExecs();
            };

            self.sliderOptions = {
              min: 1,
              max: 10,
              options: {
                floor: 0,
                ceil: 1500,
                onChange: self.dynExecChangeListener
              },
              getPointerColor: function (value) {
                return '#4b91ea';
              }
            };

            self.sparkState = {//Will hold spark-specific state
              "selectedJar": null //The path to the selected jar
            };
            self.flinkState = {//Will hold flink-specific state
              "selectedJar": null //The path to the selected jar
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
              "title": "Configure and create"
            };

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
                "accordions": [self.accordion1, self.accordion2, self.accordion3, self.accordion4, self.accordion5]
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
                self.accordion1 = self.undoneState.accordions[0];
                self.accordion2 = self.undoneState.accordions[1];
                self.accordion3 = self.undoneState.accordions[2];
                self.accordion4 = self.undoneState.accordions[3];
                self.accordion5 = self.undoneState.accordions[4];
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




            var handleFileSelect = function (evt) {
              console.log(evt.target.files[0]);
              var file = evt.target.files[0]; // FileList object

              var fileType = file.type;
              if (!angular.equals(fileType, "application/json")) {
                growl.error("Not a valid file type", {title: 'File not JSON', ttl: 7000})
                return;
              }
              // files is a FileList of File objects. List some properties.
              var reader = new FileReader();

              reader.onerror = function () {
                console.log("Error reading file: " + reader.error.code);
                growl.error("Error reading file", {title: reader.error.code, ttl: 7000})
              };

              reader.onload = (function (theFile) {
                return function (e) {
                  // Render thumbnail.
                  var content = e.target.result;
                  jobConfigFileImported(content);
                };
              })(file);

              reader.readAsText(file);
            }
            document.getElementById('jobConfigFile').addEventListener('change', handleFileSelect, false);

            var jobConfigFileImported = function (config) {
              try {
                var jobConfig = angular.fromJson(config);
                JobService.createNewJob(self.projectId, jobConfig.type, jobConfig.config).then(
                        function (success) {
                          $location.path('project/' + self.projectId + '/jobs');
                          self.removed = true;
                        }, function (error) {
                        if (typeof error.data.usrMsg !== 'undefined') {
                            growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 8000});
                        } else {
                            growl.error("", {title: error.data.errorMsg, ttl: 8000});
                        }
                });
              } catch (e) {
                growl.error("Error parsing JSON file", {title: 'Error parsing job configuration file', ttl: 7000});
              }
            };



            /**
             * Create the job.
             * @returns {undefined}
             */
            self.createJob = function () {
              if (self.kafkaSelected) {
                if (self.projectIsGuide) {
                  //If it is the first time the job is created topics will be empty
                  if (self.runConfig.kafka.topics.length === 0) {
                    self.runConfig.kafka.topics = self.guideKafkaTopics;
                  }
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
              if (self.getJobType() === "SPARK" || self.getJobType() === "PYSPARK") {
                self.runConfig.selectedMinExecutors = self.sliderOptions.min;
                self.runConfig.selectedMaxExecutors = self.sliderOptions.max;
              }
              if (self.getJobType() === "SPARK" || self.getJobType() === "FLINK") {
                if (typeof self.runConfig.mainClass === 'undefined' || self.runConfig.mainClass === "") {
                  growl.warning("Please specify main class first", {ttl: 5000});
                  return;
                }
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
                      if (typeof error.data.usrMsg !== 'undefined') {
                          growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 8000});
                      } else {
                          growl.error("", {title: error.data.errorMsg, ttl: 8000});
                      }
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
                  self.jobname = "Job-" + Math.round(new Date().getTime() / 1000);
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
              if ((typeof self.jobname === 'undefined' || self.jobname === '')) {
                if (angular.equals('producer', jobState)) {
                  self.jobname = "KafkaDemoProducer";
                } else {
                  self.jobname = "KafkaDemoConsumer";
                }
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
                  self.accordion3.title = "App file (.jar, .py)";
                  self.accordion4.title = "Job details";
                  selectedType = "Spark";
                  break;
                case 2:
                  self.accordion3.title = "App file (.py)";
                  self.accordion4.title = "Job details";
                  selectedType = "PySpark";
                  break;
                case 3:
                  self.accordion3.title = "App file (.jar)";
                  self.accordion4.title = "Job details";
                  selectedType = "Flink";
                  break;
                default:
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
                  return "PYSPARK";
                case 3:
                  return "FLINK";
                default:
                  return null;
              }
            };

            /**
             * Used by tour.
             * @param {type} jobType
             * @returns {undefined}
             */
            self.setTourJobType = function (jobType) {
              self.jobtype = jobType;
              self.jobTypeChosen();
            };

            self.chooseParameters = function () {
              if (self.jobtype === 1 && self.projectIsGuide &&
                      (typeof self.runConfig.mainClass === 'undefined' || self.runConfig.mainClass === '')) {
                self.runConfig.mainClass = 'org.apache.spark.examples.SparkPi';
              }
              if (self.jobtype === 1 && self.projectIsGuide &&
                      (typeof self.runConfig.args === 'undefined' || self.runConfig.args === '')) {
                self.runConfig.args = '10';
              }
              // For Kafka tour
              if (self.projectIsGuide) {
                self.tourService.currentStep_TourSeven = 7;
              }
              if (self.jobtype === 6 && !self.runConfig.args) {
                self.runConfig.args = '--base_path hdfs://default/Projects/' + self.projectName + '/TestJob --images tfr/train --format tfr --mode train --model mnist_model';
                self.runConfig.numOfPs = 1;
              }

              if (self.tourService.currentStep_TourFour > -1) {
                self.tourService.currentStep_TourFour = 7;
              }
            };

            self.populateKafkaJobParameters = function () {
              self.runConfig.mainClass = 'io.hops.examples.spark.kafka.StructuredStreamingKafka';
              var jobState = self.tourService.kafkaJobCreationState;
              if (angular.equals('producer', jobState)) {
                self.runConfig.args = 'producer';
              } else if (angular.equals('consumer', jobState)) {
                self.runConfig.args = "consumer";
              } else {
                self.runConfig.args = "Internal error, something went wrong. Select manually!";
              }
            };

            self.guideKafkaTopics = [];

            self.populateKafkaTopic = function () {
              var tipsEnabled = StorageService.get("hopsworks-showtourtips");
              if (tipsEnabled) {
                self.accordion5.isOpen = true;
                self.accordion5.visible = true;

                self.kafkaSelected = true;
                self.getAllTopics(self.projectId).then(
                        function (success) {
                          for (var i = 0; i < self.topics.length; i++) {
                            if (angular.equals(self.tourService.kafkaTopicName + "_"
                                    + self.projectId, self.topics[i]['name'])) {
                              self.guideKafkaTopics.push(self.topics[i]);
                              break;
                            }

                          }
                        }, function (error) {
                  console.log(">>> Something bad happened:" + error.data.errorMsg);
                }
                );
              }
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

              if (reason.toUpperCase() === "PYSPARK") {
                PythonDepsService.enabled(self.projectId).then(
                    function (success) {
                    },
                    function (error) {
                      self.jobtype = 0;
                      growl.error("You need to enable Python before running this job.", {title: 'Error - Python not Enabled Yet.', ttl: 15000});
                });
              }


              switch (reason.toUpperCase()) {
                case "SPARK":
                case "PYSPARK":
                  self.sparkState.selectedJar = filename;
                  SparkService.inspectJar(self.projectId, path).then(
                          function (success) {
                            self.runConfig = success.data;

                            if (self.runConfig.appPath.toLowerCase().endsWith(".py")) {
                              self.jobtype = 2;
                            } else {
                              self.jobtype = 1;
                            }
                            //Update the min/max spark executors based on
                            //backend configuration
                            if (typeof self.runConfig !== 'undefined') {
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
                          if (typeof error.data.usrMsg !== 'undefined') {
                              growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 8000});
                          } else {
                              growl.error("", {title: error.data.errorMsg, ttl: 8000});
                          }
                  });
                  break;
                case "LIBRARY":
                  //Push the new library into the localresources array
                  var libType = 'file';
                  if (path.endsWith(".zip") || path.endsWith(".tar") || path.endsWith(".gz")) {
                    libType = 'archive';
                  }
                  self.localResources.push({
                    'name': filename,
                    'path': path,
                    'type': libType,
                    'visibility': 'application',
                    'pattern': null
                  });
                  break;
                case "FLINK":
                  self.flinkState.selectedJar = filename;
                  FlinkService.inspectJar(self.projectId, path).then(
                          function (success) {
                            self.runConfig = success.data;
                            self.mainFileSelected(filename);
                          }, function (error) {
                          if (typeof error.data.usrMsg !== 'undefined') {
                              growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 8000});
                          } else {
                              growl.error("", {title: error.data.errorMsg, ttl: 8000});
                          }
                  });
                  break;
                default:
                  break;
              }
            };

            /**
             * Open a dialog for file selection.
             * @param {String} reason Goal for which the file is selected. (JobType or "LIBRARY").
             * @param {Object} parameter The  parameter to bind.
             * @returns {undefined}
             */
            this.selectFile = function (reason, parameter) {
              ModalService.selectFile('lg', self.selectFileRegexes[reason],
                      self.selectFileErrorMsgs["PYSPARK"]).then(
                      function (success) {
                        self.onFileSelected(reason, "hdfs://" + success);
                      }, function (error) {
                //The user changed their mind.
              });
            };
            /**
             * Open a dialog for directory selection.
             * @param {String} reason Goal for which the file is selected. (JobType or "LIBRARY").
             * @param {Object} parameter The parameter to bind.
             * @returns {undefined}
             */
            this.selectDir = function (reason, parameter) {
              ModalService.selectDir('lg', self.selectFileRegexes[reason],
                      self.selectFileErrorMsgs["PYSPARK"]).then(
                      function (success) {
                        self.onFileSelected(reason, "hdfs://" + success);
                      }, function (error) {
                //The user changed their mind.
              });
            };


            /**
             * Remove the given entry from the localResources list.
             * @param {type} name
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
                "flinkState": self.flinkState,
                "accordion1": self.accordion1,
                "accordion2": self.accordion2,
                "accordion3": self.accordion3,
                "accordion4": self.accordion4,
                "accordion5": self.accordion5,
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
                if (typeof self.jobname !== "undefined") {
                  self.jobname = self.jobname + "." + Math.floor(Math.random() * 10);
                  stored.accordion1.value = " - " + self.jobname;
                }
                self.localResources = stored.runConfig.localResources;
                if (typeof self.localResources === "undefined") {
                  self.localResources = [];
                }

                self.phase = stored.phase;
                self.runConfig = stored.runConfig;
                if (self.runConfig) {
                  self.topics = [];
                  self.runConfig.schedule = null;
                  if (typeof self.runConfig.minExecutors !== "undefined") {
                    self.sliderOptions.options['floor'] = self.runConfig.minExecutors;
                  }
                  if (typeof self.sliderOptions.options['ceil'] !== "undefined") {
                    self.runConfig.maxExecutors;
                  }
                  if (typeof self.runConfig.selectedMinExecutors === "undefined") {
                    self.runConfig.selectedMinExecutors = self.sliderOptions.min;
                  } else {
                    self.sliderOptions.min = self.runConfig.selectedMinExecutors;
                  }
                  if (typeof self.runConfig.selectedMaxExecutors === "undefined") {
                    self.runConfig.selectedMaxExecutors = self.sliderOptions.max;
                  } else {
                    self.sliderOptions.max = self.runConfig.selectedMaxExecutors;
                  }
                  //Load Kafka properties
                  if (typeof self.runConfig.kafka !== "undefined" && self.runConfig.kafka.topics.length > 0) {
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
                                self.guideKafkaTopics = storedTopics;
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
                      console.log("Error during job init:" + error.data.errorMsg);
                    });
                  }
                }
                if (self.jobtype === 1 || self.jobtype === 2) {
                  self.sparkState = stored.sparkState;
                } else if (self.jobtype === 3) {
                  self.flinkState = stored.flinkState;
                }
                //GUI state
                self.accordion1 = stored.accordion1;
                self.accordion2 = stored.accordion2;
                self.accordion3 = stored.accordion3;
                self.accordion4 = stored.accordion4;
                self.accordion5 = stored.accordion5;
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

            };

            init(); //Call upon create;

            /**
             * Creates a jobDetails object with the arguments typed by the user and send
             * these attributes to the server. The server responds with the results from the
             * heuristic search.
             * @param {type} filterValue
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

          }]);
