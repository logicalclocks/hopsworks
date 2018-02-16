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

