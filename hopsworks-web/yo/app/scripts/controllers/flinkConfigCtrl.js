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

/**
 * Controller for the serving page.
 */
'use strict';

angular.module('hopsWorksApp')
    .controller('FlinkConfigCtrl', ['$scope', '$routeParams', '$route',
        'growl', 'ModalService', '$interval', 'JupyterService', 'StorageService', '$location', '$timeout',
        function($scope, $routeParams, $route, growl, ModalService, $interval, JupyterService,
                 StorageService, $location, $timeout) {

            var self = this;
            self.loc = $location.url().split('#')[0];


            $scope.$watch('jobConfig', function(jobConfig, oldConfig) {
                if(jobConfig) {
                    self.jobConfig = jobConfig;
                }
            });

            $scope.$watch('settings', function(settings, oldSettings) {
                self.settings = settings;
            });


            //Set some (semi-)constants
            self.selectFileRegexes = {
                "JAR": /.jar\b/,
                "PY": /.py\b/,
                "FILES": /[^]*/,
                "ZIP": /.zip\b/,
                "TGZ": /.zip\b/
            };
            self.selectFileErrorMsgs = {
                "JAR": "Please select a JAR file.",
                "PY": "Please select a Python file.",
                "ZIP": "Please select a zip file.",
                "TGZ": "Please select a tgz file.",
                "FILES": "Please select a file."
            };



            self.autoExpand = function(e) {
                var element = typeof e === 'object' ? e.target : document.getElementById(e);
                var scrollHeight = element.scrollHeight; // replace 60 by the sum of padding-top and padding-bottom
                var currentElemHeight = element.style.height.slice(0, -2);
                if(currentElemHeight < scrollHeight) {
                    element.style.height = scrollHeight + "px";
                }
            };
        }
    ]);