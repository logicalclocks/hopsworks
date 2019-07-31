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
 *
 */'use strict';
/*
 * Controller for the job UI dialog.
 */
angular.module('hopsWorksApp')
    .controller('RStudioCtrl', ['$scope', '$timeout', 'growl', 'ProjectService', '$interval',
        '$routeParams', '$route', '$sce', '$window', 'RStudioService',
        function($scope, $timeout, growl, ProjectService, $interval, $routeParams, $route, $sce, $window, RStudioService) {

            var self = this;
            self.appIds = [];
            self.ui = "";
            self.id = "";
            self.current = "";
            self.projectId = $routeParams.projectID;
            self.url = "";
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

//http://127.0.0.1:8787


            self.viewRStudio = function() {
                RStudioService.getRStudioUrl(self.projectId).then(
                    function(success) {
                        self.url = success.data;
                        self.rstudioUI();
                    },
                    function(error) {
                        if (error.data !== undefined && error.status === 404) {
                            growl.error("The RStudio was shutdown. Please start a new one.", {
                                title: 'RStudio not running',
                                ttl: 15000
                            });
                        } else {
                            growl.error(error.data.errorMsg, {
                                title: 'Error viewing RStudio',
                                ttl: 15000
                            });
                        }
                    });
            };

            self.startRStudio = function() {
                if (self.id === '' || !self.id.startsWith('application')) {
                    growl.error("Please specify a valid experiment _id", {
                        title: 'Invalid argument',
                        ttl: 15000
                    });
                    return;
                }

                startLoading("Starting RStudio...");


                RStudioService.startRStudio(self.projectId, self.id).then(
                    function(success) {
                        self.url = success.data;
                        console.log(self.url);
                        self.rstudioUI();
                        self.id = "";
                    },
                    function(error) {
                        stopLoading();
                        if (typeof error.data.usrMsg !== 'undefined') {
                            growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 8000});
                        } else {
                            growl.error("", {title: error.data.errorMsg, ttl: 8000});
                        }
                    });
            };


            self.rstudioUI = function() {
                startLoading("Loading RStudio...");
                self.ui = "http://127.0.0.1:8787";
//                self.ui = self.url.endpoint + "/";
                self.current = "rstudioUI";

                self.reloadedOnce = false;
                var iframe = document.getElementById('ui_iframe');
                if (iframe === null) {
                    stopLoading();
                } else {
                    iframe.onload = function() {
                        if (!self.reloadedOnce) {
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
                    iframe.contentWindow.location.reload();

                }
                self.reloadedOnce = false;
            };


            self.stopRStudio = function() {
                RStudioService.stopRStudio(self.projectId).then(
                    function(success) {
                        self.url = "";
                        $route.reload();
                    },
                    function(error) {
                        if (typeof error.data.usrMsg !== 'undefined') {
                            growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 8000});
                        } else {
                            growl.error("", {title: error.data.errorMsg, ttl: 8000});
                        }
                    });
            };

            self.refresh = function() {
                var ifram = document.getElementById('ui_iframe');
                if (ifram !== null) {
                    ifram.contentWindow.location.reload();
                }
            };

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

//            $scope.$on('$destroy', function () {
//               if(self.url !== "") {
//                console.log("Stopping RStudio.");
//                self.stopRStudio();
//               }
//           });

        }
    ]);