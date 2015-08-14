/*jshint undef: false, unused: false, indent: 2*/
/*global angular: false */

'use strict';

angular.module('hopsWorksApp')
        .controller('ImportExportTemplateCtrl', 
          ['$scope', '$cookies', '$modalInstance', 'growl', 'MetadataActionService', 'flowFactory', 'template',
          function ($scope, $cookies, $modalInstance, growl, MetadataActionService, flowFactory, template) {

            var self = this;
            self.model = {};
            self.errorMsg;
            self.files = {};

            //selectedTemplate is bound to the front end component
            self.selectedTemplate = {};
            self.templates = [];
            self.template = {};

            MetadataActionService.fetchTemplates($cookies['email'])
                    .then(function (response) {
                      var temps = JSON.parse(response.board);
                      angular.forEach(temps.templates, function (value, key) {
                        self.templates.push(value);
                      });
                      console.log("FETCHED TEMPLATES " + JSON.stringify(self.templates));
                    }, function (error) {
                      console.log("ERROR " + JSON.stringify(error));
                    });

            self.onSelectUpdate = function () {
              console.log("SELECTED TEMPLATE " + JSON.stringify(self.selectedTemplate));
            };
            
            self.path = 'Uploads';
            //File upload handling functions
            self.target = '/hopsworks/api/template/upload/' + self.path;

            self.size = function (fileSizeInBytes) {
              return convertSize (fileSizeInBytes);
            };
                  
            self.existingFlowObject = flowFactory.create({
              target: self.target//,
              //query: {template: self.selectedTemplate}
            });

            self.target = function (FlowFile, FlowChunk, isTest) {
              return '/hopsworks/api/template/upload/' + self.path;
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
            
            self.close = function () {
              $modalInstance.dismiss('cancel');
              console.log("closing the window");
            };

          }]);