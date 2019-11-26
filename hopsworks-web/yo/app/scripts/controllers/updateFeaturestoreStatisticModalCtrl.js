/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
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
 */

/**
 * Controller for the update-statistic-view
 */
angular.module('hopsWorksApp')
    .controller('updateFeaturestoreStatisticModalCtrl', ['$uibModalInstance', '$scope', 'FeaturestoreService',
        'StorageService', '$location', 'growl', 'projectId', 'featuregroup', 'trainingDataset',
        'projectName', 'featurestore', 'settings',
        function ($uibModalInstance, $scope, FeaturestoreService, StorageService, $location, growl,
                  projectId, featuregroup, trainingDataset, projectName, featurestore, settings) {

            /**
             * Initialize controller state
             */
            var self = this;
            //Controller Inputs
            self.projectId = projectId;
            self.featuregroup = featuregroup;
            self.trainingDataset = trainingDataset;
            self.projectName = projectName;
            self.featurestore = featurestore;
            self.settings=settings;
            self.newJobName = self.projectId + "_newjob";

            //Constants
            self.featurestoreUtil4jMainClass = self.settings.featurestoreUtil4jMainClass
            self.featurestoreUtilPythonMainClass = self.settings.featurestoreUtilPythonMainClass
            self.featurestoreUtil4JExecutable = self.settings.featurestoreUtil4jExecutable
            self.featurestoreUtilPythonExecutable = self.settings.featurestoreUtilPythonExecutable
            self.sparkJobType = "SPARK"
            self.pySparkJobType = "PYSPARK"


            /**
             * Closes the modal
             */
            self.close = function () {
                $uibModalInstance.dismiss('cancel');
            };

            /**
             * Initializes the statistics settings checkboxes
             */
            var init = function () {
                FeaturestoreService.getFeaturegroupMetadata(self.projectId, self.featurestore, self.featuregroup.id).then(
                    function (success) {
                        self.clusterAnalysis = success.data.clusterAnalysisEnabled;
                        self.featureCorrelations = success.data.featCorrEnabled;
                        self.descriptiveStats = success.data.descStatsEnabled;
                        self.featureHistograms = success.data.featHistEnabled;
                        self.statColumns = success.data.statisticColumns;
                    }, function (error) {
                        self.clusterAnalysis = true;
                        self.featureCorrelations = true;
                        self.descriptiveStats = true;
                        self.featureHistograms = true;
                        self.statisticColumns = [];
                        console.log('Failed to fetch statistics settings');
                    });
            }

            if(!self.trainingDataset) {
                init();
            } else {
                self.clusterAnalysis = true;
                self.featureCorrelations = true;
                self.descriptiveStats = true;
                self.featureHistograms = true;
                self.statisticColumns = [];
            }

            /**
             * Sets the feature correlations checkbox
             */
            self.setFeatureCorrelations = function () {
                if(self.featureCorrelations){
                    self.featureCorrelations = false;
                } else {
                    self.featureCorrelations = true;
                }
            }

            /**
             * Sets the descriptive stats checkbox
             */
            self.setDescriptiveStats = function () {
                if(self.descriptiveStats){
                    self.descriptiveStats = false;
                } else {
                    self.descriptiveStats = true;
                }
            }


            /**
             * Sets the feature histograms checkbox
             */
            self.setFeatureHistograms = function () {
                if(self.featureHistograms){
                    self.featureHistograms = false;
                } else {
                    self.featureHistograms = true;
                }
            }

            /**
             * Sets the cluster analysis checkbox
             */
            self.setClusterAnalysis = function () {
                if(self.clusterAnalysis){
                    self.clusterAnalysis = false;
                } else {
                    self.clusterAnalysis = true;
                }
            }

            /**
             * Helper function for redirecting to another project page
             *
             * @param serviceName project page
             */
            self.goToUrl = function (serviceName) {
                $location.path('project/' + self.projectId + '/' + serviceName);
            };

            /**
             * Configures the job for updating the statistics
             *
             * @param jobName name of the job
             * @param argsPath HDFS path to input arguments
             */
            self.setupUpdateStatsJob = function (jobName, argsPath) {
                var path = self.featurestoreUtil4JExecutable
                var mainClass = self.featurestoreUtil4jMainClass
                var jobType = self.sparkJobType
                var runConfig = {
                    type: "sparkJobConfiguration",
                    appName: jobName,
                    amQueue: "default",
                    amMemory: 4000,
                    amVCores: 1,
                    jobType: jobType,
                    appPath: path,
                    mainClass: mainClass,
                    args: "--input " + argsPath,
                    "spark.blacklist.enabled": false,
                    "spark.dynamicAllocation.enabled": true,
                    "spark.dynamicAllocation.initialExecutors": 1,
                    "spark.dynamicAllocation.maxExecutors": 10,
                    "spark.dynamicAllocation.minExecutors": 1,
                    "spark.executor.cores": 1,
                    "spark.executor.gpus": 0,
                    "spark.executor.instances": 1,
                    "spark.executor.memory": 4000,
                    "spark.tensorflow.num.ps": 0
                }
                return runConfig
            }

            /**
             * Configures the JSON input to the job for updating the statistics
             *
             * @param fileName name of the file to save the JSON
             * @param operation the operation name
             * @returns the configured JSON
             */
            self.setupJobArgs = function (fileName, operation) {
                var argsJson = {
                    "operation": operation,
                    "featurestore": self.featurestore.featurestoreName,
                    "version": self.featuregroup.version,
                    "fileName": fileName,
                    "descriptiveStats": self.descriptiveStats,
                    "featureCorrelation": self.featureCorrelations,
                    "clusterAnalysis": self.clusterAnalysis,
                    "featureHistograms": self.featureHistograms,
                    "statColumns": self.statColumns
                }
                if(self.trainingDataset){
                    argsJson["trainingDataset"] = self.featuregroup.name
                } else {
                    argsJson["featuregroup"] = self.featuregroup.name
                }
                return argsJson
            }

            /**
             * Setup jobState for redirecting to 'newjob' page
             *
             * @param runConfig the job runConfig
             * @returns the jobState
             */
            self.setupJobState = function (runConfig) {
                var jobState = {}
                jobState.accordion1 = {//Contains the job name
                    "isOpen": false,
                    "visible": true,
                    "value": "",
                    "title": "Job name - " + runConfig.appName
                };
                jobState.accordion2 = {//Contains the job type
                    "isOpen": false,
                    "visible": true,
                    "value": "",
                    "title": "Job type - " + runConfig.jobType
                };
                if (runConfig.jobType === self.sparkJobType) {
                    jobState.accordion3 = {// Contains the main execution file (jar, workflow,...)
                        "isOpen": false,
                        "visible": false,
                        "value": "",
                        "title": "App file (.jar, .py or .ipynb) - " + runConfig.appPath
                    };
                    jobState.jobtype = 1
                }
                if (runConfig.jobType === self.pySparkJobType) {
                    jobState.accordion3 = {// Contains the main execution file (jar, workflow,...)
                        "isOpen": false,
                        "visible": false,
                        "value": "",
                        "title": "App file (.py or .ipynb) - " + runConfig.appPath
                    };
                    jobState.jobtype = 2
                }
                jobState.accordion4 = {// Contains the job setup (main class, input variables,...)
                    "isOpen": false,
                    "visible": true,
                    "value": "",
                    "title": "Job details"
                };
                jobState.accordion5 = {//Contains the configuration and creation
                    "isOpen": true,
                    "visible": true,
                    "value": "",
                    "title": "Configure and create"
                };
                jobState.phase = 5
                jobState.jobname = runConfig.appName
                jobState.runConfig = runConfig
                jobState.sparkState = {
                    "selectedJar": runConfig.appPath
                }
                return jobState
            }

            /**
             * Creates a spark job for updating the featuegroup statistics using the featurestore_util.py script
             */
            self.updateStatistics = function () {
                self.working = true;
                var jobName = ""
                var operation = ""
                if(!self.trainingDataset){
                    jobName = "update_featuregroup_statistics_" + self.featuregroup.name + "_" + new Date().getTime()
                    operation = 'update_fg_stats'
                    var settingsJson = {
                        "clusterAnalysisEnabled": self.clusterAnalysis,
                        "featCorrEnabled": self.featureCorrelations,
                        "descStatsEnabled": self.descriptiveStats,
                        "featHistEnabled": self.featureHistograms,
                        "featuregroupType": self.featuregroup.featuregroupType
                    }
                    FeaturestoreService.updateFeaturegroupStatsSettings(
                        self.projectId, self.featurestore, self.featuregroup.id, settingsJson).then(
                        function (success) {
                            growl.success("Featuregroup statistics settings updated", {title: 'Success', ttl: 15000});
                            writeUtilArgstoHdfs(jobName, operation);
                        }, function (error) {
                            growl.error(error.data.errorMsg, {
                                title: 'Failed to update featuregroup statistics settings',
                                ttl: 15000
                            });
                            self.working = false;
                        }
                    )
                } else {
                    jobName = "update_trainingdataset_statistics_" + self.featuregroup.name + "_" + new Date().getTime()
                    operation = 'update_td_stats'
                    writeUtilArgstoHdfs(jobName, operation);
                }
            }

            /**
             * Writes the jobArgs to Hdfs with a given jobName and operation
             *
             * @param jobName
             * @param operation
             */
            var writeUtilArgstoHdfs = function (jobName, operation) {
                    var utilArgs = self.setupJobArgs(jobName + "_args.json", operation)
                    FeaturestoreService.writeUtilArgstoHdfs(self.projectId, utilArgs).then(
                        function (success) {
                            growl.success("Featurestore util args written to HDFS", {title: 'Success', ttl: 1000});
                            var hdfsPath = success.data.successMessage
                            var runConfig = self.setupUpdateStatsJob(jobName, hdfsPath)
                            var jobState = self.setupJobState(runConfig)
                            StorageService.store(self.newJobName, jobState);
                            $uibModalInstance.close(success);
                            self.goToUrl("newjob")
                        }, function (error) {
                            growl.error(error.data.errorMsg, {
                                title: 'Failed to setup featurestore util job arguments',
                                ttl: 15000
                            });
                            self.working = false;
                        });
                    growl.info("Settings up job arguments... wait", {title: 'Creating', ttl: 1000})
            }

        }]);
