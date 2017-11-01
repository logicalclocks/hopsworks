angular.module('hopsWorksApp')
        .controller('FilePreviewCtrl', ['$uibModalInstance','DataSetService', 'growl', 'fileName', 'filePath', 'projectId', 'mode',
          function ($uibModalInstance, DataSetService, growl, fileName, filePath, projectId, mode) {
            var self = this;
            self.modes = ['head','tail'];
            self.filePath = filePath;
            self.fileName = fileName;
            self.projectId = projectId;
            self.content;
            self.type;
            self.extension;
            self.fileDetails;
            self.mode; //Head or Tail the file
            self.fetchFile = function (mode) {
              var dataSetService = DataSetService(self.projectId); //The datasetservice for the current project.
              dataSetService.filePreview(filePath, mode).then(
                      function (success) {
//                                .replace(/\\/g, '\\\\')
//                                .replace(/\"/g, '\\"')
//                                .replace(/\//g, '\\/')
//                                .replace(/\b/g, '\\b')
//                                .replace(/\f/g, '\\f')
//                                .replace(/\n/g, '\\n')
//                                .replace(/\r/g, '\\r')
//                                .replace(/\t/g, '\\t')
                        self.mode = mode;
                        self.fileDetails = JSON.parse(success.data.data);

                        self.type = self.fileDetails.filePreviewDTO[0].type;
                        self.content = self.fileDetails.filePreviewDTO[0].content;
                        self.extension = self.fileDetails.filePreviewDTO[0].extension;
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Could not get file contents', ttl: 5000, referenceId: 23});
              });
            };
            self.fetchFile(mode);

            self.close = function () {
              $uibModalInstance.dismiss('cancel');
            };
          }]);

