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
        .controller('HopsDatasetCtrl', ['$location', '$anchorScroll', '$scope', '$rootScope',
          'md5', 'ModalService', 'HopssiteService', 'DelaService', 'ProjectService', 'growl',
          function ($location, $anchorScroll, $scope, $rootScope, md5, ModalService,
                  HopssiteService, DelaService, ProjectService, growl) {
            var self = this;
            self.comments;
            self.readme;
            self.displayCategories;
            self.selectedCategory;
            self.selectedCategoryMap = {};
            self.selectedDataset;
            self.userRating = 0;
            self.myUserId;
            self.myClusterId;
            self.commentEditable = [];
            self.newComment;
            self.updateComment;
            self.publicDSId = $rootScope.publicDSId;
            $rootScope.publicDSId = undefined; //reset
            self.loadingDisplayCategories = false;
            self.loadingSelectedCategory = false;
            self.loadingReadme = false;
            self.loadingComments = false;

            var getUser = function () {
              HopssiteService.getUserId().then(function (success) {
                self.myUserId = success.data;
              }, function (error) {
                self.myUserId = undefined;
                console.log("getUser", error);
              });
            };
            
            var getClusterId = function () {
              HopssiteService.getClusterId().then(function (success) {
                self.myClusterId = success.data;
              }, function (error) {
                self.myClusterId = undefined;
                console.log("getClusterId", error);
              });
            };

            var getDisplayCategories = function () {
              self.loadingDisplayCategories = true;
              HopssiteService.getDisplayCategories().then(function (success) {
                self.displayCategories = success.data;
                self.loadingDisplayCategories = false;
              }, function (error) {
                self.displayCategories = [];
                self.loadingDisplayCategories = false;
                console.log("getDisplayCategories", error);
              });
            };
            
            var getDataset = function (publicId) {
              HopssiteService.getDataset(publicId).then(function (success) {
                self.selectedDataset = success.data;
                self.selectedSubCategory = undefined;
                getBasicReadme(publicId);
                getComments(publicId);
                getUserRating(publicId);
              }, function (error) {
                console.log("getDataset", error);
              });
            };
            
            var getDatasetFromLocal = function (inodeId) {
              ProjectService.getDatasetInfo({inodeId: inodeId}).$promise.then(
                function (response) {
                  self.selectedDataset = response;
                  getReadMeLocal(self.selectedDataset.inodeId);
                }, function (error) {
                  console.log('getDatasetFromLocal Error: ', error);
              });
            };            
            
            self.selectCategory = function (category) {
              self.selectedDataset = undefined;
              self.selectedCategory = category;
              self.selectedCategoryMap[category.categoryName] = category;
              doGetLocalCluster(self.selectedCategoryMap[category.categoryName]);
            };
            
            self.selectDisplayCategory = function (category) {
              self.selectedDataset = undefined;
              self.selectedCategory = category;
              self.selectedCategoryMap[category.categoryName] = category;
              if ($rootScope.isDelaEnabled) {
                doGet(self.selectedCategoryMap[category.categoryName]);
              } else {
                doGetLocalCluster(self.selectedCategoryMap[category.categoryName]);
              }
            };

            self.selectItem = function (selectItem) {
              self.selectedDataset = selectItem;
              self.selectedSubCategory = undefined;
              getBasicReadme(self.selectedDataset.publicId);
              getComments(self.selectedDataset.publicId);
              getUserRating(self.selectedDataset.publicId);
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
              
              DelaService.getLocalPublicDatasets().then(
                      function (success) {
                        category['selectedList'] = success;
                        category['selectedSubCategoryList'] = success.data;
                        self.loadingSelectedCategory = false;
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

            var doGet = function (category) {
              if (category['selectedList'] === undefined || category['selectedList'].lenght === 0) {
                self.loadingSelectedCategory = true;
              }

              HopssiteService.getType(category.categoryName).then(function (success) {
                category['selectedList'] = success.data;
                category['selectedSubCategoryList'] = success.data;
                self.loadingSelectedCategory = false;
              }, function (error) {
                category['selectedList'] = [];
                category['selectedSubCategoryList'] = [];
                self.loadingSelectedCategory = false;
                console.log("doGet", error);
              });
            };

            var getBasicReadme = function (publicId) {
              DelaService.getDetails(publicId).then(function (success) {
                getreadme(publicId, success.data.bootstrap);
              }, function (error) {
                self.readme = "No details found.";
                console.log("getBasicReadme", error);
              });
            };
            
            var getReadMeLocal = function (inodeId) {
              self.readme = '';
              self.loadingReadme = true;
              HopssiteService.getReadmeByInode(inodeId).then(function (success) {
                var conv = new showdown.Converter({parseImgDimensions: true});
                self.readme =  conv.makeHtml(success.data.content);
                self.loadingReadme = false;
              }, function (error) {
                self.readme = "No readme found.";
                self.loadingReadme = false;
                console.log("getReadMeLocal", error);
              });
            };
            
            var getreadme = function (publicId, bootstrap) {
              self.readme = '';
              self.loadingReadme = true;
              DelaService.getReadme(publicId, bootstrap).then(function (success) {
                var conv = new showdown.Converter({parseImgDimensions: true});
                self.readme = conv.makeHtml(success.data.content);
                self.loadingReadme = false;
              }, function (error) {
                self.readme = "No readme found.";
                self.loadingReadme = false;
                console.log("getreadme", error);
              });
            };

            var getComments = function (publicId) {
              self.loadingComments = true;
              self.comments = [];
              if (self.myUserId === undefined) {
                getUser();
              }
              HopssiteService.getComments(publicId).then(function (success) {
                self.comments = success.data;
                self.loadingComments = false;
              }, function (error) {
                self.comments = [];
                self.loadingComments = false;
                console.log("getComments", error);
              });
            };

            var getUserRating = function (publicId) {
              HopssiteService.getUserRating(publicId).then(function (success) {
                self.userRating = success.data.rate;
              }, function (error) {
                self.userRating = 1;
                console.log("getUserRating", error);
              });
            };           

            self.getEmailHash = function (email) {
              return md5.createHash(email || '');
            };

            self.gotoComment = function () {
              var old = $location.hash();
              $location.hash('commentbtn');
              $anchorScroll();
              $location.hash(old);
            };
            
            var getIssueObject = function (type, msg) {
                var issue = {};
                issue['type'] = type;
                issue['msg'] = msg;
                return issue;
            };

            self.reportAbuse = function (commentId) {
              ModalService.reportIssueModal('md', 'Report issue', '').then(function (success) {
                var issue = getIssueObject('CommentIssue', success);
                postCommentIssue(commentId, issue);
              }, function (error) {
                console.log(error);
              });
            };
            
            self.rate = function () {
              postRating(self.selectedDataset.publicId, self.userRating);
            };
            
            
            self.reportDataset = function () {
              ModalService.reportIssueModal('md', 'Report issue', '').then(function (success) {
                var issue = getIssueObject('DatasetIssue', success);
                postDatasetIssue(self.selectedDataset.publicId, issue);
              }, function (error) {
                console.log(error);
              });
            };

            self.commentMakeEditable = function (index) {
              self.commentEditable[index] = !self.commentEditable[index];
              var commentId = "#commentdiv-" + index;
              if (self.commentEditable[index]) {
                $(commentId).css("background-color", "#FFFFCC");
              }
            };

            self.cancelEdit = function (comment, index) {
              self.commentEditable[index] = !self.commentEditable[index];
              var commentElementId = "#commentdiv-" + index;
              if (!self.commentEditable[index]) {
                $(commentElementId).html(comment.content);
                $(commentElementId).css("background-color", "#FFF");
              }
            };
            
            self.postComment = function () {
              HopssiteService.postComment(self.selectedDataset.publicId, self.newComment).then(function (success) {
                self.newComment = '';
                self.commentEditable = [];
                getComments(self.selectedDataset.publicId);
              }, function (error) {
                console.log("saveComment", error);
              });
            };
            
            self.deleteComment = function (commentId) {
              HopssiteService.deleteComment(self.selectedDataset.publicId, commentId).then(function (success) {
                getComments(self.selectedDataset.publicId);
              }, function (error) {
                console.log("deleteComment", error);
              });
            };

            self.saveComment = function (commentId, index) {
              self.commentEditable[index] = !self.commentEditable[index];
              var commentElementId = "#commentdiv-" + index;
              self.updateComment = $(commentElementId).html();
              //TODO (Ermias): delete if empty
              HopssiteService.updateComment(self.selectedDataset.publicId, commentId, self.updateComment).then(function (success) {
                $(commentElementId).css("background-color", "#FFF");
                self.updateComment = '';
                getComments(self.selectedDataset.publicId);
              }, function (error) {
                console.log("saveComment", error);
              });
            };
            
            var postCommentIssue = function (commentId, commentIssue) {
              HopssiteService.postCommentIssue(self.selectedDataset.publicId, commentId, commentIssue).then(function (success) {
                console.log("postCommentIssue", success);
              }, function (error) {
                console.log("postCommentIssue", error);
              });
            };
            
            var postDatasetIssue = function (publicId, datasetIssue) {
              HopssiteService.postDatasetIssue(publicId, datasetIssue).then(function (success) {
                console.log("postCommentIssue", success);
              }, function (error) {
                console.log("postCommentIssue", error);
              });
            };
            
            var postRating = function (publicDSId, rating) {
              HopssiteService.postRating(publicDSId, rating).then(function (success) {
                console.log("postRating", success);
              }, function (error) {
                console.log("postRating", error);
              });
            };
            
            self.addPublicDatasetModal = function (dataset) {
              var datasetDto = dataset;
              var projects;
              HopssiteService.getLocalDatasetByPublicId(dataset.publicId).then(function (response) {
                datasetDto = response.data;
                //fetch the projects to pass them in the modal.
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

              }, function (error) {
                  if (typeof error.data.usrMsg !== 'undefined') {
                      growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 8000});
                  } else {
                      growl.error("", {title: error.data.errorMsg, ttl: 8000,  referenceId: 13});
                  }
              });
            };
            
            var initCtrlDela = function () {
              getUser();
              getClusterId();
              getDisplayCategories();
              if (self.publicDSId !== undefined) {
                getDataset(self.publicDSId);
              }
              self.selectDisplayCategory({'categoryName': 'ALL', 'displayName': 'All', 'parentCategory': false}); //open by default
            };
            
            var initCtrl = function () {
              self.displayCategories = [{'categoryName': 'all', 'displayName': 'All'}];
              if (self.publicDSId !== undefined) {
                getDatasetFromLocal(self.publicDSId);
              }
            };

            initCtrlDela();


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
              return convertSize(fileSizeInBytes);
            };

          }]);


