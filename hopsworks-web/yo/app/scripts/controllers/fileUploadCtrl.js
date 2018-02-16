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

/*jshint undef: false, unused: false, indent: 2*/
/*global angular: false */

'use strict';

angular.module('hopsWorksApp')
        .controller('FileUploadCtrl', ['$uibModalInstance', '$scope', 'projectId', 'path', 'templateId', 'growl', 'flowFactory', 'DataSetService',
          function ($uibModalInstance, $scope, projectId, path, templateId, growl, flowFactory, DataSetService) {

            var self = this;
            self.model = {};
            self.projectId = projectId;
            self.path = path;
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

            self.fileErrorHandler = function (file, message, flow) {
              var msg = JSON.parse(message);
              self.errorMsg = msg.errorMsg;
              self.files[file.name] = msg.errorMsg;
            };

            self.errorHandler = function (file, message, flow) {
              var msg = JSON.parse(message);
              growl.error(msg.errorMsg, {title: 'Error', ttl: 5000, referenceId: 1});
            };

            self.fileAddedHandler = function (file, flow) {
              console.log(file.name);
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
                  var remPath = 'corrupted/' + self.path + "/" + file;
                  dataSetService.removeDataSetDir(remPath).then(
                          function (success) {
                          }, function (error) {
                  });
                }
                $uibModalInstance.close();
              }
            };
            
          }]);
