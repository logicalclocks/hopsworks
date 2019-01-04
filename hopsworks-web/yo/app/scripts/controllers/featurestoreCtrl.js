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
 * Controller for the featurestore page.
 */
'use strict';

angular.module('hopsWorksApp')
    .controller('featurestoreCtrl', ['$scope', '$routeParams', 'growl', 'FeaturestoreService', '$location', '$interval', '$mdSidenav', 'ModalService',
        'JobService', 'ProjectService',
        function ($scope, $routeParams, growl, FeaturestoreService, $location, $interval, $mdSidenav, ModalService, JobService, ProjectService) {


            /**
             * Initialize controller state
             */
            var self = this;
            self.projectId = $routeParams.projectID;
            self.featurestores = [];
            self.features = [];
            self.trainingDatasets = [];
            self.featuregroups = [];
            self.showFeaturesBool = -1;
            self.showFeaturegroupsBool = 1;
            self.showTrainingDatasetsBool = -1;
            self.jobs = [];
            self.pageSize = 10;
            self.featureSortKey = 'name'
            self.featuregroupSortKey = 'name'
            self.trainingDatasetSortKey = 'name'
            self.reverse = false
            self.fgFilter = "";
            self.fFilter = "";
            self.dFilter = "";
            self.firstPull = false;
            self.featuregroupsDictList = [];
            self.trainingDatasetsDictList = [];
            self.loading = false;
            self.loadingText = "";
            self.featuregroupsLoaded = false
            self.trainingDatasetsLoaded = false

            /**
             * Called when clicking the sort-arrow in the UI
             *
             * @param keyname
             */
            self.sort = function (keyname) {
                self.sortKey = keyname;   //set the sortKey to the param passed
                self.reverse = !self.reverse; //if true make it false and vice versa
            };

            /**
             * Function to start the loading screen
             *
             * @param label the text to show to the user while loading
             */
            self.startLoading = function (label) {
                self.loading = true;
                self.loadingText = label;
            };

            /**
             * Function to stop the loading screen
             */
            self.stopLoading = function () {
                if(self.featuregroupsLoaded && self.trainingDatasetsLoaded){
                    self.loading = false;
                    self.loadingText = "";
                }
            };

            /**
             * Shows the Modal for creating new feature groups through the UI
             */
            self.showCreateFeaturegroupForm = function () {
                ModalService.createFeaturegroup('lg', self.projectId, $scope.selected.value, self.jobs, self.featuregroups)
                    .then(
                        function (success) {
                            self.getFeaturegroups($scope.selected.value)
                        }, function (error) {
                            //The user changed their mind.
                        });
            };

            /**
             * Shows the Modal for creating new training datasets through the UI
             */
            self.showCreateTrainingDatasetForm = function () {
                ModalService.createTrainingDataset('lg', self.projectId, $scope.selected.value, self.jobs, self.trainingDatasets)
                    .then(
                        function (success) {
                            self.getTrainingDatasets($scope.selected.value)
                        }, function (error) {
                        });
            };

            /**
             * Retrieves a list of all featurestores for the project from the backend
             */
            self.getFeaturestores = function () {
                FeaturestoreService.getFeaturestores(self.projectId).then(
                    function (success) {
                        self.featurestores = success.data
                        if (!self.firstPull) {
                            $scope.selected = {value: self.featurestores[0]}
                            self.getTrainingDatasets($scope.selected.value)
                            self.getFeaturegroups($scope.selected.value)
                            self.firstPull = true
                        }
                    },
                    function (error) {
                        growl.error(error.data.errorMsg, {
                            title: 'Failed to fetch list of featurestores',
                            ttl: 15000
                        });
                    }
                );
            }

            /**
             * Shows the modal for updating an existing feature group.
             *
             * @param featuregroup
             */
            self.updateFeaturegroup = function (featuregroup) {
                ModalService.updateFeaturegroup('lg', self.projectId, featuregroup, $scope.selected.value, self.jobs, self.trainingDatasets)
                    .then(
                        function (success) {
                            self.getFeaturegroups($scope.selected.value)
                            self.showFeaturegroups()
                        }, function (error) {

                            self.showFeaturegroups()
                        });
            };

            /**
             * Shows the modal for updating an existing training dataset.
             *
             * @param trainingDataset
             */
            self.updateTrainingDataset = function (trainingDataset) {
                ModalService.updateTrainingDataset('lg', self.projectId, trainingDataset,
                    $scope.selected.value, self.jobs, self.trainingDatasets)
                    .then(
                        function (success) {
                            self.getTrainingDatasets($scope.selected.value)
                            self.showTrainingDatasets()
                        }, function (error) {
                            self.showTrainingDatasets()
                        });
            };

            /**
             * Called when the delete-featuregroup-button is pressed
             *
             * @param featuregroup
             */
            self.deleteFeaturegroup = function (featuregroup) {
                ModalService.confirm('sm', 'Are you sure?',
                    'Are you sure that you want to delete this version of the feature group? ' +
                    'this action will delete all the data in the feature group with the selected version')
                    .then(function (success) {
                        FeaturestoreService.deleteFeaturegroup(self.projectId, $scope.selected.value, featuregroup.id).then(
                            function (success) {
                                self.getFeaturegroups($scope.selected.value)
                                growl.success("Feature group deleted", {title: 'Success', ttl: 1000});
                            },
                            function (error) {
                                growl.error(error.data.errorMsg, {
                                    title: 'Failed to delete the feature group',
                                    ttl: 15000
                                });
                            });
                        growl.info("Deleting featuregroup... wait", {title: 'Deleting', ttl: 1000})
                    }, function (error) {
                        $uibModalInstance.close()
                    });
            };

            /**
             * Called when the delete-trainingDataset-button is pressed
             *
             * @param trainingDataset
             */
            self.deleteTrainingDataset = function (trainingDataset) {
                ModalService.confirm('sm', 'Are you sure?',
                    'Are you sure that you want to delete this version of the training dataset? ' +
                    'this action will delete all the data in the training dataset of this version together with its metadata')
                    .then(function (success) {
                        FeaturestoreService.deleteTrainingDataset(self.projectId, $scope.selected.value, trainingDataset.id).then(
                            function (success) {
                                self.getTrainingDatasets($scope.selected.value)
                                growl.success("Training Dataset deleted", {title: 'Success', ttl: 1000});
                            },
                            function (error) {
                                growl.error(error.data.errorMsg, {
                                    title: 'Failed to delete the training dataset',
                                    ttl: 15000
                                });
                            });
                        growl.info("Deleting training dataset... wait", {title: 'Deleting', ttl: 1000})
                    }, function (error) {
                        $uibModalInstance.close()
                    });
            };

            /**
             * Called when the increment-version-featuregroup-button is pressed
             *
             * @param featuregroups list of featuregroup versions
             * @param versions list
             */
            self.newFeaturegroupVersion = function (featuregroups, versions) {
                var i;
                var maxVersion = -1;
                for (i = 0; i < versions.length; i++) {
                    if (versions[i] > maxVersion)
                        maxVersion = versions[i]
                }
                ModalService.createNewFeaturegroupVersion('lg', self.projectId, featuregroups[maxVersion], $scope.selected.value, self.jobs, self.featuregroups)
                    .then(
                        function (success) {
                            self.getFeaturegroups($scope.selected.value)
                            self.showFeaturegroups()
                        }, function (error) {
                            growl.error(error.data.errorMsg, {
                                title: 'Failed to create a new version of the feature group',
                                ttl: 15000
                            });
                            self.showFeaturegroups()
                        });
            };

            /**
             * Called when the increment-version-trainingDataset-button is pressed
             *
             * @param trainingDatasets list of featuregroup versions
             * @param versions list
             */
            self.newTrainingDatasetVersion = function (trainingDatasets, versions) {
                var i;
                var maxVersion = -1;
                for (i = 0; i < versions.length; i++) {
                    if (versions[i] > maxVersion)
                        maxVersion = versions[i]
                }
                ModalService.createNewTrainingDatasetVersion('lg', self.projectId, trainingDatasets[maxVersion], $scope.selected.value, self.jobs, self.trainingDatasets)
                    .then(
                        function (success) {
                            self.getTrainingDatasets($scope.selected.value)
                            self.showTrainingDatasets()
                        }, function (error) {
                            growl.error(error.data.errorMsg, {
                                title: 'Failed to create a new version of the training dataset',
                                ttl: 15000
                            });
                            self.showTrainingDatasets()
                        });
            };

            /**
             * Called when the clear-featuregroup-contents-button is pressed
             *
             * @param featuregroup
             */
            self.clearFeaturegroupContents = function (featuregroup) {
                ModalService.confirm('sm', 'Are you sure? This action will drop all data in the feature group',
                    'Are you sure that you want to delete the contents of this feature group? ' +
                    'If you want to keep the contents and write new data you can create a new version of the same feature group.')
                    .then(function (success) {
                        FeaturestoreService.clearFeaturegroupContents(self.projectId, $scope.selected.value, featuregroup).then(
                            function (success) {
                                self.getFeaturegroups($scope.selected.value)
                                growl.success("Feature group contents cleared", {title: 'Success', ttl: 1000});
                            },
                            function (error) {
                                growl.error(error.data.errorMsg, {
                                    title: 'Failed to clear the featuregroup contents',
                                    ttl: 15000
                                });
                            }
                        );
                        growl.info("Clearing contents of the featuregroup... wait", {title: 'Clearing', ttl: 1000})
                    }, function (error) {

                    });
            };

            /**
             * Called when the view-featuregroup-statistics button is pressed
             *
             * @param featuregroup
             */
            self.viewFeaturegroupStatistics = function (featuregroup) {
                ModalService.viewFeaturegroupStatistics('lg', self.projectId, featuregroup).then(
                    function (success) {
                        self.showFeaturegroups()
                    }, function (error) {
                        self.showFeaturegroups()
                    });
            };

            /**
             * Called when the view-featuregroup-dependencies button is pressed
             *
             * @param featuregroup
             */
            self.viewFeaturegroupDependencies = function (featuregroup) {
                ModalService.viewFeaturegroupDependencies('lg', self.projectId, featuregroup).then(
                    function (success) {
                        self.showFeaturegroups()
                    }, function (error) {
                        self.showFeaturegroups()
                    });
            };

            /**
             * Called when the view-training dataset-dependencies button is pressed
             *
             * @param training dataset
             */
            self.viewTrainingDatasetDependencies = function (trainingDataset) {
                ModalService.viewTrainingDatasetDependencies('lg', self.projectId, trainingDataset).then(
                    function (success) {
                        self.showTrainingDatasets()
                    }, function (error) {
                        self.showTrainingDatasets()
                    });
            };

            /**
             * Called when the view-training-dataset-statistics button is pressed
             *
             * @param trainingDataset
             */
            self.viewTrainingDatasetStatistics = function (trainingDataset) {
                ModalService.viewTrainingDatasetStatistics('lg', self.projectId, trainingDataset).then(
                    function (success) {
                        self.showTrainingDatasets()
                    }, function (error) {
                        self.showTrainingDatasets()
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
             * Called when the launch-job button is pressed
             */
            self.launchJob = function (jobName) {
                JobService.setJobFilter(jobName)
                self.goToUrl("jobs")
            };

            /**
             * Retrieves a list of all featuregroups for a given featurestore
             *
             * @param featurestore
             */
            self.getFeaturegroups = function (featurestore) {
                FeaturestoreService.getFeaturegroups(self.projectId, featurestore).then(
                    function (success) {
                        self.featuregroups = success.data;
                        self.groupFeaturegroupsByVersion()
                        self.checkFreshnessOfFeaturegroups()
                        self.collectAllFeatures();
                        self.featuregroupsLoaded = true
                        self.stopLoading()
                    },
                    function (error) {
                        self.featuregroupsLoaded = true
                        self.stopLoading()
                        growl.error(error.data.errorMsg, {
                            title: 'Failed to fetch the featuregroups for the featurestore',
                            ttl: 15000
                        });
                    });
            };


            /**
             * Retrieves a list of all training datasets for a given featurestore
             *
             * @param featurestore
             */
            self.getTrainingDatasets = function (featurestore) {
                FeaturestoreService.getTrainingDatasets(self.projectId, featurestore).then(
                    function (success) {
                        self.trainingDatasets = success.data;
                        self.checkFreshnessOfTrainingDatasets()
                        self.groupTrainingDatasetsByVersion()
                        self.trainingDatasetsLoaded = true
                        self.stopLoading()
                    },
                    function (error) {
                        self.trainingDatasetsLoaded = true
                        self.stopLoading()
                        growl.error(error.data.errorMsg, {
                            title: 'Failed to fetch the training datasets for the featurestore',
                            ttl: 15000
                        });
                    });
            };

            /**
             * Helper to collect the features of all featuregroups in the featurestore into a
             * single list
             */
            self.collectAllFeatures = function () {
                var featuresTemp = []
                var i;
                var j;
                for (i = 0; i < self.featuregroups.length; i++) {
                    var fgFeatures = []
                    for (j = 0; j < self.featuregroups[i].features.length; j++) {
                        fgFeatures.push({
                            name: self.featuregroups[i].features[j].name,
                            type: self.featuregroups[i].features[j].type,
                            description: self.featuregroups[i].features[j].description,
                            primary: self.featuregroups[i].features[j].primary,
                            featuregroup: self.featuregroups[i].name,
                            version: self.featuregroups[i].version
                        })
                    }
                    featuresTemp = featuresTemp.concat(fgFeatures)
                }
                self.features = featuresTemp;
            }

            /**
             * Goes through the list of featuregrup and analyzes associated jobs and dependencies to check
             * if featuregroups are up-to-date featuregroup stale.
             */
            self.checkFreshnessOfFeaturegroups = function () {
                var i;
                var j;
                for (i = 0; i < self.featuregroups.length; i++) {
                    var outOfDate = false;
                    var outOfDateReason = "Featuregroup is out-of-date:"
                    if (self.featuregroups[i].lastComputed !== null) {
                        var lastComputed = Date.parse(self.featuregroups[i].lastComputed)
                        var jobStatus = self.featuregroups[i].jobStatus
                    } else {
                        lastComputed = -1
                    }
                    for (j = 0; j < self.featuregroups[i].dependencies.length; j++) {
                        var dependencyModificationDate = Date.parse(self.featuregroups[i].dependencies[j].modification)
                        if ((dependencyModificationDate > lastComputed || jobStatus !== 'Succeeded') && self.featuregroups[i].jobId !== null) {
                            outOfDate = true
                            if (jobStatus === 'Succeeded') {
                                outOfDateReason = outOfDateReason
                                    + " dataset dependency: "
                                    + self.featuregroups[i].dependencies[j].path
                                    + " was modified at: "
                                    + self.featuregroups[i].dependencies[j].modification
                                    + ", which is after the last successful job execution: " + Date.parse(self.featuregroups[i]).lastComputed
                            } else {
                                var jobFailedstr = " the last feature engineering job did not complete successfully."
                                if (outOfDateReason.indexOf(jobFailedstr) === -1) {
                                    outOfDateReason = outOfDateReason
                                        + jobFailedstr
                                }
                            }
                        }
                    }
                    self.featuregroups[i].outOfDate = outOfDate;
                    self.featuregroups[i].outOfDateReason = outOfDateReason
                }
            }

            /**
             * Goes through the list of Training Datasets and analyzes associated jobs and dependencies to check
             * if training datasets are up-to-date or stale.
             */
            self.checkFreshnessOfTrainingDatasets = function () {
                var i;
                var j;
                for (i = 0; i < self.trainingDatasets.length; i++) {
                    var outOfDate = false;
                    var outOfDateReason = "Training Dataset is out-of-date:"
                    if (self.trainingDatasets[i].lastComputed !== null) {
                        var lastComputed = Date.parse(self.trainingDatasets[i].lastComputed)
                        var jobStatus = self.trainingDatasets[i].jobStatus
                    } else {
                        lastComputed = -1
                    }
                    for (j = 0; j < self.trainingDatasets[i].dependencies.length; j++) {
                        var dependencyModificationDate = Date.parse(self.trainingDatasets[i].dependencies[j].modification)
                        if ((dependencyModificationDate > lastComputed || jobStatus !== 'Succeeded') && self.trainingDatasets[i].jobId !== null) {
                            outOfDate = true
                            if (jobStatus === 'Succeeded') {
                                outOfDateReason = outOfDateReason
                                    + " dataset dependency: "
                                    + self.trainingDatasets[i].dependencies[j].path
                                    + " was modified at: "
                                    + self.trainingDatasets[i].dependencies[j].modification
                                    + ", which is after the last successful job execution: " + Date.parse(self.trainingDatasets[i]).lastComputed
                            } else {
                                var jobFailedstr = " the last feature engineering job did not complete successfully."
                                if (outOfDateReason.indexOf(jobFailedstr) === -1) {
                                    outOfDateReason = outOfDateReason
                                        + jobFailedstr
                                }
                            }
                        }
                    }
                    self.trainingDatasets[i].outOfDate = outOfDate;
                    self.trainingDatasets[i].outOfDateReason = outOfDateReason
                }
            }

            /**
             * Goes through a list of featuregroups and groups them by name so that you get name --> versions mapping
             */
            self.groupFeaturegroupsByVersion = function () {
                var dict = {}
                var i;
                var versionVar;
                for (i = 0; i < self.featuregroups.length; i++) {
                    if (self.featuregroups[i].name in dict) {
                        versionVar = self.featuregroups[i].version.toString()
                        dict[self.featuregroups[i].name][versionVar] = self.featuregroups[i]
                    } else {
                        versionVar = self.featuregroups[i].version.toString()
                        dict[self.featuregroups[i].name] = {}
                        dict[self.featuregroups[i].name][versionVar] = self.featuregroups[i]
                    }
                }
                var dictList = []
                var item;
                for (var key in dict) {
                    item = {};
                    item.name = key;
                    item.versionToGroups = dict[key];
                    var versions = Object.keys(item.versionToGroups)
                    item.versions = versions
                    item.activeVersion = versions[versions.length - 1];
                    dictList.push(item);
                }
                self.featuregroupsDictList = dictList
            }

            /**
             * Goes through a list of training datasets and groups them by name so that you get name --> versions mapping
             */
            self.groupTrainingDatasetsByVersion = function () {
                var dict = {}
                var i;
                var versionVar;
                for (i = 0; i < self.trainingDatasets.length; i++) {
                    if (self.trainingDatasets[i].name in dict) {
                        versionVar = self.trainingDatasets[i].version.toString()
                        dict[self.trainingDatasets[i].name][versionVar] = self.trainingDatasets[i]
                    } else {
                        versionVar = self.trainingDatasets[i].version.toString()
                        dict[self.trainingDatasets[i].name] = {}
                        dict[self.trainingDatasets[i].name][versionVar] = self.trainingDatasets[i]
                    }
                }
                var dictList = []
                var item;
                for (var key in dict) {
                    item = {};
                    item.name = key;
                    item.versionToGroups = dict[key];
                    var versions = Object.keys(item.versionToGroups)
                    item.versions = versions
                    item.activeVersion = versions[versions.length - 1];
                    dictList.push(item);
                }
                self.trainingDatasetsDictList = dictList
            }

            /**
             * Called when the "features" tab is pressed in the UI
             */
            self.showFeatures = function () {
                self.showFeaturegroupsBool = -1;
                self.showTrainingDatasetsBool = -1;
                self.showFeaturesBool = 1;
            };

            /**
             * Called when the "featuresGroups" tab is pressed in the UI
             */
            self.showFeaturegroups = function () {
                self.showFeaturesBool = -1;
                self.showTrainingDatasetsBool = -1;
                self.showFeaturegroupsBool = 1;
            };

            /**
             * Called when the "Training Datasets" tab is pressed in the UI
             */
            self.showTrainingDatasets = function () {
                self.showFeaturesBool = -1;
                self.showFeaturegroupsBool = -1;
                self.showTrainingDatasetsBool = 1;
            };

            /**
             * Opens the modal to view a featuregroup schema
             *
             * @param featuregroup
             */
            self.viewSchemaContent = function (featuregroup) {
                ModalService.viewFeatureSchemaContent('lg', self.projectId, featuregroup).then(
                    function (success) {
                        self.showFeaturegroups()
                    }, function (error) {
                        self.showFeaturegroups()
                    });
            }

            /**
             * Opens the modal to view a trainingDataset schema
             *
             * @param trainingDataset
             */
            self.viewTrainingDatasetSchemaContent = function (trainingDataset) {
                ModalService.viewTrainingDatasetSchemaContent('lg', self.projectId, trainingDataset).then(
                    function (success) {
                        self.showTrainingDatasets()
                    }, function (error) {
                        self.showTrainingDatasets()
                    });
            }

            /**
             * Opens the modal to view featurestore information
             */
            self.viewFeaturestoreInfo = function () {
                ModalService.viewFeaturestoreInfo('lg', self.projectId, $scope.selected.value).then(
                    function (success) {

                    }, function (error) {
                        //The user changed their mind.
                    });
            };

            /**
             * Opens the modal to view featuregroup information
             *
             * @param featuregroup
             */
            self.viewFeaturegroupInfo = function (featuregroup) {
                ModalService.viewFeaturegroupInfo('lg', self.projectId, featuregroup, $scope.selected.value).then(
                    function (success) {
                        self.showFeaturegroups()
                    }, function (error) {
                        self.showFeaturegroups()
                    });
            };

            /**
             * Opens the modal to view feature information
             *
             * @param feature
             */
            self.viewFeatureInfo = function (feature) {
                ModalService.viewFeatureInfo('lg', self.projectId, feature, $scope.selected.value).then(
                    function (success) {
                        self.showFeatures()
                    }, function (error) {
                        self.showFeatures()
                    });
            };

            /**
             * Opens the modal to view training dataset information
             *
             * @param trainingDataset
             */
            self.viewTrainingDatasetInfo = function (trainingDataset) {
                ModalService.viewTrainingDatasetInfo('lg', self.projectId, trainingDataset, $scope.selected.value).then(
                    function (success) {
                        self.showTrainingDatasets()
                    }, function (error) {
                        self.showTrainingDatasets()
                    });
            };

            /**
             * Opens the modal to preview featuregroup data
             *
             * @param featuregroup
             */
            self.previewFeaturegroup = function (featuregroup) {
                ModalService.previewFeaturegroup('lg', self.projectId, $scope.selected.value, featuregroup).then(
                    function (success) {
                        self.showFeaturegroups()
                    }, function (error) {
                        self.showFeaturegroups()
                    });
            };

            /**
             * Called when a new featurestore is selected in the dropdown list in the UI
             *
             * @param item the selected featurestore
             * @param model the select-model
             */
            self.onSelectFeaturestoreCallback = function (featurestore) {
                self.startLoading("Loading Feature store data...");
                self.getTrainingDatasets(featurestore)
                self.getFeaturegroups(featurestore)
            };

            /**
             * Initializes the UI by retrieving featurstores from the backend
             */
            self.init = function () {
                self.startLoading("Loading Feature store data...");
                self.getFeaturestores()
                self.getAllJobs()
            };


            /**
             * Called when clicking the link to featuregroup from the list of features. Switches the view to the
             * specific featuregroup
             *
             * @param featuregroupName the featuregroup to go to
             */
            self.goToFeaturegroup = function (featuregroupName) {
                self.showFeaturesBool = -1;
                self.showFeaturegroupsBool = 1;
                self.fgFilter = featuregroupName
            };


            /**
             * Check if a job of a featuregroup in the featurestore belongs to this project's jobs or another project
             *
             * @param jobId the jobId to lookup
             */
            self.isJobLocal = function (jobId) {
                var i;
                var jobIds = []
                var jobFoundBool = false
                for (i = 0; i < self.jobs.length; i++) {
                    if (self.jobs[i].id === jobId) {
                        jobFoundBool = true
                    }
                }
                return jobFoundBool
            };

            /**
             * Gets all jobs for the project
             */
            self.getAllJobs = function () {
                JobService.getAllJobsInProject(self.projectId).then(
                    function (success) {
                        self.jobs = success.data;
                    }, function (error) {
                        growl.error(error.data.errorMsg, {title: 'Failed to fetch jobs for the project', ttl: 15000});
                    });
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
             * Format javascript date as string (YYYY-mm-dd HH:MM:SS)
             *
             * @param d date to format
             * @returns {string} formatted string
             */
            $scope.formatDate = function (javaDate) {
                var d = new Date(javaDate)
                var date_format_str = d.getFullYear().toString() + "-" + ((d.getMonth() + 1).toString().length == 2 ? (d.getMonth() + 1).toString() : "0" + (d.getMonth() + 1).toString()) + "-" + (d.getDate().toString().length == 2 ? d.getDate().toString() : "0" + d.getDate().toString()) + " " + (d.getHours().toString().length == 2 ? d.getHours().toString() : "0" + d.getHours().toString()) + ":" + ((parseInt(d.getMinutes() / 5) * 5).toString().length == 2 ? (parseInt(d.getMinutes() / 5) * 5).toString() : "0" + (parseInt(d.getMinutes() / 5) * 5).toString()) + ":00";
                return date_format_str
            }

            self.init()
        }
    ])
;
