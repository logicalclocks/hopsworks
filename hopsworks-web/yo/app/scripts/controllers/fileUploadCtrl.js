/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
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
