angular.module('hopsWorksApp')
        .controller('FilePreviewCtrl', ['$modalInstance', 'DataSetService', 'growl', 'filePath', 'projectId',
          function ($modalInstance, DataSetService, growl, filePath, projectId) {
            var self = this;

            self.filePath = filePath;
            self.projectId = projectId;
            self.fileContents = [];
           
            self.init = function () {
              var dataSetService = DataSetService(self.projectId); //The datasetservice for the current project.
              dataSetService.filePreview(filePath).then(
                      function (success) {
                        self.fileContents = success.data.successMessage;
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Could not get file contents', ttl: 5000, referenceId: 21});
              });
            };
            self.init();

            self.close = function () {
              $modalInstance.dismiss('cancel');
            };
          }]);

