/*jshint undef: false, unused: false, indent: 2*/
/*global angular: false */

'use strict';


angular.module('hopsWorksApp')
        .controller('MetaSliderCtrl', ['$cookies', '$modal', '$scope', '$routeParams',
          '$filter', 'DataSetService', 'ModalService', 'growl', 'MetadataActionService',
          'MetadataHelperService',
          function ($cookies, $modal, $scope, $routeParams, $filter, DataSetService,
                  ModalService, growl, MetadataActionService, MetadataHelperService) {

            var self = this;
            self.metaData = {};
            self.currentFile = MetadataHelperService.getCurrentFile();
            self.tabs = [];
            self.meta = [];
            self.availableTemplates = [];
            self.newTemplateName = "";
            self.currentTableId = -1;
            self.currentTemplateID = -1;
            self.editedField;
            self.extendedFrom = {};
            self.currentBoard = {};
            var dataSetService = DataSetService($routeParams.projectID);

            /**
             * submit form data when the 'save' button is clicked
             */
            self.submitMetadata = function () {
              if (!self.metaData) {
                return;
              }

              var currentfile = MetadataHelperService.getCurrentFile();
              self.metaData.inodeid = currentfile.id;
              self.metaData.tableid = self.currentTableId;
              console.log("saving " + JSON.stringify(self.metaData));

              MetadataActionService.storeMetadata($cookies['email'], self.metaData)
                      .then(function (response) {
                        console.log("Metadata saved " + response.status);
                        //rename the corresponding folder
                        MetadataActionService.renameDir($cookies['email'], self.currentFile.path)
                                .then(function(resp){
                                  console.log("FILE RENAMED " + JSON.stringify(resp));
                        });
                      });

              //truncate metaData object
              self.metaData = {};
            };

            /* -- TEMPLATE HANDLING FUNCTIONS -- */
            /**
             * Creates a new template from an existing one
             * 
             * @returns {undefined}
             */
            self.extendTemplate = function () {
              //store the new template name
              MetadataActionService.addNewTemplate($cookies['email'], self.newTemplateName)
                      .then(function (data) {
                        var tempTemplates = JSON.parse(data.board);

                        //get the id of the new template
                        var newlyCreatedID = tempTemplates.templates[tempTemplates.numberOfTemplates - 1].id;

                        //get the contents of the template to extend
                        MetadataActionService.fetchTemplate($cookies['email'], parseInt(self.extendedFrom))
                                .then(function (response) {
                                  var templateToExtend = JSON.parse(response.board);
                                  //console.log(JSON.stringify(cleanBoard));
                                  //associate existing contents with the new template
                                  MetadataActionService.extendTemplate($cookies['email'], newlyCreatedID, templateToExtend)
                                          .then(function (data) {
                                            self.newTemplateName = "";

                                            //trigger the necessary variable change in the service
                                            MetadataHelperService.fetchAvailableTemplates()
                                                    .then(function (response) {
                                                      self.availableTemplates = JSON.parse(response.board).templates;
                                                      //console.log("AVAILABLE TEMPLATES " + JSON.stringify(self.availableTemplates));
                                                    });

                                            console.log('Response from extending template: ');
                                            console.log(data);
                                          });
                                });
                      });
            };

            /**
             * Fetches a specific template from the database based on its id
             * 
             * @param {type} templateId
             * @returns {undefined}
             */
            self.fetchTemplate = function (templateId) {
              self.currentTemplateID = templateId;

              MetadataActionService.fetchTemplate($cookies['email'], templateId)
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
             * @param {type} closeSlideout
             * @returns {undefined}
             */
            self.storeTemplate = function (closeSlideout) {

              MetadataActionService.storeTemplate($cookies['email'], self.currentTemplateID, self.currentBoard)
                      .then(function (response) {
                        var template = JSON.parse(response.board);
                        var sortedTables = sortObject($filter, 'id', template);
                        template.columns = sortedTables;

                        self.currentBoard = template;
                        if (closeSlideout === 'true') {
                          MetadataHelperService.setCloseSlider("true");
                        }
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
              MetadataActionService.addNewTemplate($cookies['email'], self.newTemplateName)
                      .then(function (data) {
                        self.newTemplateName = "";
                        //trigger a variable change (availableTemplates) in the service
                        MetadataHelperService.fetchAvailableTemplates();
                      });
            };

            /**
             * Deletes a template from the database
             * 
             * @param {type} templateId. The id of the template to be removed
             * @returns {undefined}
             */
            self.removeTemplate = function (templateId) {
              MetadataActionService.removeTemplate($cookies['email'], templateId)
                      .then(function (data) {
                        //trigger a variable change (availableTemplates) in the service
                        MetadataHelperService.fetchAvailableTemplates();
                        console.log(JSON.stringify(data));
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
              console.log(JSON.stringify(file));

              var data = {inodePath: "", templateId: -1};
              data.inodePath = file.path;

              ModalService.selectTemplate('sm', false, templateId).then(
                      function (success) {
                        data.templateId = success.templateId;
                        console.log("RETURNED TEMPLATE ID " + data.templateId);

                        dataSetService.attachTemplate(data).then(
                                function (success) {
                                  growl.success(success.data.successMessage, {title: 'Success', ttl: 15000});
                                  //refresh the file browser to get the updated objects
                                  MetadataHelperService.setDirContents("true");
                                }, function (error) {
                          growl.info("Could not attach template to file " + file.name + ".",
                                  {title: 'Info', ttl: 5000});
                        });
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
              MetadataActionService.isTableEmpty($cookies['email'], column.id)
                      .then(function (response) {

                        if (response.board !== "EMPTY") {
                          ModalService.confirm("sm", "Delete table",
                                  "This table contains fields. Do you really want to delete it?\n\
                                This action cannot be undone.")
                                  .then(function (success) {
                                    self.deleteTable(column);
                                  }, function (cancelled) {
                                    console.log("CANCELED " + JSON.stringify(cancelled));
                                    growl.info("Delete aborted", {title: 'Info', ttl: 5000});
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
              MetadataActionService.deleteList($cookies['email'], self.currentTemplateID, column)
                      .then(function (success) {
                        console.log(success);
                        self.fetchTemplate(self.currentTemplateID);
                        growl.success("Table " + column.name + " deleted successfully.",
                                {title: 'Success', ttl: 5000});
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
              MetadataActionService.isFieldEmpty($cookies['email'], card.id)
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
              MetadataActionService.deleteCard($cookies['email'], self.currentTemplateID, column, card)
                      .then(function (success) {
                        console.log(success);
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

              return MetadataActionService.storeCard($cookies['email'], templateId, column, card);
            };

            /**
             * Displays the modal dialog to creating a new card
             * 
             * @param {type} column
             * @returns {undefined}
             */
            self.addCard = function (column) {
              $scope.currentColumn = column;
              var modalInstance = $modal.open({
                templateUrl: 'views/metadata/newCardModal.html',
                controller: 'NewCardCtrl',
                scope: $scope
              })
                      .result.then(function (card) {

                        console.log('Created card, ready to send:');
                        console.log(JSON.stringify(card));

                        MetadataActionService.storeCard($cookies['email'], self.currentTemplateID, column, card)
                                .then(function (success) {
                                  console.log(success);
                                  self.fetchTemplate(self.currentTemplateID);
                                }, function (error) {
                                  console.log(error);
                                });

                      }, function (error) {
                        console.log(error);
                      });
            };

            /**
             * Displays the modal dialog to creating a new table
             * 
             * @returns {undefined}
             */
            self.addNewList = function () {
              $scope.template = self.currentTemplateID;
              $modal.open({
                templateUrl: 'views/metadata/newListModal.html',
                controller: 'NewlistCtrl',
                scope: $scope
              })
                      .result.then(function (list) {

                        if (!angular.isUndefined(list)) {

                          //we need to add the new table into the mainboard object
                          self.currentBoard.columns.push(list);

                          MetadataActionService.storeTemplate($cookies['email'], self.currentTemplateID, self.currentBoard)
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
              console.log("Card " + card.title + " became searchable " + card.find);
            };

            /**
             * Makes a field (not)required by setting the attribute 'required' accordingly
             * 
             * @param {type} card
             * @returns {undefined}
             */
            self.makeRequired = function (card) {
              card.required = !card.required;
              console.log("Card " + card.title + " became required " + card.required);
            };

            /**
             * Allows editing the size (max chars) of a field
             * 
             * @param {type} card
             * @returns {undefined}
             */
            self.editSizeField = function (card) {

              card.sizefield.showing = !card.sizefield.showing;
              console.log("Card " + card.title + " showing " + card.sizefield.showing + " max size " + card.sizefield.value);

              self.editedField = card;
            };

            self.doneEditingSizeField = function (card) {

              card.sizefield.showing = false;
              self.editedField = null;
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
                                });

                      });
            };

            /**
             * Allows modifying the metadata (raw data) a file contains
             * 
             * @param {type} raw
             * @returns {undefined}
             */
            self.updateRawdata = function (raw) {
              console.log("META " + JSON.stringify(raw));
              MetadataActionService.updateRawdata($cookies['email'], raw)
                      .then(function (response) {
                        growl.success(response.board, {title: 'Success', ttl: 5000});
                        MetadataActionService.renameDir($cookies['email'], self.currentFile.path)
                                .then(function(resp){
                                  console.log("REEEENAMED FOLDER " + JSON.stringify(resp));
                        });
                      }, function (dialogResponse) {
                        growl.info("Could not update metadata " + raw.raw + ".",
                                {title: 'Info', ttl: 5000});
                      });
            };

            self.setMetadataTemplate = function (file) {

              console.log("SELECTED FILE " + JSON.stringify(file));

              var templateId = file.template;
              self.currentTemplateID = templateId;
              self.currentFile = file;
              MetadataHelperService.setCurrentFile(file);

              MetadataActionService.fetchTemplate($cookies['email'], templateId)
                      .then(function (response) {
                        console.log("LOADED TEMPLATE " + JSON.stringify(response.board) + " template id " + templateId);
                        self.currentBoard = JSON.parse(response.board);
                        self.initializeMetadataTabs(JSON.parse(response.board));
                        self.fetchMetadataForTemplate();
                      });
            };

            /**
             * Fetches all the metadata a template holds
             * 
             * @returns {undefined}
             */
            self.fetchMetadataForTemplate = function () {
              //columns are the tables in the template
              self.meta = [];

              var tables = self.currentBoard.columns;
              var currentfile = MetadataHelperService.getCurrentFile();

              angular.forEach(tables, function (table, key) {
                MetadataActionService.fetchMetadata($cookies['email'], table.id, currentfile.id)
                        .then(function (response) {
                          self.reconstructMetadata(table.name, JSON.parse(response.board));
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
            self.reconstructMetadata = function (tableName, rawdata) {

              $scope.tableName = rawdata.table;

              self.meta.push({name: tableName, rest: rawdata});
              self.metadataView = {};
              console.log("RECONSTRUCTED ARRAY  " + JSON.stringify(self.meta));
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
          }
        ]);

