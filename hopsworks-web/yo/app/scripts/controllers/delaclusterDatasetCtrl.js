'use strict';

angular.module('hopsWorksApp')
        .controller('ClusterDatasetCtrl', ['$location', '$anchorScroll', '$scope', '$rootScope',
          'showdown', 'md5', 'ModalService', 'HopssiteService', 'DelaService', 'DelaClusterService','ProjectService', 'growl',
          function ($location, $anchorScroll, $scope, $rootScope, showdown, md5, ModalService,
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

            var initCtrl = function () {
              self.displayCategories = [{'categoryName': 'all', 'displayName': 'All'}];
              if (self.publicDSId !== undefined) {
                getDatasetFromLocal(self.publicDSId);
              }
            };
            
            initCtrl();
            
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


