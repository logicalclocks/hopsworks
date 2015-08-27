angular.module('hopsWorksApp')
    .controller('ShareDatasetCtrl', ['$scope','$modalInstance', 'DataSetService', '$routeParams', 'growl', 'ProjectService', 'dsName',
        function ($scope, $modalInstance, DataSetService, $routeParams, growl, ProjectService, dsName) {

            var self = this;

            self.datasets = [];
            self.projects = [];
            self.dataSet = {'name': dsName, 'description': "", 'projectId': "", 'editable':false};
            var pId = $routeParams.projectID;
            var dataSetService = DataSetService(pId);

            ProjectService.getAll().$promise.then(
                function (success) {
                    self.projects = success;
                }, function (error) {
                    growl.error(error.data.errorMsg, {title: 'Error', ttl: 15000});
                }
            );


            self.close = function () {
                $modalInstance.dismiss('cancel');
            };

            self.shareDataset = function () {
                if ($scope.dataSetForm.$valid) {
                    dataSetService.shareDataSet(self.dataSet)
                        .then(function (success) {
                            $modalInstance.close(success);
                        },
                        function (error) {
                            growl.error(error.data.errorMsg, {title: 'Error', ttl: 15000});
                        });
                } else {
                    self.dataSet.editable = false;
                }

            };
        }]);