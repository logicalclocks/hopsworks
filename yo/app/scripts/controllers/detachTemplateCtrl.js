/*jshint undef: false, unused: false, indent: 2*/
/*global angular: false */

'use strict';

var mainModule = angular.module('hopsWorksApp')
        .controller('DetachTemplateCtrl',
                ['$modalInstance', '$routeParams', 'file', 'templateId', 'DataSetService',
                  function ($modalInstance, $routeParams, file, templateId, DataSetService) {

                    var self = this;

                    self.fileId = file.id;
                    self.filename = file.name;
                    self.parentId = file.parentId;
                    self.templateId = templateId;
                    
                    self.templates = [];
                    self.selectedTemplate;
                    self.noTemplates = false;
                    
                    var dataSetService = DataSetService($routeParams.projectID);

                    dataSetService.fetchTemplatesForInode(self.fileId)
                            .then(function (response) {
                              self.templates = response.data;
                              
                              if(self.templates.length === 0){
                                self.noTemplates = true;
                              }
                            });

                    self.detachTemplate = function () {
                      if (angular.isUndefined(self.selectedTemplate)) {
                        return false;
                      }

                      $modalInstance.close({templateId: self.selectedTemplate.templateId, fileId: self.fileId});
                    };

                    self.hitEnter = function (evt) {
                      if (angular.equals(evt.keyCode, 13)) {
                        self.detachTemplate();
                      }
                    };

                    self.cancel = function () {
                      $modalInstance.dismiss('canceled');
                    };

                    self.update = function () {
                    };

                  }]);