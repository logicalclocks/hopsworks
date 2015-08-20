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
                    self.templateContents = {};
                    self.toDownload;
                    self.blob;

                    MetadataActionService.fetchTemplates($cookies['email'])
                            .then(function (response) {
                              var temps = JSON.parse(response.board);
                              angular.forEach(temps.templates, function (value, key) {
                                self.templates.push(value);
                              });
                              console.log("FETCHED TEMPLATES " + JSON.stringify(self.templates));
                            }, function (error) {
                              console.log("Error " + JSON.stringify(error));
                            });

                    self.onSelectUpdate = function () {

                      var selectedTmpt = self.selectedTemplate;
                      var selectedTmptName = selectedTmpt.name;
                      
                      //get the actual template
                      MetadataActionService.fetchTemplate($cookies['email'], selectedTmpt.id)
                              .then(function (response) {
                                var contents = JSON.parse(response.board);
                                self.templateContents.templateName = selectedTmptName;
                                self.templateContents.templateContents = contents.columns;

                                //clear any previously created urls
                                if (!angular.isUndefined(self.blob)) {
                                  (window.URL || window.webkitURL).revokeObjectURL(self.blob);
                                }
                                
                                //construct the url that downloads the template
                                self.toDownload = JSON.stringify(self.templateContents);
                                self.blob = new Blob([self.toDownload], {type: 'text/plain'});
                                self.url = (window.URL || window.webkitURL).createObjectURL(self.blob);
                                //console.log("URL CREATED " + JSON.stringify(self.url));
                              });
                    };

                    //File upload handling functions
                    self.path = 'Uploads';
                    self.target = '/hopsworks/api/template/upload/' + self.path;

                    self.size = function (fileSizeInBytes) {
                      return convertSize(fileSizeInBytes);
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