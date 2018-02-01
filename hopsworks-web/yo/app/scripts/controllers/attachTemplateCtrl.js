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