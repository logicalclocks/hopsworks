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