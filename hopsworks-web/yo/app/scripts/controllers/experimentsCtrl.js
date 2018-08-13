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

'use strict';
/*
 * Controller for the job UI dialog.
 */
angular.module('hopsWorksApp')
    .controller('ExperimentsCtrl', ['$scope', '$timeout', 'growl', 'JobService', 'TensorBoardService', '$interval',
        '$routeParams', '$route', '$sce', '$window',
        function($scope, $timeout, growl, JobService, TensorBoardService, $interval,
            $routeParams, $route, $sce, $window) {

            var self = this;
            self.appIds = [];
            self.ui = "";
            self.id = "";
            self.current = "";
            self.projectId = $routeParams.projectID;
            self.tb = "";
            self.reloadedOnce = false;

            self.loading = false;
            self.loadingText = "";

            var startLoading = function(label) {
                self.loading = true;
                self.loadingText = label;
            };
            var stopLoading = function() {
                self.loading = false;
                self.loadingText = "";
            };


            var tbRunning = function() {
                TensorBoardService.running(self.projectId).then(
                    function(success) {
                        self.tb = success.data;
                    },
                    function(error) {
                        if (error.data !== undefined && error.status !== 404) {
                        self.tb = "";
                            growl.error(error.data.errorMsg, {
                                title: 'Error fetching TensorBoard status',
                                ttl: 15000
                            });
                        }
                    });
            };

            tbRunning();

            self.viewTB = function() {
                    self.tbUI();
                TensorBoardService.view(self.projectId).then(

                    function(success) {
                    },
                    function(error) {
                    });
            };

            self.startTB = function() {
                     if(self.id === '' || !self.id.startsWith('application')) {
                            growl.error("Please specify a valid experiment _id", {
                                                                    title: 'Invalid argument',
                                                                    ttl: 15000
                                                                });
                                            return;
                                        }

            startLoading("Starting TensorBoard...");


                TensorBoardService.start(self.projectId, self.id).then(
                    function(success) {
                    self.tb = success.data;
                    self.tbUI();
                    self.id = "";
                    },
                    function(error) {
                    stopLoading();
                        growl.error(error.data.errorMsg, {
                            title: 'Error starting TensorBoard',
                            ttl: 15000
                        });
                    });
            };


            self.tbUI = function() {
                startLoading("Loading TensorBoard...");
                self.ui = "/hopsworks-api/tensorboard/experiments/" + self.tb.endpoint + "/";
                self.current = "tensorboardUI";

                self.reloadedOnce = false;
                var iframe = document.getElementById('ui_iframe');
                if (iframe === null) {
                    stopLoading();
                } else {
                    iframe.onload = function() {
                        if(!self.reloadedOnce) {
                            self.reloadedOnce = true;
                            self.refresh();
                        } else {
                            stopLoading();
                            self.reloadedOnce = false;
                            }
                    };
                }
                if (iframe !== null) {
                    iframe.src = $sce.trustAsResourceUrl(self.ui);
                }
                self.reloadedOnce = false;
            };

            self.hitEnter = function (event) {
                          var code = event.which || event.keyCode || event.charCode;
                          if (angular.equals(code, 13)) {
                          self.startTB();
                          }
                        };

            self.kibanaUI = function() {

                startLoading("Loading Experiments Overview...");
                JobService.getProjectName(self.projectId).then(
                    function(success) {
                        var projectName = success.data;
                        self.ui = "/hopsworks-api/kibana/app/kibana?projectId=" + self.projectId + "#/dashboard/demo_tensorflow_admin000_experiments_summary-dashboard?_g=" +
                        "(refreshInterval:('$$hashKey':'object:161',display:'5%20seconds',pause:!f,section:1,value:5000),time:(from:now-15m,mode:quick,to:now))&_a=" +
                        "(description:'A%20summary%20of%20all%20experiments%20run%20in%20this%20project',filters:!(),fullScreenMode:!f,options:(darkTheme:!f,hidePanelTitles:!" +
                        "f,useMargins:!t),panels:!((gridData:(h:9,i:'1',w:12,x:0,y:0),id:" + projectName.toLowerCase() + "_experiments_summary-search,panelIndex:'1',type:search,version:'6.2.3'))," +
                        "query:(language:lucene,query:''),timeRestore:!f,title:'Experiments%20summary%20dashboard',viewMode:view)"

                        var iframe = document.getElementById('ui_iframe');
                        if (iframe !== null) {
                            iframe.src = $sce.trustAsResourceUrl(self.ui);
                        }

                        self.current = "kibanaUI";
                        stopLoading();
                    },
                    function(error) {
                        growl.error(error.data.errorMsg, {
                            title: 'Error fetching project name',
                            ttl: 15000
                        });
                        stopLoading();
                    });
            };

            self.stopTB = function() {

                TensorBoardService.stop(self.projectId).then(
                    function(success) {
                    self.tb="";
                    $route.reload();
                    },
                    function(error) {
                        growl.error(error.data.errorMsg, {
                            title: 'Error stopping TensorBoard',
                            ttl: 15000
                        });
                    });
            };

            var init = function() {
                if(self.tb === '') {
                    self.kibanaUI();
                } else {
                    self.tbUI();
                }
            }

            init();

            self.openUiInNewWindow = function() {
                $window.open(self.ui, '_blank');
            };

            self.refresh = function() {
                var ifram = document.getElementById('ui_iframe');
                if (ifram !== null) {
                    ifram.contentWindow.location.reload();
                }
            };

            var startPolling = function() {
                self.poller = $interval(function() {
                    TensorBoardService.running(self.projectId).then(
                        function(success) {
                            self.tb = success.data;
                        },
                        function(error) {
                            if(self.tb !== "") {
                                self.kibanaUI();
                            }
                            self.tb = "";
                            if (error.data !== undefined && error.status !== 404) {
                                growl.error(error.data.errorMsg, {
                                    title: 'Error fetching TensorBoard status',
                                    ttl: 10000
                                });
                            }
                        });

                }, 60000);
            };
            startPolling();

            angular.module('hopsWorksApp').directive('bindHtmlUnsafe', function($parse, $compile) {
                return function($scope, $element, $attrs) {
                    var compile = function(newHTML) {
                        newHTML = $compile(newHTML)($scope);
                        $element.html('').append(newHTML);
                    };
                    var htmlName = $attrs.bindHtmlUnsafe;
                    $scope.$watch(htmlName, function(newHTML) {
                        if (!newHTML)
                            return;
                        compile(newHTML);
                    });
                };
            });

        }
    ]);