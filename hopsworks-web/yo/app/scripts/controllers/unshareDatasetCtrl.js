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
    .controller('UnshareDatasetCtrl', ['$scope','$uibModalInstance', 'DataSetService', '$routeParams', 'growl', 'ProjectService', 'dsName',
        function ($scope, $uibModalInstance, DataSetService, $routeParams, growl, ProjectService, dsName) {

            var self = this;

            self.datasets = [];
            self.projects = [];
            self.dataSet = {'name': dsName, 'description': "", 'projectId': $routeParams.projectID, 'permissions':'OWNER_ONLY'};
            self.dataSets = {'name': dsName, 'description': "", 'projectIds': [], 'permissions':'OWNER_ONLY'};
            self.pId = $routeParams.projectID;
            var dataSetService = DataSetService(self.pId);

            dataSetService.projectsSharedWith(self.dataSet).then(
                function (success) {
                    self.projects = success.data;
                }, function (error) {
                    growl.error(error.data.errorMsg, {title: 'Error', ttl: 15000});
                }
            );


            self.close = function () {
                $uibModalInstance.dismiss('cancel');
            };

            self.unshareDataset = function () {
                if ($scope.dataSetForm.$valid) {
                    dataSetService.unshareDataSet(self.dataSets)
                        .then(function (success) {
                            $uibModalInstance.close(success);
                        },
                        function (error) {
                            growl.error(error.data.errorMsg, {title: 'Error', ttl: 15000});
                        });
                }

            };
            
            $scope.omitCurrentProject = function (project) {
              var id = parseInt(self.pId);
              return project.id !== id;
            };
        }]);