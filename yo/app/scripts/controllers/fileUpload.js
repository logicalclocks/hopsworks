/*jshint undef: false, unused: false, indent: 2*/
/*global angular: false */

'use strict';

angular.module('hopsWorksApp')
        .controller('FileUploadCtrl', ['$modalInstance', '$scope', 'projectId', 'path', 'templateId', 'growl', 'flowFactory', 
        function ($modalInstance, $scope, projectId, path, templateId, growl, flowFactory) {
          
            var self = this;
            self.model = {};
            self.projectId = projectId;
            self.path = path;
            self.templateId = templateId;
            self.errorMsg;
            self.files = {};

            self.datasets = [];
            self.selectedTemplate = {};
            self.temps = [{'temp':"temp"}];
            
            self.target = '/hopsworks/api/project/' + self.projectId + '/dataset/upload/' + self.path;
            
            self.size = function (fileSizeInBytes) {
              if (fileSizeInBytes === 0) {
                return 0;
              }
              var i = -1;
              var byteUnits = ['kB', ' MB', ' GB', ' TB', 'PB', 'EB', 'ZB', 'YB'];
              do {
                fileSizeInBytes = fileSizeInBytes / 1024;
                i++;
              } while (fileSizeInBytes > 1024);

              return Math.max(fileSizeInBytes, 0.1).toFixed(1) + byteUnits[i];
            };

            self.existingFlowObject = flowFactory.create({
              target: self.target,
              query: {templateId: self.templateId}
            });
            
            self.update = function(){
              console.log("NEW TEMPLATE SELECTED " + JSON.stringify(self.selectedTemplate));
            };
            
            self.target = function (FlowFile, FlowChunk, isTest) {
              return '/hopsworks/api/project/'+ self.projectId  +'/dataset/upload/'+ self.path + '/' + self.selectedTemplate.id;
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
          }]);
