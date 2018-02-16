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

angular.module('hopsWorksApp').controller('AttachTemplateCtrl',
        ['$routeParams', '$uibModalInstance', 'file', 'templateId', 'DataSetService',
          function ($routeParams, $uibModalInstance, file, templateId, DataSetService) {

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
              $uibModalInstance.dismiss('cancelled');
            };

            self.skipThisStep = function(){
              self.skipTemplate = true;
            };
            
            self.getTemplate = function () {
              if(self.skipTemplate === false && self.templateId === -1) {
                return false;
              }

              $uibModalInstance.close({templateId: self.templateId});
            };
          }]);