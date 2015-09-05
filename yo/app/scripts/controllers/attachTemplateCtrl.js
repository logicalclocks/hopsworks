/*jshint undef: false, unused: false, indent: 2*/
/*global angular: false */

'use strict';

angular.module('hopsWorksApp').controller('AttachTemplateCtrl',
        ['$routeParams', '$modalInstance', 'file', 'templateId', 'DataSetService',
          function ($routeParams, $modalInstance, file, templateId, DataSetService) {

            var self = this;

            self.templateId = templateId;
            self.selectedTemplate = {};
            self.templates = [];
            self.noTemplates = false;
            
            var dataSetService = DataSetService($routeParams.projectID);

            dataSetService.fetchAvailableTemplatesforInode(file.id)
                    .then(function(response){
              
                      angular.forEach(response.data, function (value, key) {
                        self.templates.push(value);
                      });
                      
                      if(self.templates.length === 0){
                        self.noTemplates = true;
                      }
            });

            self.update = function () {
              
              self.templateId = self.selectedTemplate.templateId;
            };

            self.close = function () {
              $modalInstance.dismiss('cancelled');
            };

            self.skipThisStep = function(){
              self.skipTemplate = true;
            };
            
            self.getTemplate = function () {
              if(self.skipTemplate === false && self.templateId === -1) {
                return false;
              }

              $modalInstance.close({templateId: self.templateId});
            };
          }]);