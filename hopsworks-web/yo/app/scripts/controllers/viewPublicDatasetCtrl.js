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
        .controller('ViewPublicDatasetCtrl', ['$uibModalInstance', 'ProjectService', 'growl', 'projects', 'datasetDto',
          function ($uibModalInstance, ProjectService, growl, projects, datasetDto) {

            var self = this;
            self.projects = projects;
            self.dataset = datasetDto;
            self.request = {'inodeId': "", 'projectId': "", 'message': ""};

            self.importDataset = function () {
              if (self.request.projectId === undefined || self.request.projectId === "") {
                growl.error("Select a project to import the Dataset to", {title: 'Error', ttl: 5000, referenceId: 21});
                return;
              }
              ProjectService.importPublicDataset({}, {'id': self.request.projectId, 
                'inodeId': self.dataset.inodeId, 'projectName': self.dataset.projectName}).$promise.then(
                      function (success) {
                        growl.success("Dataset Imported", {title: 'Success', ttl: 1500});
                        $uibModalInstance.close(success);
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000, referenceId: 21});
              });
            };

            self.close = function () {
              $uibModalInstance.dismiss('cancel');
            };
          }]);

