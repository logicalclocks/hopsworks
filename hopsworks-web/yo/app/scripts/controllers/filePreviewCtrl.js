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

