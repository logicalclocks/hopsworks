angular.module('hopsWorksApp')
    .controller('ShareDatasetCtrl', ['$scope','$uibModalInstance', 'DataSetService', '$routeParams', 'growl', 'ProjectService', 'dsName', 'ModalService',
        function ($scope, $uibModalInstance, DataSetService, $routeParams, growl, ProjectService, dsName, ModalService) {

            var self = this;

            self.datasets = [];
            self.projects = [];
            self.dataSet = {'name': dsName, 'description': "", 'projectId': "", 'editable':false};
            self.pId = $routeParams.projectID;
            var dataSetService = DataSetService(self.pId);

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
                    self.dataSet.editable = false;
                }

            };
            
            self.makeEditable = function () {
                if ($scope.dataSetForm.$valid) {
                    dataSetService.makeEditable(self.dataSet)
                        .then(function (success) {
                            $uibModalInstance.close(success);
                        },
                        function (error) {
                            growl.error(error.data.errorMsg, {title: 'Error', ttl: 15000});
                        });
                } else {
                    self.dataSet.editable = false;
                }

            };

          self.removeEditable = function () {
                if ($scope.dataSetForm.$valid) {
                    dataSetService.removeEditable(self.dataSet)
                        .then(function (success) {
                            $uibModalInstance.close(success);
                        },
                        function (error) {
                            growl.error(error.data.errorMsg, {title: 'Error', ttl: 15000});
                        });
                } else {
                    self.dataSet.editable = false;
                }

            };
            
              /**
             * Opens a modal dialog to make dataset editable
             * @returns {undefined}
             */
            self.makeEditableModal = function () {
              ModalService.makeEditable('md', dsName).then(
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