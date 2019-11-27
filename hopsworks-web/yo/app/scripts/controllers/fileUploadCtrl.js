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

/*jshint undef: false, unused: false, indent: 2*/
/*global angular: false */

'use strict';

angular.module('hopsWorksApp')
        .controller('FileUploadCtrl', ['$uibModalInstance', '$scope', 'growl', 'flowFactory', 'DataSetService', 'AuthService', 'projectId', 'path', 'templateId', 'datasetType',
          function ($uibModalInstance, $scope, growl, flowFactory, DataSetService, AuthService, projectId, path, templateId, datasetType) {

            var self = this;
            self.model = {};
            self.projectId = projectId;
            self.path = path;
            self.datasetType = datasetType;
            self.templateId = templateId;
            self.errorMsg;
            self.files = {};
            
            self.datasets = [];
            self.selectedTemplate = {};
            self.temps = [{'temp': "temp"}];

            self.target = getApiPath() + '/project/' + self.projectId + '/dataset/upload/' + self.path;
            var dataSetService = DataSetService(self.projectId); //The datasetservice for the current project.
            self.size = function (fileSizeInBytes) {
              return convertSize (fileSizeInBytes);
            };
                     

            self.existingFlowObject = flowFactory.create({
              target: self.target,
              query: {templateId: self.templateId}
            });

            self.update = function () {
              console.log("NEW TEMPLATE SELECTED " + JSON.stringify(self.selectedTemplate));
            };

            self.target = function (FlowFile, FlowChunk, isTest) {
              return getApiPath() + '/project/' + self.projectId + '/dataset/upload/' + self.path + '/' + self.selectedTemplate.id;
            };

            self.existingFlowObject.on('fileProgress', function (file, chunk) {
              var token = chunk.xhr.getResponseHeader('Authorization');
              if (token) {
                console.log("xhr: Authorization header updated.");
                AuthService.saveToken(token);
              }
            });

            self.fileErrorHandler = function (file, message, flow) {
              var msg = JSON.parse(message);
              self.errorMsg = msg.errorMsg;
              self.files[file.name] = msg.errorMsg;
            };

            self.errorHandler = function (file, message, flow) {
              var msg = JSON.parse(message);
                if (typeof msg.usrMsg !== 'undefined') {
                    growl.error(msg.usrMsg, {title: msg.errorMsg, ttl: 8000});
                } else {
                    growl.error("", {title: msg.errorMsg, ttl: 8000});
                }
            };

            self.fileAddedHandler = function (file, flow) {
              self.files[file.name] = '';
            };
            
            self.flowCompleteHandler = function (flow){
              var length = flow.files.length;
              for(var i=0; i<length; i++){
                var file = flow.files[i];
                if(file.progress()!=1){
                  return;
                }
                if(file.error){
                  return;
                }
              }
              $uibModalInstance.close("All files uploaded successfully");
            };
            
            self.closeModal = function (flow) {
              var uploading = flow.isUploading();
              if (uploading) {
                growl.info("Upload in progress, pause or cancel transfers first...", {ttl: 5000, referenceId: 1});
              } else {
                //Check if files were uploaded successfully and delete wrong ones
                for (var file in self.files) {
                  var remPath = self.path + "/" + file;
                  dataSetService.deleteCorrupted(remPath).then(function (success) {
                          }, function (error) {
                  });
                }
                $uibModalInstance.close();
              }
            };
            
          }]);
