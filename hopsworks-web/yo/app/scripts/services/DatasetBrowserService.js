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

angular.module('hopsWorksApp').service('DatasetBrowserService', ['DataSetService', 'ProjectService', 'growl',
    function(DataSetService, ProjectService, growl) {

        var DatasetBrowserService = function(projectId, type, growlReferenceId) {
            this.projectId = projectId;
            this.projectName;
            this.files = [];
            this.pathFiles = [];
            this.relativePath;
            this.fullPath;
            this.selected = -1;
            this.isDir = true;
            this.working = false;
            this.type = type;
            this.referenceId = growlReferenceId;
            this.pageSize = 20;
            this.currentPage = 1;
            this.filesCount = 0;
            this.datasetService = DataSetService(this.projectId);
            this.init();
        };

        DatasetBrowserService.prototype.init = function() {
            var self = this;
            ProjectService.get({}, { 'id': self.projectId }).$promise.then(
                function(success) {
                    self.projectName = success.projectName;
                    self.getAll();
                },
                function(error) {
                    showError(error, self.referenceId);
                });
        };

        DatasetBrowserService.prototype.getAll = function() {
            var self = this;
            self.working = true;
            self.files = [];
            self.selected = -1;
            self.isDir = true;
            self.currentPage = 1;
            self.datasetService.getAllDatasets(undefined, self.currentPage -1, self.pageSize, ['ID:asc']).then(function(success) {
                self.pathFiles = [];
                self.setFiles(success.data.items);
                self.filesCount = success.data.count;
            }, function(error) {
                showError(error, self.referenceId);
            }).finally(function() {
                self.working = false;
                self.getRelativePath();
                self.getFullPath();
            });
        };

        DatasetBrowserService.prototype.goto = function(file) {
            var self = this;
            self.working = true;
            self.files = [];
            self.selected = -1;
            self.currentPage = 1;
            self.datasetService.getAllDatasets(file.attributes.path, self.currentPage -1, self.pageSize, ['ID:asc']).then(function(success) {
                self.pathFiles.push(file);
                self.setFiles(success.data.items);
                self.filesCount = success.data.count;
            }, function(error) {
                showError(error, self.referenceId);
            }).finally(function() {
                self.working = false;
                self.getRelativePath();
                self.getFullPath();
            });
        };

        DatasetBrowserService.prototype.back = function() {
            if (typeof this.pathFiles === 'undefined' || this.pathFiles.length < 1) {
                return;
            }
            var file = this.pathFiles.pop(); // back
            if (typeof file !== 'undefined' && !file.dir) { // if file pop again to get dir
                this.pathFiles.pop();
            }
            if (this.pathFiles.length < 1) {
                this.getAll();
            } else {
                this.goto(this.pathFiles.pop());
            }
        };

        DatasetBrowserService.prototype.select = function(file, index) {
            if (typeof file === "undefined") {
                return;
            }
            var self = this;
            self.isDir = file.attributes.dir;
            if (!self.isDir) {
                var currentFile = self.getCurrentFile();
                if (!currentFile.attributes.dir) {
                    self.pathFiles.pop();
                }
                self.selected = index;
                self.pathFiles.push(file);
                self.getRelativePath();
                self.getFullPath();
            } else {
                self.goto(file);
            }

        };

        DatasetBrowserService.prototype.getRelativePath = function () {
            if (typeof this.pathFiles === 'undefined' || this.pathFiles.length < 1) {
                this.relativePath = "";
                return this.relativePath;
            }
            var files = angular.copy(this.pathFiles);
            var type = findType(files);
            var file = files.pop();
            var path = getPathArray(file.attributes.path);
            if (type === "DATASET") {
                path.splice(0, 2); // remove /Projects/projectName
            } else {
                path.splice(0, 3); // remove /apps/hive/warehouse
            }
            this.relativePath = path.join('/');
            return this.relativePath;
        };

        DatasetBrowserService.prototype.getFullPath = function () {
            if (typeof this.pathFiles === 'undefined' || this.pathFiles.length < 1) {
                this.fullPath = "/Projects/" + this.projectName;
                return this.fullPath;
            }
            var files = angular.copy(this.pathFiles);
            var file = files.pop();
            this.fullPath = file.attributes.path;
            return this.fullPath;
        };

        DatasetBrowserService.prototype.getCurrentFile = function () {
            var files = angular.copy(this.pathFiles);
            var file = files.pop();
            return file;
        };

        DatasetBrowserService.prototype.setFiles = function (list) {
            if (typeof list === 'undefined' || list.length < 1) {
                return;
            }
            if (this.type === "Dirs") {
                this.files = list.filter(function(file) {
                    return file.dir === true;
                });
            } else {
                this.files = list;
            }
        };

        DatasetBrowserService.prototype.pageChange = function (newPageNumber) {
            var self = this;
            self.currentPage = newPageNumber;
            var current = self.getCurrentFile();
            if (typeof current === "undefined") {
                return;
            }
            var offset = (self.currentPage - 1)*self.pageSize;
            self.datasetService.getAllDatasets(current.attributes.path, offset, self.pageSize, ['ID:asc']).then(function(success) {
                self.setFiles(success.data.items);
                self.filesCount = success.data.count;
            }, function(error) {
                showError(error, self.referenceId);
            }).finally(function() {
                self.working = false;
                self.getRelativePath();
                self.getFullPath();
            });
        };

        function getPathArray(path) {
            if (typeof path === 'undefined') {
                return [];
            }
            return path.split('/').filter(function(el) {
                return el != null && el !== "";
            });
        }

        function findType(files) {
            var i;
            for (i = 0; i < files.length; i++) {
                if (typeof files[i].datasetType !== 'undefined') {
                   return files[i].datasetType;
                }
            }
        }

        function showError(error, referenceId) {
            if (referenceId) {
                var errorMsg = typeof error.data.usrMsg !== 'undefined' ? error.data.usrMsg : '';
                growl.error(errorMsg, { title: error.data.errorMsg, ttl: 5000, referenceId: referenceId });
            }
        }

        return DatasetBrowserService;
    }
]);