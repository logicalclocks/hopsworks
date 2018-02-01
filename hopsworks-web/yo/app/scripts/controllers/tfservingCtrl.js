/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * Controller for the kafka page.
 */
'use strict';

angular.module('hopsWorksApp')
    .controller('TfservingCtrl', ['$scope', '$routeParams', 'growl', 'TfServingService', '$location', 'ModalService', '$interval', 'StorageService', '$mdSidenav',
        function($scope, $routeParams, growl, TfServingService, $location, ModalService, $interval, StorageService, $mdSidenav) {


            var self = this;
            self.projectId = $routeParams.projectID;
            self.closeAll = false;

            self.servings = [];
            self.newServing = {};

            self.activeTab = 'serving';

            self.showCreateNewServingForm = false;

            self.selectedIndex = -1;

            self.jobClickStatus = [];

            self.log = '';
            self.logId = -1;
            self.logModel = '';

            $scope.pageSize = 10;
            $scope.sortKey = 'creationTime';
            $scope.reverse = true;

            self.clickTab = function(tab) {
                if (tab === 'serving') {
                    self.activeTab = 'serving'
                } else {
                    self.activeTab = 'models'
                }

            };

            self.createNewServingMode = false;

            //Set some (semi-)constants
            self.selectFileRegexes = {
                "PB": /.pb\b/
            };

            self.selectFileErrorMsgs = {
                "PB": "Please select a .pb file. It should be in Models/{model_name}/{version}/model.pb"
            };

            this.selectFile = function(reason, serving, op) {
                ModalService.selectFile('lg', self.selectFileRegexes[reason.toUpperCase()], self.selectFileErrorMsgs[reason.toUpperCase()]).then(
                    function(success) {
                        if(op === 'NEW') {
                            self.onFileSelected(reason, "hdfs://" + success, 'NEW', serving);
                        }

                        if(op === 'VERSION') {
                            self.onFileSelected(reason, "hdfs://" + success, 'VERSION', serving);
                        }

                    },
                    function(error) {
                        //The user changed their mind.
                    });
            };

            self.onFileSelected = function(reason, path, op, serving) {
                var re = /(?:\.([^.]+))?$/;
                var extension = re.exec(path)[1];
                var file = path.replace(/^.*[\\\/]/, '')
                var fileName = file.substr(0, file.lastIndexOf('.'))
                switch (extension.toUpperCase()) {
                case "PB":
                    if(op === 'NEW') {
                        self.newServing.hdfsModelPath = path
                    }

                    if(op === 'VERSION') {

                        serving.hdfsModelPath = path

                        TfServingService.changeVersion(self.projectId, serving).then(
                                                                                        function(success) {
                                                                                            self.getAllServings(self.projectId);
                                                                                        },
                                                                                        function(error) {
                                                                                            growl.error(error.data.errorMsg, {
                                                                                                title: 'Error',
                                                                                                ttl: 15000
                                                                                            });
                                                                                        });

                    }


                    break;
                default:
                    growl.error("Invalid file type selected: " + extension + ", but expected .pb", {ttl: 10000});
                    break;
                }
            };


            self.showCreateServingForm = function() {
                self.showCreateNewServingForm = true
                self.createNewServingMode = true
            };

            self.getAllServings = function() {
                TfServingService.getAllServings(self.projectId).then(
                    function(success) {
                        self.servings = success.data;
                    },
                    function(error) {
                        growl.error(error.data.errorMsg, {
                            title: 'Error',
                            ttl: 15000
                        });
                    });
            };


            self.containsServingStatus = function(status) {

                for (var j = 0; j < self.servings.length; j++) {
                    if (self.servings[j].status === status) {
                        return true;
                    }
                }

                return false;
            };

            self.getAllServings(self.projectId)

            self.createNewServing = function() {
                TfServingService.createNewServing(self.projectId, self.newServing).then(
                    function(success) {
                        self.getAllServings(self.projectId);
                        self.showCreateNewServingForm = false;
                        self.createNewServingMode = false;

                    },
                    function(error) {
                        if (error.data !== undefined) {
                            growl.error(error.data.errorMsg, {
                                title: 'Error',
                                ttl: 15000
                            });
                        }
                    });
            };

            self.deleteServing = function(serving) {

                TfServingService.deleteServing(self.projectId, serving.id).then(
                    function(success) {
                        if(serving.id === self.logId) {
                        self.logId = -1;
                        self.log = '';
                        self.logModel = '';
                        }
                        self.getAllServings(self.projectId);
                    },
                    function(error) {
                        growl.error(error.data.errorMsg, {
                            title: 'Error',
                            ttl: 15000
                        });
                    });
            };

            self.showLogs = function(serving) {
                TfServingService.getLogs(self.projectId, serving.id).then(
                    function(success) {
                        self.logId = serving.id;
                        self.log = success.data.stdout;
                        self.logModel = serving.modelName;
                    },
                    function(error) {
                    self.log = 'Internal error: could not get logs';
                        growl.error(error.data.errorMsg, {
                            title: 'Error',
                            ttl: 15000
                        });
                    });
            };

            self.startServing = function(serving) {

                serving.status = 'Starting'

                TfServingService.startServing(self.projectId, serving.id).then(
                    function(success) {
                        self.getAllServings(self.projectId);
                    },
                    function(error) {
                    self.getAllServings(self.projectId);
                        if (error.data !== undefined) {
                            growl.error(error.data.errorMsg, {
                                title: 'Error',
                                ttl: 15000
                            });
                        }
                    });
            };

            self.stopServing = function(serving) {
                TfServingService.stopServing(self.projectId, serving.id).then(
                    function(success) {
                        self.logId = -1;
                        self.log = '';
                        self.logModel = '';
                        self.getAllServings(self.projectId)

                    },
                    function(error) {
                        if (error.data !== undefined) {
                            growl.error(error.data.errorMsg, {
                                title: 'Error',
                                ttl: 15000
                            });
                        }
                    });
            };

            $scope.$on('$destroy', function () {
                          $interval.cancel(self.poller);
                        });

            var startPolling = function () {
                self.poller = $interval(function () {
                    self.getAllServings();
               }, 10000);
            };
            //startPolling();
        }
    ]);