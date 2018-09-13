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
        .controller('ClusterDatasetCtrl', ['$location', '$anchorScroll', '$scope', '$rootScope',
          'md5', 'ModalService', 'HopssiteService', 'DelaService', 'DelaClusterService','ProjectService', 'growl',
          function ($location, $anchorScroll, $scope, $rootScope, md5, ModalService,
                  HopssiteService, DelaService, DelaClusterService, ProjectService, growl) {
            var self = this;
            self.readme;
            self.displayCategories;
            self.selectedCategory;
            self.selectedCategoryMap = {};
            self.selectedDataset;
            self.loadingDisplayCategories = false;
            self.loadingSelectedCategory = false;
            self.loadingReadme = false;
            self.publicDSId = $rootScope.publicDSId;
            $rootScope.publicDSId = undefined; //reset

            var getDisplayCategories = function () {
              self.loadingDisplayCategories = true;
              HopssiteService.getDisplayCategories().then(function (success) {
                console.log("getDisplayCategories", success);
                self.displayCategories = success.data;
                self.loadingDisplayCategories = false;
              }, function (error) {
                self.displayCategories = [];
                self.loadingDisplayCategories = false;
                console.log("getDisplayCategories", error);
              });
            };
            
            var getDatasetFromLocal = function (inodeId) {
              ProjectService.getDatasetInfo({inodeId: inodeId}).$promise.then(
                function (response) {
                  console.log('getDatasetFromLocal: ', response);
                  self.selectedDataset = response;
                  getReadMeLocal(self.selectedDataset.inodeId);
                }, function (error) {
                  console.log('getDatasetFromLocal Error: ', error);
              });
            };

            self.selectCategory = function (category) {
              console.log("selectDisplayCategory", category);
              self.selectedDataset = undefined;
              self.selectedCategory = category;
              self.selectedCategoryMap[category.categoryName] = category;
              doGetLocalCluster(self.selectedCategoryMap[category.categoryName]);
            };
                                    
            self.selectDisplayCategory = function (category) {
              console.log("selectDisplayCategory", category);
              self.selectedDataset = undefined;
              self.selectedCategory = category;
              self.selectedCategoryMap[category.categoryName] = category;
              doGetLocalCluster(self.selectedCategoryMap[category.categoryName]);
            };

            self.selectItem = function (selectItem) {
              self.selectedDataset = selectItem;
              self.selectedSubCategory = undefined;
              getReadMeLocal(selectItem.inodeId);
            };
            
            self.selectDataset = function (selectItem) {
              self.selectedDataset = selectItem;
              self.selectedSubCategory = undefined;
              getReadMeLocal(selectItem.inodeId);
            };
            
            var doGetLocalCluster = function (category) {
              if (category['selectedList'] === undefined || category['selectedList'].lenght === 0) {
                self.loadingSelectedCategory = true;
              }
              
              DelaClusterService.getLocalPublicDatasets().then(
                      function (success) {
                        category['selectedList'] = success.data;
                        category['selectedSubCategoryList'] = success.data;
                        self.loadingSelectedCategory = false;
                        console.log("doGetLocalCluster", success);
                      },
                      function (error) {
                        category['selectedList'] = [];
                        category['selectedSubCategoryList'] = [];
                        self.loadingSelectedCategory = false;
                        console.log("doGetLocalCluster", error);
                        growl.info("Could not load Public Datasets", {title: 'Info', ttl: 5000});
                      }
              );
            };

            var getReadMeLocal = function (inodeId) {
              self.readme = '';
              self.loadingReadme = true;
              HopssiteService.getReadmeByInode(inodeId).then(function (success) {
                console.log("getReadMeLocal", success);
                var conv = new showdown.Converter({parseImgDimensions: true});
                self.readme = conv.makeHtml(success.data.content);
                self.loadingReadme = false;
              }, function (error) {
                self.readme = "No readme found.";
                self.loadingReadme = false;
                console.log("getReadMeLocal", error);
              });
            };
            
            self.getEmailHash = function (email) {
              return md5.createHash(email || '');
            };
            
            self.addPublicDatasetModal = function (dataset) {
              var datasetDto = dataset;
              var projects;
              ProjectService.query().$promise.then(function (success) {
                projects = success;
                //show dataset
                ModalService.viewPublicDataset('md', projects, datasetDto).then(function (success) {
                  growl.success(success.data.successMessage, {title: 'Success', ttl: 1000, referenceId: 13});
                }, function (error) {

                });
              }, function (error) {
                console.log('Error: ' + error);
              });
            };
            
            var initCtrl = function () {
              self.displayCategories = [{'categoryName': 'all', 'displayName': 'All'}];
              if (self.publicDSId !== undefined) {
                getDatasetFromLocal(self.publicDSId);
              }
              self.selectCategory({'categoryName': 'all', 'displayName': 'All'}); //open by default 
            };
            
            initCtrl();


            var init = function () {
              $('.keep-open').on('shown.bs.dropdown', '.dropdown', function () {
                $(this).attr('closable', false);
              });

              $('.keep-open').on('click', '.dropdown', function () {
                console.log('.keep-open: click');
              });

              $('.keep-open').on('hide.bs.dropdown', '.dropdown', function () {
                return $(this).attr('closable') === 'true';
              });

              $('.keep-open').on('click', '#dLabel', function() {
                $(this).parent().attr('closable', true );
              });
              
              $(window).scroll(function () {
                if ($(this).scrollLeft() > 0) {
                  $('#publicdataset').css({'left': 45 - $(this).scrollLeft()});
                }
              });
              $(window).resize(function () {
                var w = window.outerWidth;
                if (w > 1280) {
                  $('#publicdataset').css({'left': 'auto'});
                }
              });
            };

            var overflowY = function (val) {
              $('#hwWrapper').css({'overflow-y': val});
            };

            self.setupStyle = function () {
              init();
              overflowY('hidden');
              $('#publicdatasetWrapper').css({'width': '1200px'});
            };

            self.overflowYAuto = function () {
              overflowY('auto');
              $('#publicdatasetWrapper').css({'width': '1500px'});
            };

            $scope.$on("$destroy", function () {
              overflowY('auto');
            });
            
            $scope.isSelected = function (name) {
              if (self.selectedCategory === undefined) {
                return false;
              }
              return self.selectedCategory.displayName === name;
            };
            
            self.sizeOnDisk = function (fileSizeInBytes) {
              if (fileSizeInBytes === undefined) {
                return '--';
              }
              if (fileSizeInBytes === -1) {
                return '--';
              }
              return convertSize(fileSizeInBytes);
            };

          }]);


