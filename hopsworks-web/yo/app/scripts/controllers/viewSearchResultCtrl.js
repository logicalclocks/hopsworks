angular.module('hopsWorksApp')
        .controller('ViewSearchResultCtrl', ['$scope', '$uibModalInstance',
          'RequestService', 'DataSetService', 'growl', 'response',
          'result', 'projects', '$showdown', 'DelaProjectService', 'DelaService', 'HopssiteService',
          'ProjectService', 'ModalService', '$routeParams', '$location', '$rootScope',
          function ($scope, $uibModalInstance, RequestService, DataSetService,
                  growl, response, result, projects, $showdown, DelaProjectService, DelaService, HopssiteService,
                  ProjectService, ModalService, $routeParams, $location, $rootScope) {
            var self = this;
            self.request = {'inodeId': "", 'projectId': "", 'message': ""};
            self.projects = projects;
            self.content = result;
            self.spinner = false;
            self.result = response;
            self.request.projectId = self.result.projectId;
            self.thisProjectId = $routeParams.projectID;
            if (result.type === 'proj') {
              self.type = 'Project';
              self.requestType = 'Send join request';
              self.infoMembers = 'Members in this project.';
              self.infoDS = 'Datasets in this project.';
              self.sendRequest = function () {
                RequestService.joinRequest(self.request).then(
                        function (success) {
                          $uibModalInstance.close(success);
                        }, function (error) {
                  growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000, referenceId: 21});
                });
              };
            } else if ((result.type === 'inode' || result.type === 'ds') && !result.public_ds) {
              self.type = 'Dataset';
              self.requestType = 'Send access request';
              self.infoMembers = 'Members of the owning project.';
              self.infoDS = 'Projects this dataset is shared with.';
              self.request.inodeId = self.result.inodeId;
              self.sendRequest = function () {
                RequestService.accessRequest(self.request).then(function (success) {
                  $uibModalInstance.close(success);
                }, function (error) {
                  growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000, referenceId: 21});
                });
              };
            } else if ((result.type === 'inode' || result.type === 'ds') &&
                    result.public_ds && (result.localDataset || result.downloaded)) {
              self.type = 'Public Dataset';
              self.requestType = 'Add DataSet';
              self.infoMembers = 'Members of the owning project.';
              self.infoDS = 'Projects this dataset is shared with.';
              self.request.inodeId = self.result.inodeId;
              self.sendRequest = function () {
                if (self.request.projectId === undefined || self.request.projectId === "") {
                  growl.error("Select a project to import the Dataset to", {title: 'Error', ttl: 5000, referenceId: 21});
                  return;
                }
                ProjectService.getDatasetInfo({inodeId: result.id}).$promise.then(
                        function (response) {
                          var datasetDto = response;
                          ProjectService.importPublicDataset({}, {'id': self.request.projectId,
                            'inodeId': datasetDto.inodeId, 'projectName': datasetDto.projectName}).$promise.then(
                                  function (success) {
                                    growl.success("Dataset Imported", {title: 'Success', ttl: 1500});
                                    $uibModalInstance.close(success);
                                  }, function (error) {
                            growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000, referenceId: 21});
                          });
                        }, function (error) {
                  console.log('Error: ', error);
                });
              };
            } else if ((result.type === 'inode' || result.type === 'ds') &&
                    result.public_ds && !result.localDataset && !result.downloaded) {
              self.type = 'Public Dataset';
              self.requestType = 'Download';
              self.infoMembers = 'Members of the owning project.';
              self.infoDS = 'Projects this dataset is shared with.';
              self.request.inodeId = self.result.inodeId;
              self.sendRequest = function () {
                if (self.request.projectId === undefined || self.request.projectId === "") {
                  growl.error("Select a project to import the Dataset to", {title: 'Error', ttl: 5000, referenceId: 21});
                  return;
                }
                var params = {size: 'md', projectId: self.request.projectId, datasetId: result.publicId,
                  defaultDatasetName: result.name, bootstrap: result.partners};
                ModalService.setupDownload(params).then(function (success) {
                }, function (error) {
                  if (error.data && error.data.details) {
                    growl.error(error.data.details, {title: 'Error', ttl: 1000});
                  }
                  self.delaHopsService = new DelaProjectService(self.request.projectId);
                  self.delaHopsService.unshareFromHops(result.publicId, false).then(function (success) {
                    growl.info("Download cancelled.", {title: 'Info', ttl: 1000});
                  }, function (error) {
                    growl.warning(error, {title: 'Warning', ttl: 1000});
                  });
                });
              };
            }

            var dataSetService = DataSetService(self.projectId);
            $scope.readme = null;

            self.getReadme = function () {
              self.spinner = false;
              if ($scope.readme !== null) {
                return;
              }
              if (self.content.id === undefined) {
                $scope.readme = null;
                return;
              }
              if (self.content.localDataset) {
                self.spinner = true;
                console.log("getReadme view search result: ", self.content);
                HopssiteService.getReadmeByInode(self.content.id).then(
                  function (success) {
                    var content = success.data.content;
                    self.spinner = false;
                    $scope.readme = $showdown.makeHtml(content);
                  }, function (error) {
                    //To hide README from UI
                    self.spinner = false;
                    growl.error(error.data.errorMsg, {title: 'Error retrieving README file', ttl: 5000, referenceId: 3});
                    $scope.readme = null;
                });
              } else {
                self.spinner = true;
                DelaService.getReadme(self.content.publicId, self.content.bootstrap).then(function (success) {
                  self.spinner = false;
                  var content = success.data.content;
                  $scope.readme = $showdown.makeHtml(content);
                }, function (error) {
                  self.spinner = false;
                  growl.error(error.data.errorMsg, {title:'Error retrieving README file', ttl: 5000, referenceId: 3});
                  $scope.readme = null;
                });
              }
            };

            self.sizeOnDisk = function (fileSizeInBytes) {
              return convertSize(fileSizeInBytes);
            };

            self.close = function () {
              $uibModalInstance.dismiss('cancel');
            };

            self.goto = function () {
              var splitedPath = self.content.details.path.split("/");
              var parentPath = "/";

              for (var i = 3; i < splitedPath.length - 1; i++) {
                parentPath = parentPath + splitedPath[i] + "/";
              }
              $rootScope.selectedFile = splitedPath[splitedPath.length - 1];

              $location.path("/project/" + self.thisProjectId + "/datasets" + parentPath);

              $uibModalInstance.dismiss('close');
            };
          }]);

