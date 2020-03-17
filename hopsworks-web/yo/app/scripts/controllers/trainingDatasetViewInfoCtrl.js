/*
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
 */

/**
 * Controller for the Training Dataset-Info view
 */
angular.module('hopsWorksApp')
    .controller('trainingDatasetViewInfoCtrl', ['$scope', 'ProjectService',
        'JobService', 'ModalService', 'StorageService', '$location', 'growl',
        function ($scope, ProjectService, JobService, ModalService, StorageService, $location, growl) {

            /**
             * Initialize controller state
             */
            var self = this;
            //Controller Inputs
            self.tgState = false;
            self.projectId = null;
            self.selectedTrainingDataset = null;
            self.trainingDatasets = null;
            self.featurestore = null;
            self.settings = null;
            //State
            self.sizeWorking = false;
            self.size = "Not fetched"
            self.pythonCode = ""
            self.scalaCode = ""

            /**
             * Get the Python API code to retrieve the featuregroup
             */
            self.getPythonCode = function () {
                var codeStr = "from hops import featurestore\n"
                codeStr = codeStr + "featurestore.get_training_dataset_path('" + self.selectedTrainingDataset.name + "')"
                return codeStr
            };

            /**
             * Get the Scala API code to retrieve the featuregroup
             */
            self.getScalaCode = function () {
                var codeStr = "import io.hops.util.Hops\n"
                codeStr = codeStr + "Hops.getTrainingDatasetPath(\"" + self.selectedTrainingDataset.name + "\").read()"
                return codeStr
            };

            /**
             * Called when the launch-job button is pressed
             */
            self.launchJob = function (jobName) {
                JobService.setJobFilter(jobName);
                self.goToUrl("jobs")
            };

            /**
             * Check if a row is a regular one or need special rendering
             */
            self.isRegularRow = function(property) {
                if (property == "API Retrieval Code" || property == "Job" || property == "Last Computed"){
                    return false
                }
                return true
            }

            self.toggle = function(selectedTrainingDataset) {
                if (self.selectedTrainingDataset &&
                          (self.selectedTrainingDataset.id === selectedTrainingDataset.id) &&
                           self.tgState === true) {
                    self.tgState = false;
                } else {
                    self.tgState = true;
                }
            }

            /**
             * Initialization function
             */
            self.view = function (featurestoreCtrl, trainingDatasets, activeVersion, toggle) {

                if(toggle) {
                    self.toggle(trainingDatasets.versionToGroups[activeVersion]);
                }

                self.selectedTrainingDataset = trainingDatasets.versionToGroups[activeVersion]

                self.projectId = featurestoreCtrl.projectId;
                self.projectName = featurestoreCtrl.projectName;
                self.featurestore = featurestoreCtrl.featurestore;
                self.trainingDatasets = trainingDatasets;
                self.activeVersion = activeVersion;
                self.settings = featurestoreCtrl.settings;

                self.hopsfsTrainingDatasetType = self.settings.hopsfsTrainingDatasetType
                self.externalTrainingDatasetType = self.settings.externalTrainingDatasetType

                self.pythonCode = self.getPythonCode();
                self.scalaCode = self.getScalaCode();
                self.fetchSize()
            };

            /**
             * Convert bytes into bytes + suitable unit (e.g KB, MB, GB etc)
             *
             * @param fileSizeInBytes the raw byte number
             */
            self.sizeOnDisk = function (fileSizeInBytes) {
                return convertSize(fileSizeInBytes);
            };

            /**
             * Send async request to hopsworks to calculate the inode size of the training dataset
             * this can potentially be a long running operation if the directory is deeply nested
             */
            self.fetchSize = function () {
                if(self.selectedTrainingDataset.trainingDatasetType == self.externalTrainingDatasetType){
                    return
                }
                if(self.sizeWorking){
                    return
                }
                self.sizeWorking = true
                var request = {type: "inode", inodeId: self.selectedTrainingDataset.inodeId};
                ProjectService.getMoreInodeInfo(request).$promise.then(function (success) {
                    self.sizeWorking = false;
                    self.size = self.sizeOnDisk(success.size)
                }, function (error) {
                    growl.error(error.data.errorMsg, {title: 'Failed to fetch training dataset size', ttl: 5000});
                    self.sizeWorking = false;
                });
            };

            /**
             * Helper function for redirecting to another project page
             *
             * @param serviceName project page
             */
            self.goToUrl = function (serviceName) {
                $location.path('project/' + self.projectId + '/' + serviceName);
            };

            /**
             * Called when the view-training-dataset-statistics button is pressed
             *
             * @param trainingDataset
             */
            self.viewTrainingDatasetStatistics = function () {
                ModalService.viewTrainingDatasetStatistics('lg', self.projectId, self.selectedTrainingDataset, self.projectName,
                    self.featurestore, self.settings).then(
                    function (success) {
                    }, function (error) {
                    });
            };

            /**
             * Called when the delete-trainingDataset-button is pressed
             *
             * @param trainingDataset
             */
            self.deleteTrainingDataset = function (featurestoreCtrl) {
                ModalService.confirm('md', 'Are you sure?',
                    'Are you sure that you want to delete version ' + self.selectedTrainingDataset.version + ' of the ' + self.selectedTrainingDataset.name + ' training dataset? ' +
                    'This action will delete the data and metadata and can not be undone.')
                    .then(function (success) {
                        FeaturestoreService.deleteTrainingDataset(self.projectId, self.featurestore, self.selectedTrainingDataset.id).then(
                            function (success) {
                                self.tgState = false;
                                featurestoreCtrl.getTrainingDatasets(self.featurestore)
                                growl.success("Training Dataset deleted", {title: 'Success', ttl: 2000});
                            },
                            function (error) {
                                growl.error(error.data.errorMsg, {
                                    title: 'Failed to delete the training dataset',
                                    ttl: 15000
                                });
                            });
                        growl.info("Deleting training dataset...", {title: 'Deleting', ttl: 2000})
                    }, function (error) {});
            };

            /**
             * Called when the increment-version-trainingDataset-button is pressed
             *
             * @param trainingDatasets list of featuregroup versions
             * @param versions list
             */
            self.newTrainingDatasetVersion = function (featurestoreCtrl) {
                StorageService.store("trainingdataset_operation", "NEW_VERSION");
                StorageService.store(self.projectId + "_fgFeatures", featurestoreCtrl.fgFeatures);
                StorageService.store(self.projectId + "_trainingDataset", self.selectedTrainingDataset);
                self.goToUrl("newtrainingdataset")
            };

            /**
             * Shows the page for updating an existing training dataset.
             *
             * @param trainingDataset
             */
            self.updateTrainingDataset = function (featurestoreCtrl) {
                StorageService.store("trainingdataset_operation", "UPDATE");
                StorageService.store(self.projectId + "_fgFeatures", featurestoreCtrl.fgFeatures);
                StorageService.store(self.projectId + "_trainingDataset", self.selectedTrainingDataset);
                self.goToUrl("newtrainingdataset")
            };

        }]);

