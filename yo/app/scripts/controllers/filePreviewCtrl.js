angular.module('hopsWorksApp')
        .controller('FilePreviewCtrl', ['$modalInstance', 'DataSetService', 'growl', 'fileName', 'filePath', 'projectId',
          function ($modalInstance, DataSetService, growl, fileName, filePath, projectId) {
            var self = this;

            self.filePath = filePath;
            self.fileName = fileName;
            self.projectId = projectId;
            self.fileContents = [];
            self.fileType;
            self.init = function () {
              var dataSetService = DataSetService(self.projectId); //The datasetservice for the current project.
              dataSetService.filePreview(filePath).then(
                      function (success) {
                        self.fileType =  success.data.status;
                        self.fileContents = success.data.successMessage;
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Could not get file contents', ttl: 5000, referenceId: 23});
              });
            };
            self.init();

            self.close = function () {
              $modalInstance.dismiss('cancel');
            };
          }]);

