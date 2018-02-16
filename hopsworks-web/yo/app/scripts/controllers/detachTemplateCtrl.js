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