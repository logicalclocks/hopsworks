angular.module('hopsWorksApp')
        .controller('ViewPublicDatasetCtrl', ['$modalInstance', 'ProjectService', 'DataSetService', 'growl', 'projects', 'datasetDto',
          function ($modalInstance, ProjectService, DataSetService, growl, projects, datasetDto) {

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
                        $modalInstance.close(success);
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000, referenceId: 21});
              });
            };

            var dataSetService = DataSetService(self.projectId);

            self.close = function () {
              $modalInstance.dismiss('cancel');
            };
          }]);

