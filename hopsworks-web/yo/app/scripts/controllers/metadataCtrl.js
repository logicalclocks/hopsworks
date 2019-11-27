/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
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
 */

'use strict';

angular.module('hopsWorksApp')
        .controller('MetadataCtrl', ['$cookies', '$uibModal', '$scope', '$rootScope', '$routeParams', '$filter',
            'ModalService', 'growl', 'MetadataActionService', 'MetadataRestService', 'MetadataHelperService',
            'TemplateService',
          function ($cookies, $uibModal, $scope, $rootScope, $routeParams, $filter, ModalService, growl,
                    MetadataActionService, MetadataRestService, MetadataHelperService, TemplateService) {

            var self = this;
            self.metaData = {};
            self.metaDataDetail = {};
            self.currentFile = MetadataHelperService.getCurrentFile();
            self.tabs = [];
            self.meta = [];
            self.metainfo = [];
            self.visibilityInfo = [];
            self.availableTemplates = [];
            self.newTemplateName = "";
            self.extendedTemplateName = "";
            self.currentTableId = -1;
            self.currentTemplateID = -1;
            self.currentFileTemplates = [];
            self.selectedTemplate = {};
            self.editedField;
            self.toExtend = -1;
            self.currentBoard = {};
            self.toDownload;
            self.blob;
            self.templateContents = {};
            self.editingTemplate = false;
            self.projectInodeid = -1;
            self.noTemplates = false;

            self.attachedDetailedTemplateList = [];

            var templateService = TemplateService($routeParams.projectID);

            //update the current template whenever other users make changes
            var listener = $rootScope.$on("template.change", function (event, response) {
              try {
                var incomingTemplateId = JSON.parse(response.board).templateId;

                if (self.currentTemplateID === incomingTemplateId) {
                  self.currentBoard = JSON.parse(response.board);
                }
              } catch (error) {
                //console.log(error);
              }
            });

            /*
             * Rootscope events are not deregistered when the controller dies.
             * So on the controller destroy event deregister the rootscope listener manually.
             * @returns {undefined}
             */
            $scope.$on("$destroy", function () {
              listener();
            });

            //fetch all the available templates
            MetadataHelperService.fetchAvailableTemplates()
                    .then(function (response) {
                      self.availableTemplates = JSON.parse(response.board).templates;
                      angular.forEach(self.availableTemplates, function (template, key) {
                        template.showing = false;
                      });
                    });

            //get the current project to get its inodeid
//            ProjectService.get({}, {'id': parseInt($routeParams.projectID)})
//                    .$promise.then(
//                            function (success) {
//                              self.projectInodeid = success.inodeid;
//
//                            }, function (error) {
//                    });

            /**
             * submit form data when the 'save' button is clicked or when the enter key is hit
             */
            self.submitMetadata = function () {
              if (!self.metaData) {
                return;
              }
              //after the project inodeid is available proceed to store metadata
              MetadataRestService.addMetadataWithSchema(
                      parseInt(self.currentFile.parentId), self.currentFile.name, self.currentTableId, self.metaData)
                      .then(function (success) {
                        self.fetchMetadataForTemplate();
                      }, function (error) {
                        growl.error("Metadata could not be saved", {title: 'Info', ttl: 1000});
                      });
              //truncate metaData object
              angular.forEach(self.metaData, function (value, key) {
                if (!angular.isArray(value)) {
                  self.metaData[key] = "";
                } else {
                  self.metaData[key] = [];
                }
              });
            };


            self.hideAddTable = function () {
              return self.currentTemplateID !== -1 && self.currentTemplateID !== undefined &&
                      !self.editingTemplate
            };

            self.createMetadata = function (tableId, metadataId) {
              if (!self.metaData) {
                return;
              }
              var value = self.metaData[metadataId];
              if (!value || value.length === 0) {
                growl.info("Metadata field cannot be empty", {title: 'Info', ttl: 3000});
                return;
              }

              var tempInput = {};
              tempInput[metadataId] = value;
              MetadataRestService.addMetadataWithSchema(
                      parseInt(self.currentFile.parentId), self.currentFile.name, self.currentTableId, tempInput)
                      .then(function (success) {
                        self.metaData[metadataId] = '';
                        self.fetchMetadataForTemplate();
                      }, function (error) {
                        growl.error("Metadata could not be saved", {title: 'Info', ttl: 1000});
                        self.metaData[metadataId] = '';
                        self.fetchMetadataForTemplate();
                      });
            };

            /* -- TEMPLATE HANDLING FUNCTIONS -- */
            /**
             * Selects/deselects a template item when the user clicks on it
             * 
             * @param {type} template
             * @returns {undefined}
             */
            self.toggleTemplate = function (template) {
              //disable toggling when a template name is being edited
              if (self.editingTemplate) {
                return;
              }
              //reset all templates showing flag
              angular.forEach(self.availableTemplates, function (temp, key) {
                if (template.id !== temp.id) {
                  temp.showing = false;
                }
              });

              //handle the clicked template accordingly
              if (template.showing === null || template.showing === undefined) {
                template.showing = true;
              }
              template.showing = !template.showing;
              self.currentTemplateID = template.id;

              //if all templates are deselected hide the add new table button t
              // TODO - this is broken
              if (!template.showing) {
                self.currentTemplateID = -1;
                self.currentBoard = {};
              }
            };

            /**
             * Updates a template name
             * 
             * @param {type} template
             * @returns {undefined}
             */
            self.updateTemplateName = function (template) {
              MetadataActionService.updateTemplateName($cookies.get('email'), template)
                      .then(function (response) {
                        self.editingTemplate = false;
                        self.currentTemplateID = -1;
                      });
            };

            /**
             * Creates a new template from an existing one
             * 
             * @returns {undefined}
             */
            self.extendTemplate = function () {

              if (self.checkTemplateAvailability(self.extendedTemplateName)) {
                //don't proceed if there is no selected template to extend
                if (self.toExtend === -1) {
                  growl.info("Select a template first.", {title: 'Info', ttl: 5000});
                  return;
                }

                var templateName = self.extendedTemplateName;
                //store the new template name
                MetadataActionService.addNewTemplate($cookies.get('email'), self.extendedTemplateName)
                        .then(function (data) {
                          var tempTemplates = JSON.parse(data.board);

                          //get the id of the new template
                          var newlyCreatedID = tempTemplates.templates[tempTemplates.numberOfTemplates - 1].id;

                          //get the contents of the template to extend
                          MetadataActionService.fetchTemplate($cookies.get('email'), parseInt(self.toExtend))
                                  .then(function (response) {
                                    var templateToExtend = JSON.parse(response.board);

                                    //associate existing contents with the new template
                                    MetadataActionService.extendTemplate($cookies.get('email'), newlyCreatedID, templateToExtend)
                                            .then(function (data) {
                                              self.extendedTemplateName = "";

                                              //trigger the necessary variable change in the service
                                              MetadataHelperService.fetchAvailableTemplates()
                                                      .then(function (response) {
                                                        self.availableTemplates = JSON.parse(response.board).templates;
                                                        angular.forEach(self.availableTemplates, function (template, key) {
                                                          if (template.name.trim().toLowerCase() === templateName.trim().toLowerCase()) {
                                                            self.toggleTemplate(template);
                                                            self.fetchTemplate(template.id)
                                                          }
                                                        });
                                                      });
//											  template.showing = true;
                                              self.toExtend = -1;
                                              //console.log('Response from extending template: ');
                                              //console.log(data);
                                            });
                                  });
                        });
              }
            };

            /**
             * Fetches a specific template from the database based on its id
             * 
             * @param {type} templateId
             * @returns {undefined}
             */
            self.fetchTemplate = function (templateId) {
              //if all templates are deselected hide the add new table button
              if (self.currentTemplateID === -1) {
                return;
              }

              MetadataActionService.fetchTemplate($cookies.get('email'), templateId)
                      .then(function (success) {
                        /*
                         * sort the objects of the retrieved template by id.
                         * Keeps the objects in a fixed position
                         */
                        var template = JSON.parse(success.board);
                        var sortedTables = sortObject($filter, 'id', template);
                        template.columns = sortedTables;

                        //update the currentBoard upon template retrieval
                        self.currentBoard = template;
                      }, function (error) {
                        console.log('fetchTemplate - error');
                        console.log(JSON.parse(error));
                      });
            };

            /**
             * Persists a template's contents (tables, fields) in the database
             * 
             * @returns {undefined}
             */
            self.storeTemplate = function () {

              MetadataActionService.storeTemplate($cookies.get('email'), self.currentTemplateID, self.currentBoard)
                      .then(function (response) {
                        var template = JSON.parse(response.board);
                        var sortedTables = sortObject($filter, 'id', template);
                        template.columns = sortedTables;

                        self.currentBoard = template;

                      }, function (error) {
                        console.log(error);
                      });
            };

            /**
             * Creates a new template in the database
             * 
             * @returns {undefined}
             */
            self.addNewTemplate = function () {

              var templateName = self.newTemplateName;
              if (self.checkTemplateAvailability(self.newTemplateName)) {

                MetadataActionService.addNewTemplate($cookies.get('email'), self.newTemplateName)
                        .then(function (data) {
                          self.newTemplateName = "";
                          //trigger a variable change (availableTemplates) in the service
                          MetadataHelperService.fetchAvailableTemplates()
                                  .then(function (response) {
                                    self.availableTemplates = JSON.parse(response.board).templates;
                                    angular.forEach(self.availableTemplates, function (template, key) {
                                      if (template.name.trim().toLowerCase() === templateName.trim().toLowerCase()) {
                                        self.toggleTemplate(template);
                                        self.fetchTemplate(template.id)
                                      }
                                    });

                                  });

                        });
              }
            };

            self.showing = function () {
              if (self.selectedTemplate === {})
                return false;

              return self.selectedTemplate.showing === true
            }



            self.checkTemplateAvailability = function (templateName) {

              var isTemplateAvailable = false;
              angular.forEach(self.availableTemplates, function (template, key) {
                if (template.name.trim().toLowerCase() === templateName.trim().toLowerCase()) {
                  isTemplateAvailable = true;
                }
              });

              if (isTemplateAvailable) {
                growl.error("Template name " + templateName.trim() + " already exisits", {title: 'Info', ttl: 5000});
                return false;
              }
              return true;
            }

            /**
             * Deletes a template from the database
             * 
             * @param {type} templateId. The id of the template to be removed
             * @returns {undefined}
             */
            self.removeTemplate = function (templateId) {
              MetadataActionService.removeTemplate($cookies.get('email'), templateId)
                      .then(function (data) {
                        self.selectedTemplate = {};
                        self.currentTemplateID = -1;
                        self.currentBoard = {};
                        //trigger a variable change (availableTemplates) in the service
                        MetadataHelperService.fetchAvailableTemplates();
                        self.availableTemplates = MetadataHelperService.getAvailableTemplates();
                      });
            };

            /**
             * Associates a template to a file. It is a template id to file (inodeid)
             * association
             * 
             * @param {type} file
             * @returns {undefined}
             */
            self.attachTemplate = function (file) {
              var templateId = -1;

              var data = {inodePath: "", templateId: -1};
              data.inodePath = file.path;

              ModalService.attachTemplate('sm', file, templateId)
                      .then(function (success) {
                        data.templateId = success.templateId;

                          templateService.attachTemplate(data).then(
                                function (success) {
                                  growl.success(success.data.successMessage, {title: 'Success', ttl: 1000});
                                  self.setMetadataTemplate(file);
                                }, function (error) {
                          growl.info("Could not attach template to file " + file.name + ".",
                                  {title: 'Info', ttl: 5000});
                        });
                      });
            };

            /**
             * Removes the selected template from the selected inode. Affects the association table
             * 
             * @param {type} file
             * @returns {undefined}
             */
            self.detachTemplate = function (file) {
              var templateId = -1;

              ModalService.detachTemplate('sm', file, templateId)
                      .then(function (success) {

                          templateService.detachTemplate(success.fileId, success.templateId)
                                .then(function (success) {
                                  growl.success(success.data.successMessage, {title: 'Success', ttl: 1000});
                                  self.setMetadataTemplate(file);
                                });
                      });
            };

            /**
             * Fetches all templates attached to a given inode
             * 
             * @param {type} file
             * @returns {undefined}
             */
            self.fetchAttachedTemplates = function () {
              //initialize the variable before fetching the templates.
              //assume by default there are no templates
              self.noTemplates = false;

                templateService.fetchTemplatesForInode(self.currentFile.id)
                      .then(function (response) {
                        self.currentFileTemplates = response.data;

                        if (self.currentFileTemplates.length === 0) {
                          self.noTemplates = true;
                        }
                      });
            };

            /* -- TABLE AND FIELD HANDLING FUNCTIONS ADD/REMOVE -- */
            /**
             * Deletes a table. It is checking first if the table contains any fields
             * and proceeds according to user input
             * 
             * @param {type} column
             * @returns {undefined}
             */
            self.checkDeleteTable = function (column) {
              MetadataActionService.isTableEmpty($cookies.get('email'), column.id)
                      .then(function (response) {

                        if (response.board !== "EMPTY") {
                          ModalService.confirm("sm", "Delete table",
                                  "This table contains fields. Do you really want to delete it?\n\
                                This action cannot be undone.")
                                  .then(function (success) {
                                    self.deleteTable(column);
                                  }, function (cancelled) {
                                    growl.info("Not Deleted", {title: 'Info', ttl: 2000});
                                  });

                          return;
                        }

                        self.deleteTable(column);
                      });
            };

            /**
             * Deletes a table
             * 
             * @param {type} column
             * @returns {undefined}
             */
            self.deleteTable = function (column) {
              MetadataActionService.deleteList($cookies.get('email'), self.currentTemplateID, column)
                      .then(function (success) {
                        self.fetchTemplate(self.currentTemplateID);
                        growl.success("Table " + column.name + " deleted successfully.",
                                {title: 'Success', ttl: 2000});
                      }, function (error) {
                        console.log(error);
                        growl.info("Could not delete table " + column.name +
                                " " + error + ".", {title: 'Info', ttl: 5000});
                      });
            };

            /**
             * Deletes a field. It is checking first if the field contains any raw data
             * and proceeds according to user input
             * 
             * @param {type} column. The table this field resides in
             * @param {type} card. The card going to be deleted
             * @returns {undefined}
             */
            self.checkDeleteField = function (column, card) {
              MetadataActionService.isFieldEmpty($cookies.get('email'), card.id)
                      .then(function (response) {

                        if (response.board !== "EMPTY") {
                          ModalService.confirm("sm", "Delete field",
                                  "This field contains raw data. Do you really want to delete it?\n\
                                This action cannot be undone.")
                                  .then(function (success) {
                                    self.deleteField(column, card);
                                  }, function (cancelled) {
                                    console.log("CANCELED " + JSON.stringify(cancelled));
                                    growl.info("Delete aborted", {title: 'Info', ttl: 5000});
                                  });

                          return;
                        }

                        self.deleteField(column, card);
                      });
            };

            /**
             * Deletes a table field
             * 
             * @param {type} column. The table this field resides in
             * @param {type} card. The field going to be deleted
             * @returns {undefined}
             */
            self.deleteField = function (column, card) {
              MetadataActionService.deleteCard($cookies.get('email'), self.currentTemplateID, column, card)
                      .then(function (success) {

                        self.fetchTemplate(self.currentTemplateID);
                        growl.success("Field " + card.title + " deleted successfully.",
                                {title: 'Success', ttl: 5000});
                      }, function (error) {
                        console.log(error);
                        growl.info("Could not delete field " + card.title +
                                " " + error + ".", {title: 'Info', ttl: 5000});
                      });
            };

            /**
             * Adds a new field to a table
             * 
             * @param {type} templateId
             * @param {type} column
             * @param {type} card
             * @returns {unresolved}
             */
            self.storeCard = function (templateId, column, card) {

              return MetadataActionService.storeCard($cookies.get('email'), templateId, column, card);
            };

            /**
             * Displays the uibModal dialog to creating a new card
             * 
             * @param {type} column
             * @returns {undefined}
             */
            self.addField = function (column) {
              $scope.currentColumn = column;

              ModalService.addNewField($scope)
                      .then(function (field) {

                        MetadataActionService.storeCard($cookies.get('email'), self.currentTemplateID, column, field)
                                .then(function (success) {
//                                  growl.success("Field " + field.title 
//                                  + " saved successfully", {title: 'Success', ttl: 1000});
                                  self.fetchTemplate(self.currentTemplateID);
                                }, function (error) {
                                  console.log(error);
                                  growl.info("Could save field " + field.title + ".", {title: 'Info', ttl: 5000});
                                });
                      });
            };

            /**
             * Displays the uibModal dialog to creating a new table
             * 
             * @returns {undefined}
             */
            self.addNewList = function () {
              $scope.template = self.currentTemplateID;
              $uibModal.open({
                templateUrl: 'views/metadata/newListModal.html',
                controller: 'NewlistCtrl',
                scope: $scope
              })
                      .result.then(function (list) {

                        if (!angular.isUndefined(list)) {

                          //we need to add the new table into the mainboard object
                          self.currentBoard.columns.push(list);

                          MetadataActionService.storeTemplate($cookies.get('email'), self.currentTemplateID, self.currentBoard)
                                  .then(function (response) {
                                    var template = JSON.parse(response.board);
                                    var sortedTables = sortObject($filter, 'id', template);
                                    template.columns = sortedTables;

                                    self.currentBoard = template;
                                  }, function (error) {
                                    console.log(error);
                                  });
                        }
                      });
            };

            /* -- Field handling functions -- */
            /**
             * Makes a field (not)searchable by setting the attribute 'find' accordingly
             * 
             * @param {type} card
             * @returns {undefined}
             */
            self.makeSearchable = function (card) {
              card.find = !card.find;
              self.storeTemplate();
              //console.log("Card " + card.title + " became searchable " + card.find);
            };

            /**
             * Makes a field (not)required by setting the attribute 'required' accordingly
             * 
             * @param {type} card
             * @returns {undefined}
             */
            self.makeRequired = function (card) {
              card.required = !card.required;
              self.storeTemplate();
              //console.log("Card " + card.title + " became required " + card.required);
            };


            /**
             * Configuration object for the ng-sortable directive. Provides some drag n drop callbacks 
             * that help us take control over the objects dragged around 
             */
            self.fieldSortOptions = {
              /*
               * Triggered when an item is moved from one container (table) to another
               */
              itemMoved: function (event) {
                /*
                 * event.dest is the destination object. Handles object moving between different table objects,
                 * resetting their position attribute. 'value' is the field under processing
                 */
                angular.forEach(event.dest.sortableScope.$parent.column.cards, function (value, key) {
                  value.position = (key + 1);
                });

                self.storeTemplate();
              },
              /*
               * Triggered when a field changes position inside the same container (table). Does not apply on cards
               * that move from one table to another
               */
              orderChanged: function (event) {
                /*
                 * event.dest is the destination object. Handles object moving inside the same table,
                 * resetting their position attributes. 'value' is the field under processing
                 */
                angular.forEach(event.dest.sortableScope.$parent.column.cards, function (value, key) {
                  value.position = (key + 1);
                });

                self.storeTemplate();
              },
              containment: '#board'
            };

            /**
             * Allows modifying the definition of a field i.e. changing the field type
             * (text, yes/no field, dropdown), the field name and description
             * 
             * @param {type} column
             * @param {type} field
             * @returns {$q@call;defer.promise}
             */
            self.modifyField = function (column, field) {
              $scope.tableid = column.id;
              $scope.field = field;

              ModalService.modifyField($scope).then(
                      function (success) {
                        //Persist the modified card to the database
                        self.storeCard(self.currentTemplateID, column, success)
                                .then(function (response) {
                                  self.currentBoard = JSON.parse(response.board);
                                  growl.success("Field " + field.title + " modified successfully", {title: 'Success', ttl: 5000});
                                });
                      });
            };

            /**
             * Allows modifying the metadata (raw data) a file contains
             * 
             * @param {type} raw
             * @returns {undefined}
             */

            self.updateMetadata = function (metadata) {

              if (!self.metaDataDetail) {
                return;
              }

              var value = self.metaDataDetail[metadata.id];
              if (!value) {
                growl.info("Metadata field cannot be empty", {title: 'Info', ttl: 3000});
                return;
              }

              metadata.data = value;

              MetadataRestService.updateMetadata(self.currentFile.parentId, self.currentFile.name, metadata)
                      .then(function (success) {
                        self.fetchMetadataForTemplate();
                      }, function (error) {
                        growl.error("Metadata could not be updated", {title: 'Info', ttl: 1000});
                      });

            };

            self.removeMetadata = function (metadata) {

              if (!self.metaDataDetail) {
                return;
              }

              MetadataRestService.removeMetadata(self.currentFile.parentId, self.currentFile.name, metadata)
                      .then(function (success) {
                        self.fetchMetadataForTemplate();
                      }, function (error) {
                        growl.error("Metadata could not be removed", {title: 'Info', ttl: 1000});
                      });

            };


            /**
             * When the user clicks on a folder/file in the file browser the self.currentFile gets updated
             * 
             * @param {type} file
             * @returns {undefined}
             */
            self.setMetadataTemplate = function (file) {
              self.meta = [];
              self.metainfo = [];
              self.visibilityInfo = [];
              // var templateId = file.template;
              // self.currentTemplateID = templateId;
              self.currentFile = file;
              //update the current file reference
              MetadataHelperService.setCurrentFile(file);
              self.currentFile = MetadataHelperService.getCurrentFile();
              self.noTemplates = false;
                templateService.fetchTemplatesForInode(self.currentFile.id)
                      .then(function (response) {
                        self.currentFileTemplates = response.data;
                        self.attachedDetailedTemplateList = [];
                        var index = 0;
                        angular.forEach(self.currentFileTemplates, function (template, key) {
                            templateService.fetchTemplate(template.templateId, $cookies.get('email'))
                                  .then(function (response) {
                                    index++;
                                    self.attachedDetailedTemplateList.push({templateid: template.templateId, content: response.data.successMessage});
                                    if (self.currentFileTemplates.length === index) {
                                      self.fetchMetadataForTemplate();
                                    }
                                  });
                        });
                        if (self.currentFileTemplates.length === 0) {
                          self.noTemplates = true;
                        }

                      });

            };

            $scope.$on('setMetadata', function (event, args) {
              self.setMetadataTemplate(args.file);
            });

            /**
             * Updates the view according to the user template selection.
             * @returns {undefined}
             */
            self.updateMetadataTabs = function () {
                templateService.fetchTemplate(self.selectedTemplate.templateId, $cookies.get('email'))
                      .then(function (response) {
                        var board = response.data.successMessage;
                        self.currentBoard = JSON.parse(board);
                        self.initializeMetadataTabs(JSON.parse(board));
                      });
            };



            /**
             * Fetches all the metadata a template holds, for a selected inode
             * 
             * @returns {undefined}
             */
            self.fetchMetadataForTemplate = function () {
              //columns are the tables in the template
              self.meta = [];
              self.metainfo = [];
              self.visibilityInfo = [];
              angular.forEach(self.attachedDetailedTemplateList, function (template, key) {
                var templatecontent = JSON.parse(template.content);
                var tables = templatecontent.columns;
                angular.forEach(tables, function (table, key) {
                    templateService.fetchMetadata(self.currentFile.parentId, self.currentFile.name, table.id)
                          .then(function (response) {
                            var content = response.data[0];
                            self.reconstructMetadata(table.name, table.id, content.metadataView, table.cards);
                          });
                });
              });
            };

            /**
             * Creates the table with the retrieved metadata, so it can be displayed
             * in the file metadata presentation section
             * 
             * @param {type} tableName
             * @param {type} rawdata
             * @returns {undefined}
             */
            self.reconstructMetadata = function (tableName, tableId, rawdata, cards) {

              $scope.tableName = rawdata.table;
              self.meta.push({name: tableName, rest: rawdata});

              var cardDescription = {};
              angular.forEach(rawdata, function (data, key) {
                var key = tableId + '-' + data.tagName;
                self.visibilityInfo[key] = false;
                angular.forEach(cards, function (card, keycard) {
                  if (data.tagName === card.title) {
                    cardDescription[data.tagName] = card.description;
                  }
                });

              });

              self.metainfo.push({name: tableName, id: tableId, rest: rawdata, inputcontent: cards, desc: cardDescription});



              angular.forEach(rawdata.metadataView, function (item, key) {
                var key = item.id;
                self.visibilityInfo[key] = false;
              });

              self.metadataView = {};

              //console.log("RECONSTRUCTED METADATA " + JSON.stringify(self.meta));
            };

            self.setVisibilityAddMetadata = function (key, value) {
              self.visibilityInfo[key] = value;
            };

            /**
             * Creates the metadata tabs in the metadata insert page, according to the 
             * template that has been previously selected. Every table in the template
             * corresponds to a tab
             * 
             * @returns {undefined}
             */
            self.initializeMetadataTabs = function () {
              self.tabs = [];

              angular.forEach(self.currentBoard.columns, function (value, key) {
                console.log(key + ': ' + value.name);
                self.tabs.push({tableid: value.id, title: value.name, cards: value.cards});
              });

              self.currentTableId = angular.isUndefined(self.tabs[0]) ? -1 : self.tabs[0].tableid;
            };

            /**
             * Listener on tab selection changes
             * 
             * @param {type} tab
             * @returns {undefined}
             */
            self.onTabSelect = function (tab) {

              self.currentTableId = tab.tableid;
            };

            /**
             * Downloads a template on the fly
             * 
             * @param {type} template
             * @returns {undefined}
             */
            self.createDownloadURL = function (template) {
              var selectedTmptName = template.name;

              //console.log("SELECTED TEMPLATE " + JSON.stringify(template));

              //get the actual template
              MetadataActionService.fetchTemplate($cookies.get('email'), template.id)
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
                      });
            };

            /**
             * Uploads a .json template file to the file system and the database
             * 
             * @returns {undefined}
             */
            self.importTemplate = function () {
              ModalService.importTemplate('md')
                      .then(function (resp) {
                        /*
                         * doesn't really happening anything on success.
                         * it means that the upload was successful and the
                         * window closed automatically
                         */
                        growl.success("The template was uploaded successfully",
                                {title: 'Success', ttl: 2000});
                      },
                              function (closed) {
                                //trigger the necessary variable change in the service
                                MetadataHelperService.fetchAvailableTemplates()
                                        .then(function (response) {
                                          self.availableTemplates = JSON.parse(response.board).templates;
                                          if (self.availableTemplates !== null
                                                  && self.availableTemplates.length === 1) {
                                            self.availableTemplates[0].showing = false;
                                            self.selectedTemplate = self.availableTemplates[0];
                                            self.toggleTemplate(self.selectedTemplate);
                                            self.fetchTemplate(self.selectedTemplate.id)
                                          }
                                        });
                              });
            };
          }
        ]);
