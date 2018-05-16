/*
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
 *
 */
'use strict';
/*
 * Controller for the file selection dialog.
 */
angular.module('hopsWorksApp')
    .controller('SelectEnvYmlCtrl', ['$uibModalInstance', 'growl', 'regex', 'errorMsg',
        function($uibModalInstance, growl, regex, errorMsg) {

            var self = this;

            self.selectedFilePath;
            self.environmentYmlDef = {};
            self.envMode = "";
            self.isDir = false;


            /**
             * Close the modal dialog.
             * @returns {undefined}
             */
            self.close = function() {
                $uibModalInstance.dismiss('cancel');
            };

            /**
             * Select a file.
             * @param {type} filepath
             * @param {type} isDirectory
             * @returns {undefined}
             */
            self.select = function(filepath, isDirectory) {
                self.selectedFilePath = filepath;
                self.isDir = isDirectory;
            };

            self.confirmSelection = function(pythonDepsCtrl, isDirectory) {

                var singleMachineType = ((pythonDepsCtrl.environmentTypes['CPU'] && !pythonDepsCtrl.environmentTypes['GPU']) || (!pythonDepsCtrl.environmentTypes['CPU'] && pythonDepsCtrl.environmentTypes['GPU']));

                if (singleMachineType && self.environmentYmlDef.allYmlPath === "") {
                    growl.error("Please select a .yml file for your Anaconda environment.", {
                        title: "No .yml selected",
                        ttl: 15000
                    });
                } else if (!singleMachineType && (self.environmentYmlDef.cpuYmlPath === "" || self.environmentYmlDef.gpuYmlPath === "")) {
                    growl.error("Please select a CPU and GPU .yml file for your Anaconda environment.", {
                        title: "Select .yml files",
                        ttl: 15000
                    });
                } else {
                    $uibModalInstance.close(self.environmentYmlDef);
                }
            };

            self.click = function(datasetsCtrl, file, isDirectory) {
                if (file.dir) {
                    self.select(file.path, true);
                    datasetsCtrl.openDir(file);
                } else {
                    self.select(file.path, false);
                }
            };




            self.selectEnvYml = function(pythonDepsCtrl, datasetsCtrl, file) {
                if (file.dir) {
                    self.select(file.path, true);
                    datasetsCtrl.openDir(file);
                } else {
                    self.select(file.path, false);
                    if (self.envMode === 'CPU') {
                        self.environmentYmlDef.cpuYmlPath = file.path;
                        self.envMode = "";
                    } else if (self.envMode === 'GPU') {
                        self.environmentYmlDef.gpuYmlPath = file.path;
                        self.envMode = "";
                    } else if (self.envMode === 'ALL') {
                        self.environmentYmlDef.allYmlPath = file.path;
                        self.envMode = "";
                    }
                }
            };

            self.back = function(datasetsCtrl) {
                if (datasetsCtrl.pathArray.length <= 1) {
                    datasetsCtrl.getAllDatasets();
                } else {
                    datasetsCtrl.back();
                }
            };

        }
    ]);