/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
 */

angular.module('hopsWorksApp')
    .controller('ShareDatasetCtrl', ['$scope','$uibModalInstance', 'DataSetService', '$routeParams', 'growl', 'ProjectService', 'dsName', 'permissions', 'ModalService',
        function ($scope, $uibModalInstance, DataSetService, $routeParams, growl, ProjectService, dsName, permissions, ModalService) {

            var self = this;
            self.datasets = [];
            self.projects = [];
            self.dataSet = {'name': dsName, 'description': "", 'projectId': "", 'permissions': permissions};
            self.pId = $routeParams.projectID;
            var dataSetService = DataSetService(self.pId);
            var defaultPermissions = 'OWNER_ONLY';
            
            self.ownerOnlyMsg = "Sets default permissions setting of a Dataset. " +
                    "Only Data Owners will be able to upload/remove files, either via the Dataset Browser or via Jobs and Jupyter notebooks. "+
                    "To allow Zeppelin notebooks to write in this Dataset, select a less strict setting.";
            self.groupWritableAndStickyBitSet = "Allow Data Owners to upload/remove files, Data Scientists are allowed to upload files but only remove files/dirs they own, via the Dataset Browser. "+
                    "Zeppelin notebooks can write into the Dataset.<br> Are you sure you want to proceed?";
            self.groupWritable = "This is the least strict setting. It allows both Data Owners and Data Scientists to upload/remove files either via the Dataset Browser or via Jobs/Notebooks. <br> Are you sure you want to proceed?";

            ProjectService.getAll().$promise.then(
                function (success) {
                    self.projects = success;
                }, function (error) {
                    growl.error(error.data.errorMsg, {title: 'Error', ttl: 15000});
                }
            );


            self.close = function () {
                $uibModalInstance.dismiss('cancel');
            };

            self.shareDataset = function () {
                if ($scope.dataSetForm.$valid) {
                    dataSetService.shareDataSet(self.dataSet)
                        .then(function (success) {
                            $uibModalInstance.close(success);
                        },
                        function (error) {
                            growl.error(error.data.errorMsg, {title: 'Error', ttl: 15000});
                        });
                } else {
                    self.dataSet.permissions = defaultPermissions;
                }

            };
            
            self.setPermissions = function () {
                if ($scope.dataSetForm.$valid) {
                    dataSetService.permissions(self.dataSet)
                        .then(function (success) {
                            $uibModalInstance.close(success);
                        },
                        function (error) {
                            growl.error(error.data.errorMsg, {title: 'Error', ttl: 15000});
                        });
                } else {
                    self.dataSet.permissions = defaultPermissions;
                }

            };
            
            self.getPermissionsText = function() {
              if(typeof self.dataSet !== 'undefined'){
                if(self.dataSet.permissions === 'GROUP_WRITABLE_SB'){
                  return self.groupWritableAndStickyBitSet;
                } else if(self.dataSet.permissions === 'GROUP_WRITABLE'){
                  return self.groupWritable;
                } else if(self.dataSet.permissions === 'OWNER_ONLY'){
                  return self.ownerOnlyMsg;
                } 
              }
            };
            
              /**
             * Opens a modal dialog to make dataset editable
             * @param {type} permissions
             * @returns {undefined}
             */
            self.setPermissionsModal = function (permissions) {
              ModalService.permissions('md', dsName, permissions).then(
                      function (success) {
                        growl.success(success.data.successMessage, {title: 'Success', ttl: 5000});
                      }, function (error) {
              });
            };
            
            $scope.omitCurrentProject = function (project) {
              var id = parseInt(self.pId);
              return project.id !== id;
            };
        }]);