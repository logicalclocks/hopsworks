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

var mainModule = angular.module('hopsWorksApp')
        .controller('DetachTemplateCtrl',
                ['$uibModalInstance', '$routeParams', 'file', 'templateId', 'DataSetService',
                  function ($uibModalInstance, $routeParams, file, templateId, DataSetService) {

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

                      $uibModalInstance.close({templateId: self.selectedTemplate.templateId, fileId: self.fileId});
                    };

                    self.hitEnter = function (evt) {
                      if (angular.equals(evt.keyCode, 13)) {
                        self.detachTemplate();
                      }
                    };

                    self.cancel = function () {
                      $uibModalInstance.dismiss('canceled');
                    };

                    self.update = function () {
                    };

                  }]);