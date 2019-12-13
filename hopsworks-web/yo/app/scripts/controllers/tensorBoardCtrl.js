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
    .controller('TensorBoardCtrl', ['$scope', '$timeout', 'growl', 'ProjectService', 'TensorBoardService', '$interval',
        '$routeParams', '$route', '$sce', '$window',
        function($scope, $timeout, growl, ProjectService, TensorBoardService, $interval,
            $routeParams, $route, $sce, $window) {

            var self = this;

            self.appIds = [{}];
            self.ui = "";
            self.id = "";
            self.current = "";
            self.projectId = $routeParams.projectID;
            self.tb = "";
            self.reloadedOnce = false;
            self.mlId = "";

            self.loading = false;
            self.loadingHeadingText = "";
            self.loadingBottomText = "";

            var startLoading = function(headingText, bottomText) {
                self.loading = true;
                self.loadingHeadingText = headingText;
                self.loadingBottomText = bottomText;
            };
            var stopLoading = function() {
                self.loading = false;
                self.loadingHeadingText = "";
                self.loadingBottomText = "";
            };

            self.start = function(mlId) {

                startLoading("Starting TensorBoard...", "Visualizations are loaded in the background and may take a while to appear.");

                TensorBoardService.startTensorBoard(self.projectId, mlId).then(
                    function(success) {
                        self.mlId = mlId;
                        self.tb = success.data;
                        self.ui = "/hopsworks-api/tensorboard/experiments/" + self.tb.endpoint + "/";
                        self.newWindow();
                        stopLoading();
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

           self.newWindow = function () {
             $window.open(self.ui, '_blank');
           };
        }
    ]);