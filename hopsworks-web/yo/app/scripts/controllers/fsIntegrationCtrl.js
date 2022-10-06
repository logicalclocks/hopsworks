/*
 * This file is part of Hopsworks
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
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

'use strict';

angular.module('hopsWorksApp')
    .controller('FsIntegrationCtrl', ['$scope', '$routeParams', 'growl', 'FsIntegrationService', 'UserService', 'MembersService', 'FileSaver',
        function ($scope, $routeParams, growl, FsIntegrationService, UserService, MembersService, FileSaver) {
            var self = this; 

            self.projectId = $routeParams.projectID;
            
            self.dbInstance = "";
            self.dbInstanceUrl = "";
            self.dbInstanceApiKey = "";
            self.dbInstances = [];
            self.dbClusters = [];

            // Only data owners are allowed to configure clusters for team members
            self.dataScientist = true;
            self.username = "";
            self.projectMembers = [];

            // sorting 
            self.sortKey = "id";
            self.sortReverse = false;
            self.pageSize = 5;

            self.clusterSpinner = false;
            self.downloadSpinner = false; 
            self.addInstanceSpinner = false;

            self.showAddInstance = false;

            self.setSort = function(key) {
                if (self.sortKey == key) {
                    self.sortReverse = !self.sortReverse
                } else {
                    self.sortKey = key;
                }
            };

            self.getClusters = function() {
                self.clusterSpinner = true;
                FsIntegrationService.getClusters(self.projectId, self.dbInstance).then(
                    function(success) {
                        self.clusterSpinner = false;
                        self.dbClusters = success.data.items;
                        self.getUsername();
                    }, function(error) {
                        self.clusterSpinner = false;
                        growl.error(error.data.errorMsg, {
                            title: 'Failed to get Databricks Clusters',
                            ttl: 15000
                        });
                    }
                )
            };

            self.configure = function(dbCluster) { 
                growl.success("Configuring cluster: " + dbCluster.id, {
                            title: 'Started cluster configuration',
                            ttl: 15000
                        });

                var targetUser = dbCluster.targetUser;
                if (self.dataScientist) {
                    targetUser = self.username;
                }

                FsIntegrationService.configure(self.projectId, self.dbInstance, 
                                               dbCluster.id, targetUser).then(
                    function(success) {
                        growl.success("Cluster: " + dbCluster.id + " configured", {
                            title: 'Success',
                            ttl: 15000
                        });
                        self.getClusters();
                    }, function(error) {
                        growl.error(error.data.errorMsg, {
                            title: 'Failed to configure Databricks cluster',
                            ttl: 15000
                        });
                    }
                )
            };

            self.showAdd = function() {
                self.showAddInstance = !self.showAddInstance;
            };

            self.addInstance = function() {
                self.addInstanceSpinner = true;
                FsIntegrationService.addInstance(self.projectId, 
                                                 self.dbInstanceUrl,
                                                 self.dbInstanceApiKey).then(
                    function(success) {
                        self.addInstanceSpinner = false;
                        self.getInstances();
                    }, function(error) {
                        self.addInstanceSpinner = false;
                        growl.error(error.data.errorMsg, {
                            title: 'Failed to add Databricks Instance',
                            ttl: 15000
                        });
                });
            };

            self.getUsername = function() {
                UserService.profile().then(
                    function(success) {
                        for (var i=0; i < self.dbClusters.length; i++) {
                            self.username = success.data.username;
                            if (typeof(self.dbClusters[i].user) === "undefined") {
                                self.dbClusters[i].targetUser = success.data.username;
                            } else {
                                self.dbClusters[i].targetUser = self.dbClusters[i].user.username;
                            }
                        }
                    }, function (error) {
                        growl.error(error.data.errorMsg, {
                            title: 'Failed to fetch user profile',
                            ttl: 15000
                        });
                    }
                )
            };

            self.getInstances = function() {
                FsIntegrationService.getInstances(self.projectId).then(
                    function(success) {
                        self.dbInstances = success.data.items;

                        if (self.dbInstances.length > 0) {
                            self.dbInstance = self.dbInstances[0].url;
                        }
                    }, function(error) {
                        growl.error(error.data.errorMsg, {
                            title: 'Failed to fetch Databricks Instances',
                            ttl: 15000
                        });
                    }
                )
            };

            self.getProjectMembers = function() {
                MembersService.query({id: self.projectId}).$promise.then(
                    function(success) {
                        self.projectMembers = success;
                    }, function(error) {
                        growl.error(error.data.errorMsg, {
                            title: 'Failed to fetch project members',
                            ttl: 15000
                        });
                    }
                )
            };

            self.downloadJars = function() {
                FsIntegrationService.downloadJarsToken(self.projectId).then(
                    function(success) {
                        FsIntegrationService.downloadJars(self.projectId, success.data.data);
                    }, function(error) {
                        growl.error(error.data.errorMsg, {
                            title: 'Failed to download clients',
                            ttl: 15000
                        });
                    }
                )
            };

            self.getSparkConfiguration = function() {
                FsIntegrationService.sparkConfiguration(self.projectId).then(
                    function(success) {
                        self.sparkConfiguration = success.data.items;
                    }, function(error) {
                        growl.error(error.data.errorMsg, {
                            title: 'Failed to fetch Spark configuration',
                            ttl: 15000
                        });
                    }
                )
            }

            self.init = function() {
                self.getInstances();
                
                UserService.getRole(self.projectId).then(
                    function(success) {
                        self.dataScientist = success.data.role.toUpperCase() === "DATA SCIENTIST"
                    }, function(error) {
                        growl.error(error.data.errorMsg, {
                            title: 'Failed to fetch user role in project',
                            ttl: 15000
                        });
                    }
                )

                self.getProjectMembers();
                self.getSparkConfiguration();
            };

            self.init();
        }]);

