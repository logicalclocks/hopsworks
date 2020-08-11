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
    .controller('trainingDatasetViewInfoCtrl', ['$scope', 'FeaturestoreService', 'ProjectService',
        'JobService', 'ModalService', 'StorageService', 'ProvenanceService', '$location', 'growl',
        function ($scope, FeaturestoreService, ProjectService, JobService, ModalService, StorageService, ProvenanceService, $location, growl) {

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
            self.loadingTags = false;
            //State
            self.sizeWorking = false;
            self.size = "Not fetched"
            self.pythonCode = ""
            self.scalaCode = ""
            self.attachedTags = [];

            /**
             * Get training dataset tags
             */
            self.fetchTags = function () {
                if (self.selectedTrainingDataset.trainingDatasetType === "EXTERNAL_TRAINING_DATASET") {
                    return 
                }

                self.loadingTags = true;
                FeaturestoreService.getTrainingDatasetTags(self.projectId, self.featurestore, self.selectedTrainingDataset).then(
                    function (success) {
                        self.loadingTags = false;
                        self.attachedTags = [];
                        if(success.data.items) {
                            for (var i = 0; i < success.data.items.length; i++) {
                                self.attachedTags.push({"tag": success.data.items[i].name, "value": success.data.items[i].value});
                            }
                        } else {
                            self.attachedTags = [];
                        }
                      },
                    function (error) {
                        self.loadingTags = false;
                        if(error.status !== 422) {
                            if (typeof error.data.usrMsg !== 'undefined') {
                                growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 8000});
                            } else {
                                growl.error("", {title: error.data.errorMsg, ttl: 8000});
                            }
                        }
                    });
            };

            /**
             * Add training dataset tags
             */
            self.addTag = function(name, value) {
                self.loadingTags = true;
                FeaturestoreService.updateTrainingDatasetTag(self.projectId, self.featurestore, self.selectedTrainingDataset, name, value).then(
                    function (success) {
                        self.attachedTags = [];
                        self.loadingTags = false;
                        if(success.data.items) {
                            for (var i = 0; i < success.data.items.length; i++) {
                                self.attachedTags.push({"tag": success.data.items[i].name, "value": success.data.items[i].value});
                            }
                        } else {
                            self.attachedTags = [];
                        }
                    },
                    function (error) {
                        self.loadingTags = false;
                        if(error.status !== 404) {
                            if (typeof error.data.usrMsg !== 'undefined') {
                                growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 8000});
                            } else {
                                growl.error("", {title: error.data.errorMsg, ttl: 8000});
                            }
                        }
                    });
            };

            /**
             * Delete training dataset tag
             */
            self.deleteTag = function(name) {
                self.loadingTags = true;
                FeaturestoreService.deleteTrainingDatasetTag(self.projectId, self.featurestore, self.selectedTrainingDataset, name).then(
                    function (success) {
                        self.attachedTags = [];
                        self.fetchTags();
                    },
                    function (error) {
                        self.loadingTags = false;
                        if(error.status !== 404) {
                            if (typeof error.data.usrMsg !== 'undefined') {
                                growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 8000});
                            } else {
                                growl.error("", {title: error.data.errorMsg, ttl: 8000});
                            }
                        }
                    });
            };

            /**
             * Get the Python API code to retrieve the training dataset
             */
            self.getPythonCode = function () {
                var codeStr = "from hops import featurestore\n"
                codeStr = codeStr + "featurestore.get_training_dataset_path('" + self.selectedTrainingDataset.name + "')"
                return codeStr
            };

            /**
             * Get the Scala API code to retrieve the training dataset
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
                if (self.selectedTrainingDataset
                    && self.selectedTrainingDataset.id === selectedTrainingDataset.id
                    && self.tgState === true) {
                    self.tgState = false;
                } else {
                    self.tgState = true;
                }
            }

            /**
             * Initialization function
             */
            self.view = function (featurestoreCtrl, trainingDatasets, toggle) {

                if(toggle) {
                    self.toggle(trainingDatasets.versionToGroups[trainingDatasets.activeVersion]);
                }

                self.selectedTrainingDataset = trainingDatasets.versionToGroups[trainingDatasets.activeVersion];

                self.projectId = featurestoreCtrl.projectId;
                self.projectName = featurestoreCtrl.projectName;
                self.featurestore = featurestoreCtrl.featurestore;
                self.trainingDatasets = trainingDatasets;
                self.activeVersion = trainingDatasets.activeVersion;
                self.settings = featurestoreCtrl.settings;

                self.hopsfsTrainingDatasetType = self.settings.hopsfsTrainingDatasetType
                self.externalTrainingDatasetType = self.settings.externalTrainingDatasetType

                // The location fields contains the scheme + IP if the training dataset
                // is stored on HopsFS. they clutter the UI and break the redirect.
                // Here we remove them.
                if (self.selectedTrainingDataset.trainingDatasetType == self.hopsfsTrainingDatasetType) {
                    self.selectedTrainingDataset.location = 
                        "/" + self.selectedTrainingDataset.location.split("/").slice(3).join("/")
                }

                self.pythonCode = self.getPythonCode();
                self.scalaCode = self.getScalaCode();
                self.fetchSize();
                self.fetchTags();
                self.getGeneratedModelLinks(self.selectedTrainingDataset.name, self.selectedTrainingDataset.version);
                self.getSourceFGLinks(self.selectedTrainingDataset.name, self.selectedTrainingDataset.version);
                self.getExperimentsLinks(self.selectedTrainingDataset.name, self.selectedTrainingDataset.version);
            };

            $scope.$on('trainingDatasetSelected', function (event, args) {
                self.view(args.featurestoreCtrl, args.trainingDatasets, args.toggle);
            });


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

            self.tdLocation = function() {
                if (self.selectedTrainingDataset.trainingDatasetType == self.externalTrainingDatasetType) {
                    return
                }
                $location.path('project/' + self.projectId + '/datasets' + self.selectedTrainingDataset.location);
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
             */
            self.newTrainingDatasetVersion = function (featurestoreCtrl) {
                StorageService.store("trainingdataset_operation", "NEW_VERSION");
                StorageService.store(self.projectId + "_fgFeatures", featurestoreCtrl.fgFeatures);
                StorageService.store(self.projectId + "_trainingDataset", self.selectedTrainingDataset);

                var maxVersion = -1;
                for (var i = 0; i < self.trainingDatasets.versions.length; i++) {
                    var version = parseInt(self.trainingDatasets.versions[i])
                    if (version > maxVersion) {
                        maxVersion = version
                    }
                }
                StorageService.store(self.projectId + "_trainingDataset_version", maxVersion + 1);
                self.goToUrl("newtrainingdataset")
            };

            /**
             * Shows the page for updating an existing training dataset.
             *
             */
            self.updateTrainingDataset = function (featurestoreCtrl) {
                StorageService.store("trainingdataset_operation", "UPDATE");
                StorageService.store(self.projectId + "_fgFeatures", featurestoreCtrl.fgFeatures);
                StorageService.store(self.projectId + "_trainingDataset", self.selectedTrainingDataset);
                StorageService.store(self.projectId + "_trainingDataset_version", self.selectedTrainingDataset.version);
                self.goToUrl("newtrainingdataset")
            };

            self.goToModel = function(model) {
                if (model) {
                    const m = model.name + '_' + model.version;
                    StorageService.store(self.projectId + "_model", m);
                    $location.path('project/' + model.projId + '/models');
                }
            };

            self.goToFG = function(fsId, name, version) {
                $location.search('');
                $location.path('/project/' + self.projectId + '/featurestore');
                $location.search('featurestore', fsId);
                $location.search('featureGroup', name);
                $location.search('version', version);
            };

            self.goToExperiment = function (link) {
                const experimentId = link.name + "_" + link.version;
                StorageService.store(self.projectId + "_experiment", experimentId);
                $location.path('project/' + self.projectId + '/experiments');
            };

            self.errorPrint = function(error) {
                if (typeof error.data.usrMsg !== 'undefined') {
                    growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 8000});
                } else {
                    growl.error('', {title: error.data.errorMsg, ttl: 8000});
                }
            };

            self.getProjectId = function(link) {
                /** get project id from project name */
                ProjectService.getProjectInfo({projectName: link.projName}).$promise.then(
                    function (success2) {
                        link.projId = success2.projectId;
                    }, self.errorPrint);
            };

            self.getLinkInfo = function(link) {
                /** get project id from project name */
                ProjectService.getProjectInfo({projectName: link.projName}).$promise.then(
                    function (success2) {
                        link.projId = success2.projectId;
                        /** get featurestore of fg */
                        FeaturestoreService.getFeaturestores(link.projId).then(
                            function (success3) {
                                /** get the project's main featurestore */
                                const fs = success3.data.filter(function(fs) {
                                    return fs.projectName === link.projName;
                                });
                                if (fs.length === 1) {
                                    link.fsId = fs[0].featurestoreId;
                                } else {
                                    console.log('featurestore not in project');
                                    growl.error('featurestore not in project', {title: 'provenance error', ttl: 8000});
                                }
                            }, self.errorPrint);
                    }, self.errorPrint);
            };

            self.getInArtifacts = function(name, version, inType, outType, linkInfoFunc, links) {
                ProvenanceService.getAppLinks(self.projectId, {outArtifactName: name, outArtifactVersion: version, inArtifactType: inType, outArtifactType: outType}).then(
                    function(success) {
                        if(success.data.items !== undefined) {
                            for (var i = 0; i < success.data.items.length; i++) {
                                for (var j = 0; j < success.data.items[i].in.entry.length; j++) {
                                    var versionSplitIndex = success.data.items[i].in.entry[j].value.mlId.lastIndexOf('_');
                                    var link = {};
                                    links.push(link);
                                    link.name = success.data.items[i].in.entry[j].value.mlId.substring(0, versionSplitIndex);
                                    link.version = parseInt(success.data.items[i].in.entry[j].value.mlId.substring(versionSplitIndex + 1));
                                    link.projName = success.data.items[i].in.entry[j].value.projectName;
                                    link.appId = success.data.items[i].in.entry[j].value.appId;
                                    linkInfoFunc(link);
                                }
                            }
                        }
                    }, self.errorPrint);
            };

            self.getOutArtifacts = function(name, version, inType, outType, linkInfoFunc, links) {
                ProvenanceService.getAppLinks(self.projectId, {inArtifactName: name, inArtifactVersion: version, inArtifactType: inType, outArtifactType: outType}).then(
                    function(success) {
                        if(success.data.items !== undefined) {
                            for (var i = 0; i < success.data.items.length; i++) {
                                for (var j = 0; j < success.data.items[i].out.entry.length; j++) {
                                    var versionSplitIndex = success.data.items[i].out.entry[j].value.mlId.lastIndexOf('_');
                                    var link = {};
                                    links.push(link);
                                    link.name = success.data.items[i].out.entry[j].value.mlId.substring(0, versionSplitIndex);
                                    link.version = parseInt(success.data.items[i].out.entry[j].value.mlId.substring(versionSplitIndex + 1));
                                    link.projName = success.data.items[i].out.entry[j].value.projectName;
                                    link.appId = success.data.items[i].out.entry[j].value.appId;
                                    linkInfoFunc(link);
                                }
                            }
                        }
                    }, self.errorPrint);
            };

            self.getGeneratedModelLinks = function (name, version) {
                self.selectedTrainingDataset.modelLinks = [];
                /** td -> app -> model */
                self.getOutArtifacts(name, version, 'TRAINING_DATASET', 'MODEL', self.getProjectId, self.selectedTrainingDataset.modelLinks);
            };

            self.getExperimentsLinks = function (name, version) {
                self.selectedTrainingDataset.experimentLinks = [];
                /** td -> app -> experiment */
                self.getOutArtifacts(name, version, 'TRAINING_DATASET', 'EXPERIMENT', self.getProjectId, self.selectedTrainingDataset.experimentLinks);
            };

            self.getSourceFGLinks = function (name, version) {
                self.selectedTrainingDataset.fgLinks = [];
                /** fg <- app <- td */
                self.getInArtifacts(name, version, 'FEATURE', 'TRAINING_DATASET', self.getLinkInfo, self.selectedTrainingDataset.fgLinks);
            };
        }]);

