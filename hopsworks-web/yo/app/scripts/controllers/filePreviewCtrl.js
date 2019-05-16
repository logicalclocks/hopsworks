/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
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
            self.imgHeight=400;
            self.imgWidth=400;
            self.loading = false;
            self.fetchFile = function (mode) {
              self.loading = true;
              var dataSetService = DataSetService(self.projectId); //The datasetservice for the current project.
              dataSetService.getDatasetBlob(filePath, mode).then(
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
                        self.fileDetails = success.data;
                        self.type = self.fileDetails.preview.type;
                        self.content = self.fileDetails.preview.content;
                        self.extension = self.fileDetails.preview.extension;
                        self.loading = false;
                        if (self.type === 'image') {
                            self.getImageWidthHeight();
                        }
                      }, function (error) {
                         self.loading = false;
                         growl.error(error.data.errorMsg, {title: 'Could not get file contents', ttl: 5000, referenceId: 23});
              });
            };
            self.fetchFile(mode);
            
            self.getImageWidthHeight = function() {
              var myImg = document.querySelector("#image");
              if (myImg) {
                  self.imgWidth = myImg.naturalWidth;
                  self.imgHeight = myImg.naturalHeight;
              }

            };

            self.getImageSrc = function () {
                return 'data:image/' + self.extension +';base64,'+ self.content;
            };
                        

            self.close = function () {
              $uibModalInstance.dismiss('cancel');
            };
          }]);

