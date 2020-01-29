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

angular.module('hopsWorksApp')
        .controller('DatasetsCtrl', ['$scope', '$window', '$mdSidenav', '$mdUtil',
          'DataSetService', 'JupyterService', '$routeParams', 'ModalService', 'growl', '$location',
          'MetadataHelperService', '$rootScope', 'DelaProjectService', 'DelaClusterProjectService', 'UtilsService', 'UserService', '$mdToast',
          'TourService', 'ProjectService',
          function ($scope, $window, $mdSidenav, $mdUtil, DataSetService, JupyterService, $routeParams,
                  ModalService, growl, $location, MetadataHelperService,
                  $rootScope, DelaProjectService, DelaClusterProjectService, UtilsService, UserService, $mdToast, TourService, ProjectService) {

            var self = this;
            var SHARED_DATASET_SEPARATOR = '::';
            var FEATURESTORE_NAME = 'Featurestore.db';
            var HIVEDB_NAME = 'Hive.db';
            var TRAINING_DATASET_NAME = 'Training Datasets';
            var WAREHOUSE_PATH = "/apps/hive/warehouse";
            var PROJECT_PATH = "/Projects";
            var MAX_NUM_FILES = 1000; // the limit on files to keep in memory
            self.sharedDatasetSeparator = SHARED_DATASET_SEPARATOR;
            self.manyFilesLimit = 10000;
            self.working = false;
            //Some variables to keep track of state.
            self.files = []; //A list of files currently displayed to the user.
            self.pagenatedFiles = [];
            self.filesCount = 0;

            self.projectId = $routeParams.projectID; //The id of the project we're currently working in.
            self.basePath = '/project/' + self.projectId + '/datasets';
            self.pathArray; //An array containing all the path components of the current path. If empty: project root directory.
            self.sharedPathArray; //An array containing all the path components of a path in a shared dataset
            self.highlighted;
            self.currentPath = [];//used in dataset browser modal
            self.datasetType = undefined;
            self.parentDS = $rootScope.parentDS;
            self.currentDir = undefined;
            self.tourService = TourService;
            self.tourService.currentStep_TourNine = 7; //Feature store Tour

            // Details of the currently selecte file/dir
            self.selected = null; //The index of the selected file in the files array.
            self.lastSelected = -1;
            self.sharedPath = null; //The details about the currently selected file.
            self.routeParamArray = [];
            $scope.readme = null;
            var dataSetService = DataSetService(self.projectId); //The datasetservice for the current project.
            var delaHopsService = DelaProjectService(self.projectId);
            var delaClusterService = DelaClusterProjectService(self.projectId);

            $scope.all_selected = false;
            self.selectedFiles = {}; //Selected files

            self.isPublic = undefined;
            self.shared = undefined;
            self.status = undefined;

            self.sortWorking = true;
            self.tgState = false;
            self.offset = 0;
            self.view = sessionStorage.getItem("datasetBrowserView");
            if (typeof self.view  === "undefined" || self.view  === '' || self.view === "null" || self.view === null) {
              self.view  = 'list';// default is list view
              sessionStorage.setItem("datasetBrowserView", self.view);
            }

            self.toggleView = function () {
               self.view === 'list'? self.view='grid' : self.view='list';
               sessionStorage.setItem("datasetBrowserView", self.view);
            };

            /**
             * Turn the array <i>pathArray</i> containing, path components, into a path string.
             * @param {type} pathArray
             * @returns {String}
            */
            var getPath = function (pathArray) {
              return pathArray.join("/");
            };

            function pagination(page, itemsPerPage, currentPage) {
                this.page = page;
                this.itemsPerPage = itemsPerPage;
                this.currentPage = currentPage;
                console.assert(this.itemsPerPage % 10 === 0 && this.itemsPerPage < MAX_NUM_FILES,
                     "items per page should be < " + MAX_NUM_FILES + " and a multiple of 10");
            };

            var getPaginationFromSessionStorage = function (id) {
                var value = sessionStorage.getItem(id);
                if (typeof value === "undefined" || value === "null" || value === null) {
                    return new pagination(1, 20, 1);
                }
                try {
                    return JSON.parse(value);
                } catch (e) {
                    return new pagination(1, 20, 1);
                }
            };

            var setPaginationInSessionStorage = function (id, pagination) {
                sessionStorage.setItem(id, JSON.stringify(pagination));
            };

            self.datasetPagination = getPaginationFromSessionStorage("datasetPagination");
            self.datasetBrowserPagination = getPaginationFromSessionStorage("datasetBrowserPagination");

              /**
               * Checks whether a dataset is a Hive database or not (featurestore or regular hive db will match here)
               * @param dataset
               * @returns true if it is a Hive db otherwise false
               */
              self.isHiveDB = function(dataset) {
                  if(dataset.attributes.path.includes("/apps/hive/warehouse")){
                      return true;
                  } else {
                      return false;
                  }
              };

              self.isFeaturestore = function(dataset) {
                  if(dataset.attributes.path.includes(WAREHOUSE_PATH) && dataset.name.includes("_featurestore.db")){
                      return true;
                  }
                  return false;
              };

              self.isHive = function(dataset) {
                  if(dataset.attributes.path.includes(WAREHOUSE_PATH) && dataset.name.includes(".db")){
                      return true;
                  }
                  return false;
              };

              self.isTrainingDatasets = function(dataset) {
                  if(dataset.name.includes("_Training_Datasets")) {
                      return true;
                  }
                  return false;
              };

              var isSystemFile = function (file) {
                  return file.attributes.path.startsWith(WAREHOUSE_PATH)
                      || file.attributes.path.includes("_Training_Datasets") || file.attributes.path.includes("Logs")
                      || file.attributes.path.includes("Jupyter") || file.attributes.path.includes("Models")
                      || file.attributes.path.includes("Resources") || file.attributes.path.includes("Experiments")
                      || file.attributes.path.includes("DataValidation");
              };

              var getPaginated = function (pagination, files) {
                  var start = (pagination.currentPage - 1) *pagination.itemsPerPage;
                  if (start >= MAX_NUM_FILES) {
                      start %= MAX_NUM_FILES;
                  }
                  var end = start + pagination.itemsPerPage;
                  //slice() doesn’t change the original array.
                  var res = typeof files !== "undefined" && files.length > pagination.itemsPerPage? files.slice(start, end) : files;
                  self.sortWorking = false;
                  return res;
              };

              self.sortedDataset = [];
              self.getSortedDatasets = function () {
                  var regularDatasets = [];
                  var pinnedDatasets;
                  var hiveDb = [];
                  var featurestoreDb = [];
                  var traningDatasets = [];
                  var system = [];
                  for (var i = 0; i < self.files.length; i++) {
                      if(!isSystemFile(self.files[i])) {
                          regularDatasets.push(self.files[i])
                      } else {
                          if(self.isHive(self.files[i])) {
                              hiveDb.push(self.files[i]);
                              continue;
                          }
                          if (self.isFeaturestore(self.files[i])) {
                              featurestoreDb.push(self.files[i]);
                              continue;
                          }
                          if (self.isTrainingDatasets(self.files[i])) {
                              traningDatasets.push(self.files[i]);
                              continue;
                          }
                          if (isSystemFile(self.files[i])) {
                              system.push(self.files[i])
                          }
                      }
                  }
                  pinnedDatasets = hiveDb.concat(featurestoreDb);
                  pinnedDatasets = pinnedDatasets.concat(traningDatasets);
                  pinnedDatasets = pinnedDatasets.concat(system);
                  pinnedDatasets = pinnedDatasets.concat(regularDatasets);
                  return getPaginated(self.datasetPagination, pinnedDatasets);
              };

              self.paginatedFiles = [];
              self.getPaginatedItems = function () {
                  return getPaginated(self.datasetBrowserPagination, self.files);
              };

              var setPaginated = function () {
                  if ($routeParams.datasetName) {
                      self.paginatedFiles = self.getPaginatedItems();
                  } else {
                      self.sortedDataset = self.getSortedDatasets();
                  }
              };
            var getFromServer = function (pagination, id) {
                var start = (pagination.currentPage - 1)*pagination.itemsPerPage;
                var end = start + pagination.itemsPerPage;
                var offset = MAX_NUM_FILES*(pagination.page - 1);
                var limit = offset + MAX_NUM_FILES;
                var getMore = false;
                if (end > limit) {
                    getMore = true;
                    pagination.page++;
                } else if (start < offset) {
                    getMore = true;
                    pagination.page--;
                }
                if (getMore) {
                    self.offset = MAX_NUM_FILES*(pagination.page - 1);
                    getDirContents();
                    setPaginationInSessionStorage(id, pagination);
                } else {
                    setPaginated();
                }
            };

            $scope.$watch('datasetsCtrl.datasetPagination', function() {
                setPaginationInSessionStorage("datasetPagination", self.datasetPagination);
            }, true);

            $scope.$watch('datasetsCtrl.datasetBrowserPagination', function() {
                setPaginationInSessionStorage("datasetBrowserPagination", self.datasetBrowserPagination);
            }, true);

            self.pageChange = function (newPageNumber) {
                self.currentPage = newPageNumber;
                self.resetSelected();
                if ($routeParams.datasetName) {
                    self.datasetBrowserPagination.currentPage = newPageNumber;
                    getFromServer(self.datasetBrowserPagination, "datasetBrowserPagination");
                } else {
                    self.datasetPagination.currentPage = newPageNumber;
                    getFromServer(self.datasetPagination, "datasetPagination");
                }
            };

            self.statWorking = false;
            self.getStat = function (file) {
              self.statWorking = true;
              dataSetService.getDatasetStat(file.attributes.path).then(
                  function (success) {
                      self.statWorking = false;
                      self.selectedFiles[file.attributes.name] = success.data;
                  }, function (error) {
                      self.statWorking = false;
                  });
            };

            var getSortOrder = function () {
              return typeof $scope.reverse === "undefined" || $scope.reverse === true ?  ':desc' : ':asc';
            };

            var getSortBy = function () {
              var sortBy = [];
              if (typeof $scope.sortKey !== "undefined" && $scope.sortKey.length > 0) {
                  switch($scope.sortKey) {
                      case 'dir':
                      case 'type':
                      case 'attributes.dir':
                      case 'attributes.type':
                          sortBy.push('TYPE' + getSortOrder());
                          break;
                      case 'name':
                      case 'attributes.name':
                          sortBy.push('NAME' + getSortOrder());
                          break;
                      case 'owner':
                      case 'attributes.owner':
                          //sortBy.push('OWNER' + getSortOrder());
                          break;
                      case 'modificationTime':
                      case 'attributes.modificationTime':
                          sortBy.push('MODIFICATION_TIME' + getSortOrder());
                          break;
                      case 'size':
                      case 'attributes.size':
                          sortBy.push('SIZE' + getSortOrder());
                          break;
                      default:
                          sortBy.push('ID:asc');
                  }
              } else {
                  sortBy.push('ID:asc');//default
              }
              return sortBy;
            };

            var getFilterBy = function () {
               var filterBy = [];
               if (typeof self.shared !== "undefined") {
                   filterBy.push('SHARED:' + self.shared);
               }
               if (typeof self.status !== "undefined") {
                   filterBy.push('STATUS:' + self.status);
               }
               if (typeof self.isPublic !== "undefined" && self.isPublic > 0) {
                   filterBy.push('PUBLIC:' + self.isPublic);
               }
               if (typeof self.searchTerm !== "undefined" && self.searchTerm.length > 0) {
                   filterBy.push('NAME:' + self.searchTerm);
               }
               return filterBy;
            };

            function getFilteredResults() {
                var filtered;
                if ($routeParams.datasetName) {
                  filtered = $scope.$eval("datasetsCtrl.files | orderBy:sortKey:reverse | filter:datasetsCtrl.searchTerm");
                  filtered = getPaginated(self.datasetBrowserPagination, filtered);
                  self.paginatedFiles = filtered;
                } else {
                  filtered = $scope.$eval("datasetsCtrl.files | filter:{shared: datasetsCtrl.shared, accepted: datasetsCtrl.status, publicDataset: datasetsCtrl.isPublic} | filter:datasetsCtrl.searchTerm");
                  filtered = getPaginated(self.datasetPagination, filtered);
                  self.sortedDataset = filtered;
                }
                return filtered;
            }

            self.searchTerm = '';
            self.doFilter = function () {
                self.resetSelected();
                if (typeof self.files !== "undefined" && self.files.length === self.totalNumFiles) {
                    // if we have nothing left on server do angular  filter.
                    getFilteredResults();
                } else { // do server side filter
                    getDirContents();
                }
            };

            self.totalNumFiles = 0;
            var setTotal = function (fileCount, getSortBy, getFilterBy) {
                //set total only if there is no filter
                if (typeof getFilterBy === "undefined" || getFilterBy.length < 1) { // if filtered total is not saved
                    self.totalNumFiles = fileCount;
                }
            };

            self.onSuccess = function (e) {
              growl.success("Copied to clipboard", {title: '', ttl: 1000});
              e.clearSelection();
            };

            self.showSuccess = function (success, msg, id) {
                var successMsg = typeof success.data.successMessage === "undefined"? msg:success.data.successMessage;
                var refId = typeof id === "undefined"? 4:id;
                growl.success(successMsg, {title: 'Success', ttl: 1000, referenceId: refId});
            };

            self.showError = function (error, msg, id) {
                var errorMsg = (typeof error.data.usrMsg !== 'undefined')? error.data.usrMsg : msg;
                var refId = typeof id === "undefined"? 4:id;
                growl.error(errorMsg, {title: error.data.errorMsg, ttl: 5000, referenceId: refId});
            };

            self.metadataView = {};
            self.availableTemplates = [];
            self.closeSlider = false;
            self.breadcrumbLen = function () {
              if (self.pathArray === undefined || self.pathArray === null) {
                return 0;
              }
              var displayPathLen = 10;
              if (self.pathArray.length <= displayPathLen) {
                return self.pathArray.length - 1;
              }
              return displayPathLen;
            };

            self.cutBreadcrumbLen = function () {
              if (self.pathArray === undefined || self.pathArray === null) {
                return false;
              }
              if (self.pathArray.length - self.breadcrumbLen() > 0) {
                return true;
              }
              return false;
            };

            $scope.sort = function (keyname) {
              $scope.sortKey = keyname;   //set the sortKey to the param passed
              $scope.reverse = !$scope.reverse; //if true make it false and vice versa
            };

            /**
             * watch for changes happening in service variables from the other controller
             */
            $scope.$watchCollection(MetadataHelperService.getAvailableTemplates, function (availTemplates) {
              if (!angular.isUndefined(availTemplates)) {
                self.availableTemplates = availTemplates;
              }
            });

            $scope.$watch(MetadataHelperService.getDirContents, function (response) {
              if (response === "true") {
                getDirContents();
                MetadataHelperService.setDirContents("false");
              }
            });

            self.goToUrl = function (serviceName) {
                $location.path('project/' + self.projectId + '/' + serviceName);
            };

            self.isSharedDs = function (name) {
              var top = typeof name !== "undefined"? name.split(SHARED_DATASET_SEPARATOR) : undefined;
              if (typeof top === "undefined" || top.length === 1) {
                return false;
              }
              return true;
            };

            self.isShared = function () {
              return self.isSharedDs(self.pathArray[0]);
            };

            self.sharedDatasetPath = function () {
              if (!self.isShared()) {
                self.sharedPathArray = [];
                return;
              }
              // /proj::shared_ds/path/to  -> /proj/ds/path/to
              // so, we add '1' to the pathLen
              self.sharedPathArray = new Array(self.pathArray.length + 1);
              self.sharedPathArray[0] = top[0];
              self.sharedPathArray[1] = top[1];
              for (var i = 1; i < self.pathArray.length; i++) {
                self.sharedPathArray[i + 1] = self.pathArray[i];
              }
              return self.sharedPathArray;
            };

            var setPath = function (path) {
                path = path.split('/').filter(function (el) {
                    return el != null && el !== "";
                });
                // path.splice(0, 1); // remove Projects
                return path;
            };

            self.setCurrentPath = function (path) {
               self.currentPath = setPath(path);
            };

            self.setCurrentPathToParent = function (path) {
                var parent = setPath(path);
                parent.pop(); // remove last to get parent path
                self.currentPath = parent;
            };

            self.setCurrentPathFromFiles = function (files, newPathArray) {
                if (typeof files === "undefined" || files.length < 1) {
                    var path = angular.copy(newPathArray);
                    path.unshift(self.projectName);
                    self.currentPath = path;
                    return;
                }
                self.setCurrentPathToParent(files[0].attributes.path);
            };

            var replaceName = function (name, shortName) {
                var index = name.indexOf(SHARED_DATASET_SEPARATOR);
                if (index === -1) {
                    return shortName;
                }
                return name.substring(0, index + 2) + shortName;
            };

            /**
             * Utility function that converts the dataset name to a shorter name for the UI
             * @param dataset the dataset to get the short name of
             * @returns short name of the dataset
             */
            self.shortDatasetName = function (dataset) {
                if(self.isFeaturestore(dataset)){
                    return replaceName(dataset.name, FEATURESTORE_NAME);
                }
                if(self.isTrainingDatasets(dataset)){
                    return replaceName(dataset.name, TRAINING_DATASET_NAME);
                }
                if(self.isHive(dataset)){
                    return replaceName(dataset.name, HIVEDB_NAME);
                }
                return dataset.name
            };

            var fromShortDatasetName = function (name) {
                var index = name.indexOf(SHARED_DATASET_SEPARATOR);
                if (index === -1) {
                    if (name === FEATURESTORE_NAME) {
                        return self.projectName + '_featurestore.db';
                    } else if (name === HIVEDB_NAME) {
                        return self.projectName + '.db';
                    } else if (name === TRAINING_DATASET_NAME) {
                        return self.projectName + '_Training_Datasets';
                    } else {
                        return name;
                    }
                } else {
                    var projectName = name.substring(0, index);
                    if (name.endsWith(SHARED_DATASET_SEPARATOR + FEATURESTORE_NAME)) {
                        return projectName + SHARED_DATASET_SEPARATOR + projectName + '_featurestore.db';
                    } else if (name.endsWith(SHARED_DATASET_SEPARATOR + HIVEDB_NAME)) {
                        return projectName + SHARED_DATASET_SEPARATOR + projectName + '.db';
                    } else if (name.endsWith(SHARED_DATASET_SEPARATOR + TRAINING_DATASET_NAME)) {
                        return projectName + SHARED_DATASET_SEPARATOR + projectName + '_Training_Datasets';
                    } else {
                        return name;
                    }
                }
            };

            var getDatasetType = function (name) {
                if (name.endsWith(FEATURESTORE_NAME) || name.endsWith('_featurestore.db')) {
                   return 'FEATURESTORE';
                } else if (name.endsWith(HIVEDB_NAME) || name.endsWith('.db')) {
                   return 'HIVEDB';
                } else {
                   return 'DATASET';
                }
            };

            var isFullPath = function (path) {
               return path.startsWith(WAREHOUSE_PATH) || path.startsWith(PROJECT_PATH);
            };

            var getDatasetName = function (pathArray) {
              if (typeof pathArray !== "undefined") {
                  return pathArray.length > 0? pathArray[0] : '';
              }
            };

            var resolvePath = function (pathArray) {
                if (typeof pathArray !== "undefined") {
                    var name = getDatasetName(pathArray);
                    self.datasetType = getDatasetType(name);
                    var longName = fromShortDatasetName(name);
                    pathArray[0] = longName;
                    return pathArray;
                }
                return '';
            };

            var gotoPath = function (pathArray) {
                pathArray = resolvePath(pathArray);
                var path = pathArray !== '' && pathArray.length > 0? pathArray.join('/') : '';
                $location.path(self.basePath + '/' + path);
            };

            self.getFile = function (pathComponents) {
              var newPathArray;
              newPathArray = pathComponents;
              //Convert into a path
              var newPath = getPath(newPathArray);
              dataSetService.getDatasetStat(newPath).then(function (success) {
                      self.highlighted = success.data;
                      self.select(-1, self.highlighted, undefined);
                      $scope.search = self.highlighted.name;
                  }, function (error) {
                      self.showError(error, '', 4);
                  });
            };

            var beforeGetDirContents = function (pathComponents) {
                //Construct the new path array
                var newPathArray;
                if (typeof pathComponents !== "undefined") {
                    newPathArray = pathComponents; // resolved path
                } else if (typeof self.routeParamArray !== "undefined") {
                    newPathArray = resolvePath(self.pathArray.concat(self.routeParamArray));
                } else {
                    newPathArray = resolvePath(self.pathArray);
                }
                self.files = [];
                self.paginatedFiles = [];
                self.sortedDataset = [];
                self.working = true;
                self.sortWorking = true;
                return newPathArray;
            };

            var getDirContentsOnSuccess = function (newPathArray, sortBy, filterBy, success) {
                //Clear any selections
                self.all_selected = false;
                self.selectedFiles = {};
                //Reset the selected file
                self.selected = null;
                self.lastSelected = -1;
                //Set the current files and path
                self.files = success.data.items;
                self.filesCount = success.data.count;
                setTotal(self.filesCount, sortBy, filterBy);
                setPaginated();
                self.pathArray = newPathArray; // new path contains pathArray + routeParamArray
                self.routeParamArray = [];
                self.setCurrentPathFromFiles(self.files, newPathArray);
                if ($rootScope.selectedFile) {
                    var filePathArray = self.pathArray.concat($rootScope.selectedFile);
                    self.getFile(filePathArray);
                    $rootScope.selectedFile = undefined;
                }
                self.working = false;
            };

            var getDirContentsOnError = function (newPathArray, error) {
                if (error.data.errorCode === 110039) { // file exists but not a dir
                    var file = {attributes: {name: "", dir: false, path: newPathArray.join('/'), underConstruction: false}};
                    file.attributes.name = newPathArray.pop();
                    self.openDir(file);
                    self.pathArray = newPathArray;
                    self.routeParamArray = [];
                    getDirContents();
                } else if (error.data.errorCode === 110011 || error.data.errorCode === 110008 || error.data.errorCode === 110018) { // not found
                    self.pathArray = [];
                    self.routeParamArray = [];
                    self.showError(error, '', 4);
                    gotoPath();// reset url
                }
                self.working = false;
            };

            /**
             * Get the contents of the directory at the path with the given path components and load it into the frontend.
             * @param {type} The array of path compontents to fetch. If empty, fetches the current path.
             * @returns {undefined}
             */
            var getDirContents = function (pathComponents) {
              var newPathArray = beforeGetDirContents(pathComponents);
              var newPath = getPath(newPathArray);
              var sortBy = getSortBy();
              var filterBy = getFilterBy();
              dataSetService.getAllDatasets(newPath, self.offset, MAX_NUM_FILES, sortBy, filterBy, self.datasetType)
                  .then(function (success) {
                      getDirContentsOnSuccess(newPathArray, sortBy, filterBy, success);
                  }, function (error) {
                      getDirContentsOnError(newPathArray, error);
              });
            };

            /**
             * Remove the file at the given path. If called on a folder, will
             * remove the folder and all its contents recursively.
             * @param {type} path. The project-relative path to the inode to be removed.
             * @returns {undefined}
             */
            var removeFile = function (file) {
                dataSetService.delete(file.attributes.path).then(
                    function (success) {
                        self.showSuccess(success, 'Dataset removed from hdfs.', 4);
                        getDirContents();
                    }, function (error) {
                        self.showError(error, '', 4);
                });
            };

            /**
             * Open a modal dialog for folder creation. The folder is created at the current path.
             * @returns {undefined}
             */
            self.newDataSetModal = function () {
              var isDataset = typeof $routeParams.datasetName === "undefined";
              ModalService.newFolder('md', getPath(self.pathArray), self.datasetType, isDataset).then(
                  function (success) {
                      self.showSuccess(success, '', 4);
                      getDirContents();
                  }, function (error) {
              });
            };

            var getDeleteWarnMsg = function (file) {
                var type = typeof file !== "undefined" && file.attributes.dir? 'directory' : 'file';
                var warnIcon = '<i class="fa fa-exclamation-triangle text-danger"></i>';
                var warn = '<strong class="text-warning">Warning:</strong> Deleting this '+ type +' might affect' +
                    ' existing jobs and notebooks.';
                var warning = typeof file !== "undefined" && isSystemFile(file)? warnIcon + warn : '';
                var multiple = typeof file === "undefined"? 'all <strong class="text-danger">' + Object.keys(self.selectedFiles).length + '</strong> selected files/folders' : 'this ' + type;
                var confirmMsg = 'Are you sure you want to delete ' + multiple + '?';
                return warning + '<br>' + confirmMsg;
            };

            /**
             * Delete the file with the given path.
             * If called on a folder, will remove the folder
             * and all its contents recursively.
             * @param {type} fileName
             * @returns {undefined}
             */
            self.deleteFile = function (file) {
                ModalService.confirm('md', 'Confirm', getDeleteWarnMsg(file)).then(function (success) {
                   removeFile(file);
                });
            };

            /**
             * Delete the dataset with the given path.
             * @param {type} fileName
             * @returns {undefined}
             */
            self.deleteDataset = function (dataset) {
                ModalService.confirm('md', 'Confirm', getDeleteWarnMsg(dataset)).then(function (success) {
                   removeFile(dataset);
                });
            };

            var deleteMultipleFiles = function () {
                var i = 0;
                var names = [];
                for (var name in self.selectedFiles) {
                  names[i] = name;
                  removeFile(self.selectedFiles[name]);
                }
                for (var i = 0; i < names.length; i++) {
                  delete self.selectedFiles[names[i]];
                }
                self.all_selected = false;
                self.selectedFiles = {};
                self.selected = null;
                self.lastSelected = -1;
            };

            self.deleteSelected = function () {
                if (Object.keys(self.selectedFiles).length === 1) {
                    self.deleteFile(self.selectedFiles[Object.keys(self.selectedFiles)[0]]);
                    return;
                }
                ModalService.confirm('md', 'Confirm', getDeleteWarnMsg(undefined)).then(function (success) {
                  deleteMultipleFiles();
                });
            };

            /**
             * Makes the dataset public for anybody within the local cluster or any outside cluster.
             * @param id inodeId
             */
            self.sharingDataset = {};
            self.shareWithHops = function (id) {
              ModalService.confirm('sm', 'Confirm', 'Are you sure you want to make this DataSet public? \n\
                This will make all its files available for any registered user to download and process.').then(
                function (success) {
                  self.sharingDataset[id] = true;
                  delaHopsService.shareWithHopsByInodeId(id).then(
                    function (success) {
                      self.sharingDataset[id] = false;
                      self.showSuccess(success, 'The DataSet is now Public(Hops Site).', 4);
                      getDirContents();
                    }, function (error) {
                      self.sharingDataset[id] = false;
                      self.showError(error, '', 4);
                  });
                }
              );
            };

            /**
             * Makes the dataset public for anybody within the local cluster
             * @param id inodeId
             */
            self.shareWithCluster = function (id) {
              ModalService.confirm('sm', 'Confirm', 'Are you sure you want to make this DataSet public? \n\
                  This will make all its files available for any cluster user to share and process.').then(
                  function (success) {
                    self.sharingDataset[id] = true;
                    delaClusterService.shareWithClusterByInodeId(id).then(
                      function (success) {
                        self.sharingDataset[id] = false;
                        self.showSuccess(success, 'The DataSet is now Public(Cluster).', 4);
                        getDirContents();
                      }, function (error) {
                        self.sharingDataset[id] = false;
                        self.showError(error, '', 4);
                    });
                  }
              );
            };

            self.showManifest = function(publicDSId){
                delaHopsService.getManifest(publicDSId).then(function(success){
                    var manifest = success.data;
                    ModalService.json('md','Manifest', manifest).then(function(){

                    });
                });
            };

            self.unshareFromHops = function (publicDSId) {
              ModalService.confirm('sm', 'Confirm', 'Are you sure you want to make this DataSet private? ').then(
                      function (success) {
                        delaHopsService.unshareFromHops(publicDSId, false).then(
                                function (success) {
                                  self.showSuccess(success, 'The DataSet is not Public(internet) anymore.', 4);
                                  getDirContents();
                                }, function (error) {
                                  self.showError(error, '', 4);
                        });

                      }
              );
            };

            self.unshareFromCluster = function (inodeId) {
              ModalService.confirm('sm', 'Confirm', 'Are you sure you want to make this DataSet private? ').then(
                      function (success) {
                        delaClusterService.unshareFromCluster(inodeId).then(
                                function (success) {
                                  self.showSuccess(success, 'The DataSet is not Public(cluster) anymore.', 4);
                                  getDirContents();
                                }, function (error) {
                                  self.showError(error, '', 4);
                        });

                      }
              );
            };

            self.parentPathArray = function () {
              var newPathArray = self.pathArray.slice(0);
              var clippedPath = newPathArray.splice(1, newPathArray.length - 1);
              return clippedPath;
            };

            self.unzip = function (file) {
              growl.info("Started unzipping...", {title: 'Unzipping Started', ttl: 2000, referenceId: 4});
              dataSetService.unzip(file.attributes.path).then(
                      function (success) {
                         self.showSuccess(success, 'Unzipping in Background. Click on the directory to see the progress.', 4);
                      }, function (error) {
                         self.showError(error, '', 4);
              });
            };

            self.zip = function (file) {
              growl.info("Started zipping...", {title: 'Zipping Started', ttl: 2000, referenceId: 4});
              dataSetService.zip(file.attributes.path).then(
                      function (success) {
                        self.showSuccess(success, 'Zipping in Background. Click on the directory to see the progress.', 4);
                      }, function (error) {
                        self.showError(error, '', 4);
              });
            };

            self.isZipped = function (selected) {
              // https://stackoverflow.com/questions/680929/how-to-extract-extension-from-filename-string-in-javascript
              var re = /(?:\.([^.]+))?$/;
              var ext = re.exec(selected)[1];
              switch (ext) {
                  case "zip":
                      return true;
                  case "rar":
                      return true;
                  case "tar":
                      return true;
                  case "tgz":
                      return true;
                  case "gz":
                      return true;
                  case "bz2":
                      return true;
                  case "7z":
                      return true;
              }
              return false;
            };

            self.isZippedfile = function () {
              return self.isZipped(self.selected);
            };

            self.isDirectory = function () {
                if (!(self.selected == null)) {
                    return (self.selected).attributes.dir;
                } else {
                    return false;
                }
            };

            self.convertIPythonNotebook = function (file) {
              growl.info("Converting...",
                      {title: 'Conversion running in background, please wait...', ttl: 5000, referenceId: 4});
              //Construct the path to the notebook
              var nbPaths = self.currentPath.slice(2);
              var nbPath = "";
              nbPaths.forEach(function (entry) {
                  nbPath += entry + "/"
              });
              nbPath += file;
              JupyterService.convertIPythonNotebook(self.projectId, nbPath).then(
                      function (success) {
                        self.showSuccess(success, "Finished - refreshing directory contents", 4);
                        getDirContents();
                      }, function (error) {
                        self.showError(error, '', 4);
              });
            };


            self.isIPythonNotebook = function () {
              if (self.selected === null || self.selected === undefined) {
                return false;
              }
              if (self.selected.indexOf('.') == -1) {
                return false;
              }

              var ext =  self.selected.split('.').pop();
              if (ext === null || ext === undefined) {
                return false;
              }
              switch (ext) {
                case "ipynb":
                  return true;
              }
              return false;
            };

            self.browseDataset = function (dataset) {
              if (dataset.accepted === true) {
                UtilsService.setDatasetName(dataset.name);
                $rootScope.parentDS = dataset;
                $location.path($location.path() + '/' + dataset.name + '/');
              } else {
                ModalService.confirmShare('sm', 'Accept Shared Dataset?', 'Do you want to accept this dataset and add it to this project?')
                  .then(function (success) {
                      dataSetService.accept(dataset.attributes.path).then(
                      function (success) {
                        $location.path($location.path() + '/' + dataset.name + '/');
                      }, function (error) {
                        growl.warning("Error: " + error.data.errorMsg, {title: 'Error', ttl: 5000});
                    });
                  }, function (error) {
                    if (error === 'reject') {
                        dataSetService.reject(dataset.attributes.path).then(
                        function (success) {
                          $location.path($location.path() + '/');
                          self.showSuccess(success, '', 4);
                        }, function (error) {
                          self.showError(error, 'The Dataset has been removed.', 4);
                      });
                    }
                  });
              }

            };

            self.getRole = function () {
              UserService.getRole(self.projectId).then(
                function (success) {
                  self.role = success.data.role;
                }, function (error) {
                  self.role = "";
              });
            };

            /**
             * Preview the requested file in a Modal. If the file is README.md
             * and the preview flag is false, preview the file in datasets.
             * @param {type} dataset
             * @param {type} preview
             * @returns {undefined}
             */
            self.filePreview = function (dataset, preview, readme) {
              var filePath = "";
              var fileName = "";
              self.readmeWorking = true;
              //handle README.md filename for datasets browser viewing here
              if (readme && !preview) {
                  filePath = dataset.attributes.path + "/README.md";
                  fileName = "README.md";
              } else {
                  filePath = dataset.attributes.path;
                  fileName = dataset.attributes.name;
              }
              //If filename is README.md then try fetching it without the modal
              if (readme && !preview) {
                dataSetService.getDatasetBlob(filePath, "head").then(
                        function (success) {
                          var fileDetails = success.data;
                          var conv = new showdown.Converter({parseImgDimensions: true});
                          $scope.readme = conv.makeHtml(fileDetails.preview.content);
                          self.readmeWorking = false;
                        }, function (error) {
                          //To hide README from UI
                          $scope.readme = null;
                          self.readmeWorking = false;
                });
              } else {
                self.readmeWorking = false;
                if(filePath.endsWith('.ipynb')) {
                    ModalService.filePreview('xl', fileName, filePath, self.projectId, "head").then(
                        function(success) {},
                        function(error) {});
                } else {
                    ModalService.filePreview('lg', fileName, filePath, self.projectId, "head").then(
                        function (success) {},
                        function (error) {});
                }
              }
            };


            self.copy = function (file, name) {
              ModalService.selectDir('lg', self.projectId, "/[^]*/", "problem selecting folder").then(function (success) {
                var destPath = success;
                // Get the relative path of this DataSet, relative to the project home directory
                // replace only first occurrence
                var finalPath = destPath + "/" + name;

                dataSetService.copy(file.attributes.path, finalPath).then(
                        function (success) {
                          self.showSuccess(success, 'Copied ' + name + ' successfully', 4);
                          getDirContents();
                        }, function (error) {
                          self.showError(error, '', 4);
                });
              }, function (error) {
              });
            };


            self.copySelected = function () {
              //Check if we are to move one file or many
              if (Object.keys(self.selectedFiles).length === 0 && self.selectedFiles.constructor === Object) {
                if (self.selected !== null && self.selected !== undefined) {
                  self.copy(self.selected, self.selected.attributes.name);
                }
              } else if (Object.keys(self.selectedFiles).length !== 0 && self.selectedFiles.constructor === Object) {

                ModalService.selectDir('lg', self.projectId, "/[^]*/", "problem selecting folder").then(
                        function (success) {
                          var destPath = success;
                          var names = [];
                          var i = 0;
                          //Check if have have multiple files
                          for (var name in self.selectedFiles) {
                            names[i] = name;
                            i++;
                          }
                          var errorCode = -1;
                          for (var name in self.selectedFiles) {
                            dataSetService.copy(self.selectedFiles[name].attributes.path, destPath + "/" + name).then(
                                    function (success) {
                                      //If we copied the last file
                                      if (name === names[names.length - 1]) {
                                        getDirContents();
                                        for (var i = 0; i < names.length; i++) {
                                          delete self.selectedFiles[names[i]];
                                        }
                                        self.all_selected = false;
                                      }
                                      //growl.success('',{title: 'Copied successfully', ttl: 5000, referenceId: 4});
                                    }, function (error) {
                                       self.showError(error, '', 4);
                                       errorCode = error.data.code;
                            });
                            if (errorCode === 110045) {
                              break;
                            }
                          }
                        }, function (error) {
                  //The user changed their mind.
                });
              }
            };


            self.move = function (file, name) {
              ModalService.selectDir('lg', self.projectId, "/[^]*/", "problem selecting folder").then(
                      function (success) {
                        var destPath = success;
                        var finalPath = destPath + "/" + name;
                        dataSetService.move(file.attributes.path, finalPath).then(
                                function (success) {
                                    self.showSuccess(success, 'Moved ' + name + ' successfully.', 4);
                                    getDirContents();
                                }, function (error) {
                                    self.showError(error, '', 4);
                                });
                      }, function (error) {
              });

            };


            self.isSelectedFiles = function () {
              return Object.keys(self.selectedFiles).length;
            };

            self.moveSelected = function () {
              //Check if we are to move one file or many
              if (Object.keys(self.selectedFiles).length === 0 && self.selectedFiles.constructor === Object) {
                if (self.selected !== null && self.selected !== undefined) {
                  self.move(self.selected, self.selected.name);
                }
              } else if (Object.keys(self.selectedFiles).length !== 0 && self.selectedFiles.constructor === Object) {

                ModalService.selectDir('lg', self.projectId, "/[^]*/", "problem selecting folder").then(
                        function (success) {
                          var destPath = success;
                          var names = [];
                          var i = 0;
                          //Check if have have multiple files
                          for (var name in self.selectedFiles) {
                            names[i] = name;
                            i++;
                          }

                          var errorCode = -1;
                          for (var name in self.selectedFiles) {
                            dataSetService.move(self.selectedFiles[name].attributes.path, destPath + "/" + name).then(
                                    function (success) {
                                      //If we moved the last file
                                      if (name === names[names.length - 1]) {
                                        getDirContents();
                                        for (var i = 0; i < names.length; i++) {
                                          delete self.selectedFiles[names[i]];
                                        }
                                        self.all_selected = false;
                                      }
                                    }, function (error) {
                                        self.showError(error, '', 4);
                                        errorCode = error.data.code;
                            });
                            if (errorCode === 110045) {
                              break;
                            }
                          }
                        }, function (error) {
                            //The user changed their mind.
                });
              }
            };

            var renameModal = function (file, name) {
              ModalService.enterName('sm', "Rename File or Directory", name).then(
                      function (success) {
                        if (typeof file !== 'string' && !(file instanceof String)){
                            file = file.attributes.path;
                        }
                        var filePathArray = file.split('/');
                        filePathArray.pop();
                        filePathArray.push(success.newName);
                        dataSetService.move(file, filePathArray.join('/')).then(
                                function (success) {
                                  getDirContents();
                                  self.all_selected = false;
                                  self.selectedFiles = {};
                                  self.selected = null;
                                  self.lastSelected = -1;
                                }, function (error) {
                                  self.showError(error, '', 4);
                                  self.all_selected = false;
                                  self.selectedFiles = {};
                                  self.selected = null;
                                  self.lastSelected = -1;
                        });
                      });
            };

            self.rename = function (file, name) {
              renameModal(file, name);
            };

            self.renameSelected = function () {
              if (self.isSelectedFiles() === 1) {
                var file, inodeName;
                for (var name in self.selectedFiles) {
                  inodeName = name;
                }
                file = self.selectedFiles[inodeName].attributes.path;
                renameModal(file, inodeName);
              }
            };
            /**
             * Opens a modal dialog for file upload.
             * @returns {undefined}
             */
            self.uploadFile = function () {
              var templateId = -1;
              ModalService.upload('lg', self.projectId, getPath(self.pathArray), templateId, self.datasetType).then(
                  function (success) {
                    getDirContents();
                  }, function (error) {
                    getDirContents();
              });
            };

            /**
             * Opens a modal dialog for sharing.
             * @returns {undefined}
             */
            self.share = function (dataset) {
              ModalService.shareDataset('md', dataset.attributes.path, dataset.type).then(
                      function (success) {
                        self.showSuccess(success, 'The Dataset was successfully shared.', 4);
                        getDirContents();
                      }, function (error) {
              });
            };

            /**
             * Opens a modal dialog to make dataset editable
             * @param {type} name
             * @param {type} permissions
             * @returns {undefined}
             */
            self.permissions = function (dataset, permissions) {
              ModalService.permissions('md', dataset.attributes.path, dataset.type, permissions).then(
                      function (success) {
                        self.showSuccess(success, 'The Dataset permissions were successfully modified.', 4);
                        getDirContents();
                      }, function (error) {
              });
            };

            /**
             * Opens a modal dialog for unsharing.
             * @param {type} name
             * @returns {undefined}
             */
            self.unshare = function (dataset) {
              ModalService.unshareDataset('md', dataset.attributes.path, dataset.type).then(
                      function (success) {
                        self.showSuccess(success, 'The Dataset was successfully unshared.', 4);
                        getDirContents();
                      }, function (error) {
              });
            };

            /**
             * Upon click on a inode in the browser:
             *  + If folder: open folder, fetch contents from server and display.
             *  + If file: open a confirm dialog prompting for download.
             * @param {type} file
             * @returns {undefined}
             */
            self.openDir = function (file) {
              if (file.attributes.dir) {
                var newPathArray = self.pathArray.slice(0);
                newPathArray.push(file.attributes.name);
                gotoPath(newPathArray);
                self.tgState = false;
              } else if (!file.attributes.underConstruction) {
                ModalService.confirm('sm', 'Confirm', 'Do you want to download this file?').then(
                        function (success) {
                          showToast('Preparing Download..');
                          dataSetService.getDownloadToken(file.attributes.path, self.datasetType).then(
                                  function (success) {
                                    var token = success.data.data.value;
                                    closeToast();
                                    dataSetService.download(file.attributes.path, token, self.datasetType);
                                  },function (error) {
                                    closeToast();
                                    self.showError(error, '', 4);
                          });
                        }
                );
              } else {
                growl.info("File under construction.", {title: 'Info', ttl: 5000});
              }
            };

            var showToast = function(text) {
              var toast = $mdToast.simple()
                            .textContent(text)
                            .action('Close')
                            .position('bottom right')
                            .hideDelay(0);

              $mdToast.show(toast).then(function(response) {
                if ( response == 'ok' ) {
                  $mdToast.hide();
                }
              });
            };

            var closeToast = function() {
              $mdToast.hide();
            };

            /**
             * Go up to parent directory.
             * @returns {undefined}
             */
            self.back = function () {
              var newPathArray = self.pathArray.slice(0);
              newPathArray.pop();
              self.currentPath.pop();
              if (newPathArray.length === 0) {
                $location.path(self.basePath);
              } else {
                gotoPath(newPathArray);
              }
            };

            self.goToDataSetsDir = function () {
              $location.path(self.basePath);
            };

            /**
             * Go to the folder at the index in the pathArray array.
             * @param {type} index
             * @returns {undefined}
             */
            self.goToFolder = function (index) {
              var newPathArray = self.pathArray.slice(0);
              newPathArray.splice(index, newPathArray.length - index);
              gotoPath(newPathArray);
              self.tgState = false;
            };

            self.refresh = function () {
                getDirContents();
                self.tgState = false;
            };


              self.menustyle = {
              "opacity": 0.2
            };

            self.selectDataset = function (selectedIndex, file) {
                self.resetSelected();
                self.tgState = true;
                self.selected = file.name;
                self.selectedFiles[file.name] = file;
                self.selectedFiles[file.name].selectedIndex = selectedIndex;
                self.menustyle.opacity = 1.0;
                self.lastSelected = selectedIndex;
            };

            /**
             * Select an inode; updates details panel.
             * @param {type} selectedIndex
             * @param {type} file
             * @param {type} event
             * @returns {undefined}
             */
            self.select = function (selectedIndex, file, event) {
              // 1. Turn off the selected file at the top of the browser.
              // Add existing selected file (idempotent, if already added)
              // If file already selected, deselect it.
              if (event && (event.ctrlKey || event.metaKey)) {

              } else if (event && event.shiftKey && self.isSelectedFiles() > 0) {
                self.selected = null;
                self.tgState = false;
                var i = self.lastSelected;
                var file;
                var files = getFilteredResults();
                if (self.lastSelected  >= 0 && self.lastSelected < selectedIndex) {
                  while (i <= selectedIndex) {
                    file = files[i];
                    self.selectedFiles[file.attributes.name] = file;
                    self.selectedFiles[file.attributes.name].selectedIndex = i;
                    i++;
                  }
                } else if (self.lastSelected >= 0 && self.lastSelected > selectedIndex) {
                  while (i >= selectedIndex) {
                    file = files[i];
                    self.selectedFiles[file.attributes.name] = file;
                    self.selectedFiles[file.attributes.name].selectedIndex = i;
                    i--;
                  }
                }
                self.menustyle.opacity = 1.0;
                self.lastSelected = selectedIndex;
                return;
              } else {
                self.resetSelected();
              }
              if (self.isSelectedFiles() > 0) {
                self.selected = null;
                self.lastSelected = -1;
                self.tgState = false;
              } else {
                self.tgState = true;
                self.selected = file.attributes.name;
              }
              self.selectedFiles[file.attributes.name] = file;
              self.selectedFiles[file.attributes.name].selectedIndex = selectedIndex;
              self.menustyle.opacity = 1.0;
              self.lastSelected = selectedIndex;
            };

            self.haveSelected = function (file) {
              if (file === undefined || file === null || file.attributes.name === undefined || file.attributes.name === null) {
                return false;
              }
              if (file.attributes.name in self.selectedFiles) {
                return true;
              }
              return false;
            };

            self.selectAll = function () {
              var filtered = getFilteredResults();
              for (var i = 0; i < filtered.length; i++) {
                var f = filtered[i];
                self.selectedFiles[f.attributes.name] = f;
                self.selectedFiles[f.attributes.name].selectedIndex = i;
              }
              self.menustyle.opacity = 1;
              self.selected = null;
              self.lastSelected = -1;
              self.all_selected = true;
              self.tgState = false;
              if (Object.keys(self.selectedFiles).length === 1 && self.selectedFiles.constructor === Object) {
                self.tgState = true;
                self.selected = Object.keys(self.selectedFiles)[0];
              }
            };

            //TODO: Move files to hdfs trash folder
            self.trashSelected = function () {

            };

            self.allSelected = function () {
                return typeof self.paginatedFiles !== "undefined" && self.paginatedFiles.length > 0
                    && Object.keys(self.selectedFiles).length === self.paginatedFiles.length;
            };

            self.deselect = function (selectedIndex, file, event) {
              var i = 0;
              if (Object.keys(self.selectedFiles).length === 1 && self.selectedFiles.constructor === Object) {
                for (var name in self.selectedFiles) {
                  if (file.attributes.name === name) {
                    delete self.selectedFiles[name];
                    //break;
                  }
                }
              } else {
                if (event.ctrlKey) {
                  for (var name in self.selectedFiles) {
                    if (file.attributes.name === name) {
                      delete self.selectedFiles[name];
                      break;
                    }
                  }
                } else {
                  for (var name in self.selectedFiles) {
                    if (file.attributes.name !== name) {
                      delete self.selectedFiles[name];
                      //break;
                    }
                  }
                }
              }
              if (Object.keys(self.selectedFiles).length === 0 && self.selectedFiles.constructor === Object) {
                self.menustyle.opacity = 0.2;
                self.selected = null;
                self.lastSelected = -1;
                self.tgState = false;
              } else if (Object.keys(self.selectedFiles).length === 1 && self.selectedFiles.constructor === Object) {
                self.menustyle.opacity = 1.0;
                self.selected = Object.keys(self.selectedFiles)[0];
                self.tgState = true;
                self.lastSelected = selectedIndex;
              } else {
                self.tgState = false;
              }
              self.all_selected = false;
            };

            self.deselectAll = function () {
              self.selectedFiles = {};
              self.selected = null;
              self.lastSelected = -1;
              self.sharedPath = null;
              self.all_selected = false;
              self.menustyle.opacity = 0.2;
            };

            self.resetSelected = function () {
              self.deselectAll();
              self.tgState = false;
            };

            self.toggleLeft = buildToggler('left');
            self.toggleRight = buildToggler('right');

            function buildToggler(navID) {
              var debounceFn = $mdUtil.debounce(function () {
                $mdSidenav(navID).toggle()
                        .then(function () {
                          MetadataHelperService.fetchAvailableTemplates()
                                  .then(function (response) {
                                    self.availableTemplates = JSON.parse(response.board).templates;
                                  });
                        });
              }, 300);
              return debounceFn;
            };

            self.getSelectedPath = function (selectedFile) {
              if (self.isSelectedFiles() !== 1) {
                return "";
              }
              return "hdfs://" + selectedFile.attributes.path;
            };

            $scope.editDescription = false;
            self.updateDescription = function (event, file, newDescription) {
                if (typeof event !== "undefined" && event.keyCode === 27) {
                    $scope.toggleEditDescription();
                } else if (typeof event !== "undefined" && event.keyCode === 13) {
                    dataSetService.updateDescription(file.attributes.path, newDescription).then(function (success) {
                        var updated = success.data;
                        file.description = updated.description;
                        self.showSuccess(success, 'The Dataset description was successfully modified.', 4);
                    }, function (error) {
                        self.showError(error, '', 4);
                    });
                    $scope.toggleEditDescription();
                }
            };

            $scope.$window = $window;
            $scope.toggleEditDescription = function (file) {
                $scope.editDescription = !$scope.editDescription;
                if ($scope.editDescription) {
                    self.description = typeof file !== "undefined"? file.description : '';
                    $scope.$window.onclick = function (event) {
                        var clickedElement = event.target;
                        var id = typeof clickedElement !== "undefined"? clickedElement.id : '';
                        if (id !== '' && id !== 'descTextarea' && id !== 'editDescriptionToggle') {
                            $scope.toggleEditDescription();
                        }
                    };
                    setTimeout(function(){
                        var target = angular.element(document).find('#descTextarea');
                        if (typeof target !== "undefined") {
                            target.focus();
                        }
                    }, 10);
                } else {
                    $scope.editDescription = false;
                    $scope.$window.onclick = null;
                    var phase = $scope.$$phase;
                    //Safe $apply to prevent $apply already in progress error
                    if (phase !== '$apply' && phase !== '$digest' ) {
                      $scope.$apply();
                    }
                }
            };

            var getProjectName = function () {
              ProjectService.get({}, {'id': self.projectId}).$promise.then(
                  function (success) {
                      self.projectName = success.projectName;
                      getDirContents();
                  }, function (error) {

                  });
            };

            var init = function () {
              //Check if the current dataset is set
              if ($routeParams.datasetName) {
                  //Dataset is set: get the contents
                  self.pathArray = [$routeParams.datasetName];
              } else {
                  //No current dataset is set: get all datasets.
                  self.pathArray = [];
              }
              if ($routeParams.datasetName && $routeParams.fileName) {
                  //file name is set: get the contents
                  var paths = $routeParams.fileName.split("/");
                  paths.forEach(function (entry) {
                      if (entry !== "") {
                          self.routeParamArray.push(entry);
                      }
                  });
              }
              self.getRole();
              getProjectName();
              self.tgState = false;
            };
            init();
          }]);
