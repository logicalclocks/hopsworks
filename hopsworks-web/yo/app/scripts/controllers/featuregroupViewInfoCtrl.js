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
 * Controller for the Featuregroup-Info view
 */
angular.module('hopsWorksApp')
    .controller('featuregroupViewInfoCtrl', ['$scope', 'FeaturestoreService', 'ProjectService',
        'JobService', 'StorageService', 'ModalService', '$location', 'growl',
        function ($scope, FeaturestoreService, ProjectService, JobService, StorageService, ModalService, $location, growl) {

            /**
             * Initialize controller state
             */
            var self = this;

            //Controller State
            self.tgState = false;
            self.featurestoreCtrl = null;
            self.projectName = null;
            self.projectId = null;
            self.selectedFeaturegroup = null;
            self.featuregroups = null;
            self.activeVersion = null;
            self.featurestore = null;
            self.settings = null;
            self.sizeWorking = false;
            self.loadingTags = false;
            self.offlineSize = "N/A"
            self.onlineSize = "N/A"
            self.offlineSchema ="Not fetched";
            self.onlineSchema = "Not fetched";
            self.hiveTableType = "N/A";
            self.inputFormat = "N/A";
            self.pythonCode = ""
            self.scalaCode = ""
            self.offlineSchemaWorking= false;
            self.onlineSchemaWorking= false;
            self.offlineSampleColumns = []
            self.attachedTags = [];
            self.showDataFormat = false;
            self.showPath = false;

            self.featurestoreCtrl = null;

            /**
             * Get featuregroup tags
             */
            self.fetchTags = function () {
                self.loadingTags = true;
                FeaturestoreService.getFeaturegroupTags(self.projectId, self.featurestore, self.selectedFeaturegroup).then(
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
             * Add featuregroup tags
             */
            self.addTag = function(name, value) {
                self.loadingTags = true;
                FeaturestoreService.updateFeaturegroupTag(self.projectId, self.featurestore, self.selectedFeaturegroup, name, value).then(
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
             * Delete featuregroup tag
             */
            self.deleteTag = function(name) {
                self.loadingTags = true;
                FeaturestoreService.deleteFeaturegroupTag(self.projectId, self.featurestore, self.selectedFeaturegroup, name).then(
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

            self.queryFeaturegroup = $location.search()['featuregroup'];

            /**
             * Get the API code to retrieve the featuregroup with the Python API
             */
            self.getPythonCode = function () {
                var codeStr = "from hops import featurestore\n"
                codeStr = codeStr + "featurestore.get_featuregroup('" + self.selectedFeaturegroup.name + "')"
                return codeStr
            };

            /**
             * Get the API code to retrieve the featuregroup with the Scala API
             */
            self.getScalaCode = function () {
                var codeStr = "import io.hops.util.Hops\n"
                codeStr = codeStr + "Hops.getFeaturegroup(\"" + self.selectedFeaturegroup.name + "\").read()"
                return codeStr
            };


            /**
             * Fetch offline details from Hive by making a REST call to Hopsworks
             */
            self.fetchOfflineDetails = function () {
                if(self.schemaWorking){
                    return
                }
                self.offlineSchemaWorking = true

                FeaturestoreService.getFeaturegroupDetails(self.projectId, self.featurestore, 
                                                           self.selectedFeaturegroup, "OFFLINE").then(
                    function (success) {
                        self.offlineSchemaWorking = false;
                        self.offlineSchema = success.data.schema;
                        self.inputFormat = success.data.inputFormat;
                        self.hiveTableType = success.data.hiveTableType;
                        self.offlineSize = success.data.size;
                    }, function (error) {
                        growl.error(error.data.errorMsg, {title: 'Failed to fetch offline featuregroup details', ttl: 5000});
                        self.offlineSchemaWorking = false;
                    });
            };

            /**
             * Fetch online details from Hive by making a REST call to Hopsworks
             */
            self.fetchOnlineDetails = function () {
                if(self.onlineSchemaWorking){
                    return
                }
                self.schemaWorking = true

                FeaturestoreService.getFeaturegroupDetails(self.projectId, self.featurestore, 
                                                           self.selectedFeaturegroup, "ONLINE").then(
                    function (success) {
                        self.onlineSchemaWorking = false;
                        self.onlineSchema = success.data.schema;
                        self.onlineSize = success.data.size;
                    }, function (error) {
                        growl.error(error.data.errorMsg, {title: 'Failed to fetch online featuregroup details', ttl: 5000});
                        self.onlineSchemaWorking = false;
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
             * Called when the launch-job button is pressed
             */
            self.launchJob = function (jobName) {
                JobService.setJobFilter(jobName);
                self.goToUrl("jobs")
            };

            self.toggle = function(selectedFeatureGroup) {
                if (self.selectedFeaturegroup
                    && self.selectedFeaturegroup.id === selectedFeatureGroup.id
                    && self.tgState === true) {
                    self.tgState = false;
                } else {
                    self.tgState = true;
                }
            }

            /**
             * Initialization function
             */
            self.view = function (featurestoreCtrl, featuregroups, toggle) {

                if(toggle) {
                    self.toggle(featuregroups.versionToGroups[featuregroups.activeVersion]);
                }

                self.selectedFeaturegroup = featuregroups.versionToGroups[featuregroups.activeVersion];

                self.showDataFormat = typeof self.selectedFeaturegroup.dataFormat !== 'undefined';
                self.showPath = typeof self.selectedFeaturegroup.path !== 'undefined' 
                                    && self.selectedFeaturegroup.path != "";

                self.featurestoreCtrl = featurestoreCtrl;
                self.projectId = featurestoreCtrl.projectId;
                self.projectName = featurestoreCtrl.projectName;
                self.featurestore = featurestoreCtrl.featurestore;
                self.featuregroups = featuregroups;
                self.activeVersion = featuregroups.activeVersion;
                self.settings = featurestoreCtrl.settings;

                self.pythonCode = self.getPythonCode();
                self.scalaCode = self.getScalaCode();

                self.featuregroupType = "";
                if(self.selectedFeaturegroup.type === 'onDemandFeaturegroupDTO'){
                    self.featuregroupType = "ON DEMAND";
                } else {
                    self.featuregroupType = "CACHED";
                    self.fetchOfflineDetails();
                    if (self.selectedFeaturegroup.onlineEnabled === true) {
                        self.fetchOnlineDetails();
                    }
                }
                self.fetchTags();
            };

            $scope.$on('featuregroupSelected', function (event, args) {
                self.view(args.featurestoreCtrl, args.featuregroups, args.toggle);
            });

            /**
             * Helper function for redirecting to another project page
             *
             * @param serviceName project page
             */
            self.goToUrl = function (serviceName) {
                $location.path('project/' + self.projectId + '/' + serviceName);
            };

            self.goToStorageConnector = function () {
                var connParam = {
                    "storageConnector": self.selectedFeaturegroup.storageConnector.name
                }

                $location.path('project/' + self.projectId + "/featurestore").search(connParam);
            };

            /**
             * Called when the increment-version-featuregroup-button is pressed
             *
             */
            self.newFeaturegroupVersion = function () {
                StorageService.store("featuregroup_operation", "NEW_VERSION");
                StorageService.store(self.projectId + "_featuregroup", self.selectedFeaturegroup);

                var maxVersion = -1;
                for (var i = 0; i < self.featuregroups.versions.length; i++) {
                    var version = parseInt(self.featuregroups.versions[i])
                    if (version > maxVersion) {
                        maxVersion = version
                    }
                }
                StorageService.store(self.projectId + "_featuregroup_version", maxVersion + 1);
                self.goToUrl("newfeaturegroup")
            };

            /**
             * Called when the delete-featuregroup-button is pressed
             *
             */
            self.deleteFeaturegroup = function (featurestoreCtrl) {
                ModalService.confirm('md', 'Are you sure?',
                    'Are you sure that you want to delete version ' + self.selectedFeaturegroup.version + ' of the ' + self.selectedFeaturegroup.name + ' featuregroup? ' +
                                        'This action will delete the data and metadata and can not be undone.')
                    .then(function (success) {
                        FeaturestoreService.deleteFeaturegroup(self.projectId, self.featurestore, self.selectedFeaturegroup.id).then(
                            function (success) {
                                self.tgState = false;
                                featurestoreCtrl.getFeaturegroups(self.featurestore);
                                growl.success("Feature group deleted", {title: 'Success', ttl: 2000});
                            },
                            function (error) {
                                growl.error(error.data.errorMsg, {
                                    title: 'Failed to delete the feature group',
                                    ttl: 15000
                                });
                            });
                        growl.info("Deleting featuregroup...", {title: 'Deleting', ttl: 2000})
                    }, function (error) {});
            };

            /**
             * Goes to the edit page for updating a feature group
             *
             */
            self.updateFeaturegroup = function () {
                StorageService.store("featuregroup_operation", "UPDATE");
                StorageService.store(self.projectId + "_featuregroup", self.selectedFeaturegroup);
                StorageService.store(self.projectId + "_featuregroup_version", self.selectedFeaturegroup.version);
                self.goToUrl("newfeaturegroup")
            };

            self.preview = function() {
                // Close tab before showing the new section of the page
                self.tgState = false;
                /// call featurestoreCtrl to show the preview
                self.featurestoreCtrl.togglePreview(self.selectedFeaturegroup);
            }

            /**
             * Called when the view-featuregroup-statistics button is pressed
             *
             */
            self.viewFeaturegroupStatistics = function () {
                self.featurestoreCtrl.fgStatistics = self.selectedFeaturegroup;
                self.featurestoreCtrl.showStatistics = true;
            };

            self.fgLocation = function() {
                var locationStr = self.selectedFeaturegroup.location;
                var locationSplits = locationStr.split("/");
                var datasetLocation = null;

                if (locationStr.includes("apps/hive/warehouse")) {
                    // need special treatment for the warehouse dirs
                    datasetLocation = locationSplits.slice(6).join("/");
                } else { 
                    // just pass the project path to the dataset
                    datasetLocation = locationSplits.slice(3).join("/");
                }
                $location.path('project/' + self.projectId + '/datasets/' + datasetLocation);
            };
        }]);

