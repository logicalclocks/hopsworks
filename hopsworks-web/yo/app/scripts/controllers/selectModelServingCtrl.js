/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
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
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

'use strict';
/*
 * Controller for model selector for serving.
 */
angular.module('hopsWorksApp')
        .controller('SelectModelServingCtrl', ['$scope', '$uibModalInstance', 'growl', 'regex', 'errorMsg',
          function ($scope, $uibModalInstance, growl, regex, errorMsg) {

            var self = this;

            self.isDir = false;

            var selectedFilePath;

            self.selectDisabled = true;
            self.watcherCreated = false;

            var validatePathUI = function(datasetsCtrl) {
                if (self.watcherCreated) {
                    return;
                }

                $scope.$watch(angular.bind(datasetsCtrl, function () {
                    return datasetsCtrl.files; // `this` IS the `this` above!!
                }), function (newVal, oldVal) {

                    // Assume that this is not a serving model directory
                    self.selectDisabled = true;

                    // Check that all the dirs are integers
                    if (newVal.length > 0) {

                        var modelDir = true;
                        newVal.forEach(function (dir) {
                            if (isNaN(dir.name)) {
                                modelDir = false;
                                return;
                            }
                        });

                        if (modelDir) {
                            self.selectDisabled = false;
                        }
                    }
                });

                self.watcherCreated = true;
            };


            /**
             * Close the modal dialog.
             * @returns {undefined}
             */
            self.close = function () {
              $uibModalInstance.dismiss('cancel');
            };

            /**
             * Select a file.
             * @param {type} filepath
             * @param {type} isDirectory
             * @returns {undefined}
             */
            self.select = function (filepath, isDirectory) {
              selectedFilePath = filepath;
              self.isDir = isDirectory;
            };

            self.confirmSelection = function (isDirectory) {
              if (selectedFilePath == null) {
                growl.error("Please select a file.", {title: "No file selected", ttl: 15000});
              } else if (self.isDir !== isDirectory) {
                var msg;
                if (self.isDir) {
                  msg = "You should select a directory."
                } else {
                  msg = "You should select a file."
                }
                growl.error(errorMsg, {title: msg, ttl: 10000});
              } else {
                $uibModalInstance.close(selectedFilePath);
              }
            };

            self.click = function (datasetsCtrl, file, isDirectory) {
              if (file.dir) {
                self.select(file.path, true);
                datasetsCtrl.openDir(file);
                validatePathUI(datasetsCtrl);
              } else {
                self.select(file.path, false);
                if(!isDirectory){
                  self.confirmSelection(false);
                } else {
                  growl.warning("", {title: "Please select a directory", ttl: 5000});
                  self.back();
                }
              }
            };

            self.back = function (datasetsCtrl, projectName) {
              if (datasetsCtrl.pathArray.length <= 1) {
                self.select("/Projects/" + projectName, true);

                datasetsCtrl.getAllDatasets();
              } else {
                var newPathArray = datasetsCtrl.pathArray.slice(0);
                newPathArray.pop();
                self.select("/Projects/" + projectName + "/" + newPathArray.join("/"), true);

                datasetsCtrl.back();
              }
            };

          }]);
