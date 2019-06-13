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
    .controller('updateFeaturestoreStatisticModalCtrl', ['$uibModalInstance', '$scope', 'FeaturestoreService', 'JobService',
        '$location', 'growl', 'projectId', 'featuregroup', 'trainingDataset', 'projectName', 'featurestore',
        function ($uibModalInstance, $scope, FeaturestoreService, JobService, $location, growl, projectId, featuregroup,
                  trainingDataset, projectName, featurestore) {

            /**
             * Initialize controller state
             */
            var self = this;
            self.projectId = projectId;
            self.featuregroup = featuregroup;
            self.trainingDataset = trainingDataset
            self.clusterAnalysis = true
            self.featureCorrelations = true
            self.descriptiveStats = true
            self.featureHistograms = true
            self.projectName = projectName;
            self.featurestore = featurestore;


            /**
             * Closes the modal
             */
            self.close = function () {
                $uibModalInstance.dismiss('cancel');
            };

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
             * Creates a spark job for updating the featuegroup statistics using the featurestore_util.py script
             */
            self.updateStatistics = function () {
                var path = "hdfs:///Projects/" + self.projectName + "/Resources/hops-examples-featurestore-util4j-1.0.0-SNAPSHOT.jar"
                var mainClass = "io.hops.examples.featurestore_util4j.Main"
                var jobType = "SPARK"
                var cmdArgs = ""
                var jobName = ""
                if(!self.trainingDataset){
                    jobName = "update_featuregroup_statistics_" + self.featuregroup.name + "_" + new Date().getTime()
                    cmdArgs = cmdArgs + "--featuregroup '" + self.featuregroup.name + "' "
                    cmdArgs = cmdArgs + "--operation 'update_fg_stats' "
                } else {
                    jobName = "update_trainingdataset_statistics_" + self.featuregroup.name + "_" + new Date().getTime()
                    cmdArgs = cmdArgs + "--trainingdataset '" + self.featuregroup.name + "' "
                    cmdArgs = cmdArgs + "--operation 'update_td_stats' "
                }
                if(self.descriptiveStats){
                    cmdArgs = cmdArgs + "--descriptivestats "
                }
                if(self.featureCorrelations) {
                    cmdArgs = cmdArgs + "--featurecorrelation "
                }
                if(self.clusterAnalysis) {
                    cmdArgs = cmdArgs + "--clusteranalysis "
                }
                if(self.featureHistograms) {
                    cmdArgs = cmdArgs + "--featurehistograms "
                }
                cmdArgs = cmdArgs + "--version '" + self.featuregroup.version + "' "
                cmdArgs = cmdArgs + "--featurestore '" + self.featurestore.featurestoreName + "' ";
                var runConfig = {
                    type: "sparkJobConfiguration",
                    appName: jobName,
                    amQueue: "default",
                    amMemory: 4000,
                    amVCores: 1,
                    jobType: jobType,
                    appPath: path,
                    mainClass: mainClass,
                    args: cmdArgs,
                    "spark.blacklist.enabled": false,
                    "spark.dynamicAllocation.enabled": true,
                    "spark.dynamicAllocation.initialExecutors": 1,
                    "spark.dynamicAllocation.maxExecutors": 10,
                    "spark.dynamicAllocation.minExecutors": 1,
                    "spark.executor.cores": 1,
                    "spark.executor.gpus": 0,
                    "spark.executor.instances": 1,
                    "spark.executor.memory": 4000,
                    "spark.tensorflow.num.ps": 0,
                }
                JobService.putJob(self.projectId, runConfig).then(
                    function (success) {
                        self.working = false;
                        JobService.setJobFilter(jobName);
                        $uibModalInstance.close(success)
                        self.goToUrl("jobs")
                        growl.success("Spark job for Updating the statistics configured", {title: 'Success', ttl: 1000});
                    }, function (error) {
                        growl.error(error.data.errorMsg, {title: 'Failed to configure spark job for updating the' +
                        ' statistics', ttl: 15000});
                        self.working = false;
                    });
            }

        }]);
