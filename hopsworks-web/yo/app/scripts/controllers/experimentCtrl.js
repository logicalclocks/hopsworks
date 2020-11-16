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

'use strict';
/**
 * Controller for experiments service.
 */
angular.module('hopsWorksApp')
    .controller('ExperimentCtrl', ['$scope', '$timeout', 'growl', '$window', 'UserService', 'ModalService', 'ProjectService', 'ProjectMembershipService', 'ExperimentService', 'TensorBoardService', 'DataSetService', 'StorageService', 'FeaturestoreService', 'ProvenanceService', '$interval',
        '$routeParams', '$route', '$sce', 'JobService', '$location',
        function($scope, $timeout, growl, $window, UserService, ModalService, ProjectService, ProjectMembershipService, ExperimentService, TensorBoardService, DataSetService, StorageService, FeaturestoreService, ProvenanceService, $interval,
            $routeParams, $route, $sce, JobService, $location) {

            var self = this;

            self.deleted = {}
            self.maxPaginationLinks = 10;
            self.pageSize = 10;
            self.currentPage = 1;
            self.totalItems = 0;

            self.currentResultPage = {};

            self.sortType = 'start';
            self.orderBy = 'desc';
            self.reverse = true;

            self.resultSortType = {};
            self.resultTotalItems = {};
            self.resultOrderBy = {};
            self.resultsReverse = {};

            self.inModalView = false;

            self.modelExperimentId;

            self.projectId = $routeParams.projectID;

            var dataSetService = DataSetService(self.projectId);

            self.memberSelected = {};

            self.experiments = [];

            self.loading = false;
            self.loadingText = "";

            self.loaded = false;

            self.experimentsFilter = "";

            self.query = "";

            self.membersList = [];
            self.members = [];
            self.userEmail = "";

            self.updating = false;
            self.expandExperiment = {};
            self.expanding = {};

            self.experimentsToDate = new Date();
            self.experimentsToDate.setMinutes(self.experimentsToDate.getMinutes() + 60*24);
            self.experimentsFromDate = new Date();
            self.experimentsFromDate.setMinutes(self.experimentsToDate.getMinutes() - 60*24*30);

            var startLoading = function(label) {
                self.loading = true;
                self.loadingText = label;
            };
            var stopLoading = function() {
                self.loading = false;
                self.loadingText = "";
            };

            self.experimentsOrder = function () {
                if (self.reverse) {
                    self.orderBy = "desc";
                } else {
                    self.orderBy = "asc";
                }
            };

            self.modelExperimentId = StorageService.recover(self.projectId + "_experiment");
            if(self.modelExperimentId) {
                StorageService.remove(self.projectId + "_experiment");
                self.expandExperiment[self.modelExperimentId] = true;
            }

            self.sortByExperiments = function(type) {
                if(self.sortType !== type) {
                    self.reverse = true;
                } else {
                    self.reverse = !self.reverse; //if true make it false and vice versa
                }
                self.sortType = type;
                self.experimentsOrder();
                self.getExperiments();
            };

            self.sortByResults = function(type, experiment) {
                if(self.resultSortType[experiment.id] !== type) {
                    self.resultsReverse[experiment.id] = true;
                } else {
                    self.resultsReverse[experiment.id] = !self.resultsReverse[experiment.id]; //if true make it false and vice versa
                }
                self.resultSortType[experiment.id] = type;
                self.getResults(experiment, true);
            };

            self.deleteExperiment = function (id) {
                ModalService.confirm('sm', 'Delete Experiment?',
                    'WARNING: This will remove the directory in your Experiments dataset containing your Experiment output, it will also be removed from this view. This action can not be undone.')
                    .then(function (success) {
                        startLoading("Deleting Experiment...");
                        ExperimentService.deleteExperiment(self.projectId, id).then(
                            function(success) {
                                  stopLoading();
                                  self.deleted[id] = true;
                                  self.getExperiments();
                            },
                            function(error) {
                                stopLoading();
                                if (typeof error.data.usrMsg !== 'undefined') {
                                    growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 8000});
                                } else {
                                    growl.error("", {title: error.data.errorMsg, ttl: 8000});
                                }
                            });
                    }, function (error) {
                    });
            };

            self.viewExperiment = function (experiment) {
                self.inModalView = true;
                ModalService.viewExperimentInfo('lg', self.projectId, experiment).then(
                function (success) {
                    self.inModalView = false;
                    self.getExperiments();
                }, function (error) {
                    self.inModalView = false;
                    self.getExperiments();
                });
            };

            self.viewMonitor = function (experiment) {
                  var isLivy;
                  if(experiment.jobName) {
                    isLivy = false;
                  } else {
                    isLivy = true;
                  }

                  JobService.getExecutionUI(self.projectId, experiment.appId, self.isLivy).then(
                      function (success) {
                          self.sessions = success.data;
                          for(var i = 0; i < success.data.length; i++) {
                              if(success.data[i].name === 'spark') {
                                    $window.open(success.data[i]['url'], '_blank');
                                    break;
                              }
                          }
                      }, function (error) {

                          if (typeof error.data.usrMsg !== 'undefined') {
                              growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 8000});
                          } else {
                              growl.error("", {title: error.data.errorMsg, ttl: 8000});
                          }
                          stopLoading();

                  });
            };

            self.buildQuery = function() {
                if(self.modelExperimentId) {
                    self.query = '?filter_by=id_eq:' + self.modelExperimentId;
                    return;
                }
                var offset = self.pageSize * (self.currentPage - 1);
                self.query = '';
                if(self.experimentsFilter !== "") {
                    self.query = self.query + '&filter_by=name_like:' + self.experimentsFilter;
                }
                self.query = self.query + '&filter_by=date_start_lt:' + self.experimentsToDate.toISOString().replace('Z','');
                self.query = self.query + '&filter_by=date_start_gt:' + self.experimentsFromDate.toISOString().replace('Z','');
                if(self.memberSelected.name !== 'All Members') {
                    self.query = self.query + '&filter_by=user:' + self.memberSelected.uid + '&filter_by=user_project:' + self.memberSelected.projectId;
                }
                self.query = self.query + '&sort_by=' + self.sortType + ':' + self.orderBy + '&offset=' + offset + '&limit=' + self.pageSize;
                if(self.query.length > 0) {
                    self.query = '?' + self.query.substring(1, self.query.length);
                }
            };

            self.getExperiments = function(loadingText) {
                if(loadingText) {
                    startLoading(loadingText);
                }
                self.buildQuery();
                self.updating = true;
                ExperimentService.getAll(self.projectId, self.query).then(
                    function(success) {
                        if(loadingText) {
                            stopLoading();
                        }
                        self.updating = false;
                        if(success.data.items) {
                              for(var i = 0; success.data.items.length > i; i++) {
                                if(success.data.items[i].id in self.deleted) {
                                   success.data.items.splice(i, 1);
                                   success.data.count = success.data.count - 1;
                                }
                              }
                            if(success.data.count !== self.totalItems) {
                                self.experiments = success.data.items;
                            } else {
                            var i=0;
                                self.experiments = self.experiments.slice(0, success.data.items.length)
                                angular.forEach(success.data.items, function (experiment, key) {
                                    if(typeof self.experiments[i] === 'undefined' || self.experiments[i] === null || self.experiments[i] === "") {
                                        self.experiments[i] = {};
                                    } else {
                                        for (var key in self.experiments[i]) {
                                            delete self.experiments[i][key];
                                        }
                                    }
                                    for (var key in experiment) {
                                        self.experiments[i][key] = experiment[key];
                                    }
                                    i++;
                                });
                            }
                            self.totalItems = success.data.count;
                            self.loaded = true;
                            angular.forEach(self.experiments, function (experiment, key) {
                                if(self.expandExperiment[experiment.id]) {
                                    self.getResults(experiment, true);
                                }
                            });
                        } else {
                            self.loaded = true;
                            self.experiments = [];
                            self.totalItems = 0;
                        }
                    },
                    function(error) {
                        if(loadingText) {
                            stopLoading();
                        }
                        self.loaded = true;
                        self.updating = false;
                        if (typeof error.data.usrMsg !== 'undefined') {
                            growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 8000});
                        } else {
                            growl.error("", {title: error.data.errorMsg, ttl: 8000});
                        }
                    });
            };

            /**
             * Helper function for redirecting to another project page
             *
             * @param serviceName project page
             */
            self.goToExperiment = function (experiment_id) {
                $location.path('project/' + self.projectId + '/datasets/Experiments/' + experiment_id);
            };

            self.init = function () {
              UserService.profile().then(
                function (success) {
                  self.userEmail = success.data.email;
                  self.getMembers();
                },
                function (error) {
                    if (typeof error.data.usrMsg !== 'undefined') {
                        growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 8000});
                    } else {
                        growl.error("", {title: error.data.errorMsg, ttl: 8000});
                    }
                });
            };

            self.updateMember = function() {
                //update experiments for member
                self.getExperiments('Loading Experiments...');
            };

            self.getMembers = function () {
                ProjectMembershipService.getDatasetMembers(self.projectId, 'Experiments', 'DATASET').then(
                function (success) {
                    if(success.data.length > 0) {
                        self.membersList = [];
                        success.data.forEach(function (member) {
                            self.membersList.push({'name': member.user.fname + ' ' + member.user.lname + ':' + member.project.name, 'uid': member.user.uid, 'email': member.user.email, 'projectId': member.project.id});
                        });
                        self.membersList.push({'name': 'All Members', 'projectId': parseInt(self.projectId)});
                        self.memberSelected = {'name': 'All Members', 'projectId': parseInt(self.projectId)};
                  }
                  self.getExperiments('Loading Experiments...');
                },
                function (error) {
                    if (typeof error.data.usrMsg !== 'undefined') {
                        growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 8000});
                    } else {
                        growl.error("", {title: error.data.errorMsg, ttl: 8000});
                    }
                });
            };

            self.init();

            self.getNewExperimentPage = function() {
                self.expandExperiment = {};
                self.getExperiments();
            };

            $scope.$on('$destroy', function () {
              $interval.cancel(self.poller);
            });

            var startPolling = function () {
              self.poller = $interval(function () {
                if(!self.updating && !self.inModalView) {
                  self.getExperiments();
                }
              }, 15000);
            };
            startPolling();

            self.hp_headers = {};
            self.metric_headers = {};
            self.all_headers = {};
            self.experimentResults = {};

            self.initExpansionTable = function(experiment) {
                var experimentId = experiment.id;
                self.experimentResults[experimentId] = [];

                if (experiment['results'] && experiment['results']['count'] > 0) {
                    var results = experiment['results']['combinations'];
                    if (results) {
                        self.hp_headers[experimentId] = [];
                        if (results[0]['parameters']) {
                            for(var key in results[0]['parameters']) {
                                self.hp_headers[experimentId].push(key)
                            }
                        }
                        self.metric_headers[experimentId] = [];
                        if(results[0]['outputs']) {
                            for(var key in results[0]['outputs']) {
                                self.metric_headers[experimentId].push(key);
                            }
                        }
                        self.all_headers[experimentId] = self.hp_headers[experimentId].concat(self.metric_headers[experimentId]);

                        for (var i = 0; i < results.length; i++) {
                            var tmp = []
                            if (results[i]['parameters']) {
                                for(var key in results[i]['parameters']) {
                                    tmp.push({
                                        'data': results[i]['parameters'][key],
                                        'file': false
                                    })
                                }
                            }
                            if (results[i]['outputs']) {
                                for(var key in results[i]['outputs']) {
                                    var is_file = String(results[i]['outputs'][key]).indexOf('/') > -1;
                                    tmp.push({
                                        'data': results[i]['outputs'][key],
                                        'file': is_file
                                    });
                                }
                            }
                            if(!self.experimentResults[experimentId]) {
                                self.experimentResults[experimentId] = [];
                            }
                            self.experimentResults[experimentId].push({
                                'row': tmp
                            });
                        }
                        self.getSourceTDLinks(experiment);
                    }
                }
            };

            self.viewFile = function(filePath) {
                ModalService.filePreview('xl', filePath.replace(/^.*[\\\/]/, ''), filePath, self.projectId, "head").then(
                    function(success) {},
                    function(error) {});
            };

            self.downloadFile = function(filePath) {
              dataSetService.getDownloadToken(filePath, "DATASET").then(
                      function (success) {
                        var token = success.data.data.value;
                        dataSetService.download(filePath, token, "DATASET");
                      },function (error) {
                        growl.error("", {
                            title: error.data.errorMsg,
                            ttl: 8000
                      });
              });
            };

            self.getResults = function(experiment, expand) {
                if(!self.expandExperiment[experiment.id] || expand) {
                    self.expanding[experiment.id] = true;
                    var query = self.buildResultsQuery(experiment);
                    ExperimentService.get(self.projectId, experiment.id, query).then(
                        function(success) {
                            self.expandExperiment[experiment.id] = true;
                            self.expanding[experiment.id] = false;
                            self.initExpansionTable(success.data);
                            if(success.data.results.count) {
                              self.resultTotalItems[experiment.id] = success.data.results.count;
                            }
                        },
                        function(error) {
                            self.expandExperiment[experiment.id] = false;
                            self.expanding[experiment.id] = false;
                            if (typeof error.data.usrMsg !== 'undefined') {
                                growl.error(error.data.usrMsg, {
                                    title: error.data.errorMsg,
                                    ttl: 8000
                                });
                            } else {
                                growl.error("", {
                                    title: error.data.errorMsg,
                                    ttl: 8000
                                });
                            }
                        });
                } else if(!expand) {
                    delete self.expanding[experiment.id];
                    delete self.expandExperiment[experiment.id];
                }
            };

            self.goToDirectory = function (path) {
                $location.path('project/' + self.projectId + '/datasets/' + path);
            };

            self.goToModel = function(model) {
                if(model) {
                    var splitIndex = model.lastIndexOf('_');
                    var modelName = model.substr(0, splitIndex);
                    var modelVersion = model.substr(splitIndex + 1, model.length);
                    StorageService.store(self.projectId + "_model", model);
                    $location.path('project/' + self.projectId + '/models');
                }
            };

            self.buildModelLink = function(model) {
                var splitIndex = model.lastIndexOf('_')
                var modelName = model.substr(0, splitIndex)
                var modelVersion = model.substr(splitIndex + 1, model.length)
                return 'Models/' + modelName + '/' + modelVersion;
            }

            self.buildResultsQuery = function(experiment) {

                if (self.resultsReverse[experiment.id]) {
                    self.resultOrderBy[experiment.id] = "desc";
                } else {
                    self.resultOrderBy[experiment.id] = "asc";
                }

                if(!self.currentResultPage[experiment.id]) {
                    self.currentResultPage[experiment.id] = 1;
                }

                var offset = self.pageSize * (self.currentResultPage[experiment.id] - 1);
                var sortBy = "";
                var query = "";

                if(!self.resultTotalItems[experiment.id]) {
                    var optKey = experiment.optimizationKey;
                    if(optKey) {
                        self.resultSortType[experiment.id] = optKey;
                        if(experiment.direction.toUpperCase() === 'MAX') {
                            self.resultsReverse[experiment.id] = true;
                            self.resultOrderBy[experiment.id] = 'desc';
                        } else if(experiment.direction.toUpperCase() === 'MIN') {
                            self.resultsReverse[experiment.id] = false;
                            self.resultOrderBy[experiment.id] = 'asc';
                        }
                        sortBy = ';sort_by=' + self.resultSortType[experiment.id] + ':' + self.resultOrderBy[experiment.id] + ')';
                    } else {
                        sortBy = ')'
                    }
                } else {
                    if(self.resultSortType[experiment.id] && self.resultOrderBy[experiment.id]) {
                        sortBy = ';sort_by=' + self.resultSortType[experiment.id] + ':' + self.resultOrderBy[experiment.id] + ')';
                    } else {
                        sortBy = ')'
                    }
                }

                return "?expand=results(offset=" + offset + ";limit=" + self.pageSize + sortBy
            };

            self.tdLinkErrorPrint = function(error) {
                if (typeof error.data.usrMsg !== 'undefined') {
                    growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 8000});
                } else {
                    growl.error('', {title: error.data.errorMsg, ttl: 8000});
                }
                self.tdLink = {};
            };

            self.hasTDSource = function() {
                return self.tdLink !== undefined && self.tdLink.name !== undefined;
            };

            self.goToTrainingDataset = function(link) {
                $location.search('');
                $location.path('/project/' + link.projId + '/featurestore');
                $location.search('featurestore', link.fsId);
                $location.search('trainingDataset', link.name);
                $location.search('version',link.version);
            };

            self.getLinkInfo = function(link) {
                /** get project id from project name */
                ProjectService.getProjectInfo({projectName: link.projName}).$promise.then(
                    function (success1) {
                        link.projId = success1.projectId;
                        /** get featurestore of fg */
                        FeaturestoreService.getFeaturestores(link.projId).then(
                            function (success2) {
                                /** get the project's main featurestore */
                                const fs = success2.data.filter(function(fs) {
                                    return fs.projectName === link.projName;
                                });
                                if (fs.length === 1) {
                                    link.fsId = fs[0].featurestoreId;
                                } else {
                                    console.log('featurestore not in project');
                                    growl.error('featurestore not in project', {title: 'provenance error', ttl: 8000});
                                }
                            }, self.tdLinkErrorPrint);
                    }, self.tdLinkErrorPrint);
            };

            self.getInArtifacts = function(name, version, inType, outType, linkInfoFunc, link) {
                ProvenanceService.getAppLinks(self.projectId, {outArtifactName: name, outArtifactVersion: version, inArtifactType: inType, outArtifactType: outType}).then(
                    function(success) {
                        if(success.data.items !== undefined && success.data.items.length === 1) {
                            if(success.data.items[0].in.entry.length === 1) {
                                var versionSplitIndex = success.data.items[0].in.entry[0].value.mlId.lastIndexOf('_');
                                link.name = success.data.items[0].in.entry[0].value.mlId.substring(0, versionSplitIndex);
                                link.version = parseInt(success.data.items[0].in.entry[0].value.mlId.substring(versionSplitIndex+1));
                                link.projName = success.data.items[0].in.entry[0].value.projectName;
                                link.appId = success.data.items[0].in.entry[0].value.appId;
                                linkInfoFunc(link);
                            }
                        }
                    }, self.tdLinkErrorPrint);
            };

            self.getSourceTDLinks = function (experiment) {
                if(self.tdLink !== undefined && self.tdLink.experiment.id === experiment.id) {
                    return;
                }
                self.tdLink = {};
                self.tdLink.experiment = experiment;
                var splitIndex = experiment.id.lastIndexOf('_')
                var name = experiment.id.substr(0, splitIndex)
                var version = experiment.id.substr(splitIndex + 1)
                /** td <- app <- experiment */
                self.getInArtifacts(name, version, 'TRAINING_DATASET', 'EXPERIMENT', self.getLinkInfo, self.tdLink);
            };
        }
    ]);