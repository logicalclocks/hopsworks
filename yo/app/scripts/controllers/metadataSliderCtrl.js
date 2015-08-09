/*jshint undef: false, unused: false, indent: 2*/
/*global angular: false */

'use strict';


angular.module('hopsWorksApp')
        .controller('MetaSliderCtrl', ['$cookies', '$modal', '$scope', '$mdSidenav', '$mdUtil',
          '$location', '$filter', 'DataSetService', 'ModalService', 'growl', 'MetadataActionService',
          'MetadataHelperService',
          function ($cookies, $modal, $scope, $mdSidenav, $mdUtil, $location, $filter, DataSetService,
                  ModalService, growl, MetadataActionService, MetadataHelperService) {

            var metaHelperService = MetadataHelperService();

            var self = this;
            self.metaData = {};
            self.currentFile = metaHelperService.getCurrentFile();



            /*
             * submit form data when the 'save' button is clicked
             */
            self.submitMetadata = function () {
              if (!self.metaData) {
                return;
              }

              var currentfile = metaHelperService.getCurrentFile();
              self.metaData.inodeid = currentfile.id;
              self.metaData.tableid = self.currentTableId;
              console.log("saving " + JSON.stringify(self.metaData));

              MetadataActionService.storeMetadata($cookies['email'], self.metaData)
                      .then(function (response) {
                        console.log("Metadata saved " + response.status);
                      });

              //truncate metaData object
              self.metaData = {};
            };

            /* -- TEMPLATE HANDLING FUNCTIONS -- */
            self.extendTemplate = function () {
              MetadataActionService.addNewTemplate(self.newTemplateName)
                      .then(function (data) {
                        var tempTemplates = JSON.parse(data.board);
                        var newlyCreatedID = tempTemplates.templates[tempTemplates.numberOfTemplates - 1].id;
                        console.log('add_new_templatE');
                        console.log(data);

                        console.log('Sent message: ');
                        console.log(self.extendedFromBoard);

                        MetadataActionService.extendTemplate($cookies['email'], newlyCreatedID, self.extendedFromBoard)
                                .then(function (data) {
                                  self.newTemplateName = "";
                                  self.getAllTemplates();

                                  console.log('Response from extending template: ');
                                  console.log(data);
                                });
                      });
            };

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

            self.storeTemplate = function (closeSlideout) {

              MetadataActionService.storeTemplate($cookies['email'], self.currentTemplateID, self.currentBoard)
                      .then(function (response) {
                        var template = JSON.parse(response.board);
                        var sortedTables = sortObject($filter, 'id', template);
                        template.columns = sortedTables;

                        self.currentBoard = template;
                        if (closeSlideout === 'true') {
                          self.close();
                        }
                      }, function (error) {
                        console.log(error);
                      });
            };

            self.getAllTemplates = function () {
              MetadataActionService.fetchTemplates($cookies['email'])
                      .then(function (data) {
                        self.availableTemplates = JSON.parse(data.board).templates;
                      });
            };

            self.addNewTemplate = function () {
              MetadataActionService.addNewTemplate($cookies['email'], self.newTemplateName)
                      .then(function (data) {
                        self.newTemplateName = "";
                        self.getAllTemplates();
                        console.log(data);
                      });
            };

            self.removeTemplate = function (templateId) {
              MetadataActionService.removeTemplate($cookies['email'], templateId)
                      .then(function (data) {
                        self.getAllTemplates();
                        console.log(data);
                      });
            };

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
                                  getDirContents();
                                }, function (error) {
                          growl.info("Could not attach template to file " + file.name + ".",
                                  {title: 'Info', ttl: 5000});
                        });
                      });
            };

            /* -- TABLE AND FIELD HANDLING FUNCTIONS ADD/REMOVE -- */
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

            self.storeCard = function (templateId, column, card) {

              return MetadataActionService.storeCard($cookies['email'], templateId, column, card);
            };

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
            self.makeSearchable = function (card) {
              card.find = !card.find;
              console.log("Card " + card.title + " became searchable " + card.find);
            };

            self.makeRequired = function (card) {
              card.required = !card.required;
              console.log("Card " + card.title + " became required " + card.required);
            };

            self.editSizeField = function (card) {

              card.sizefield.showing = !card.sizefield.showing;
              console.log("Card " + card.title + " showing " + card.sizefield.showing + " max size " + card.sizefield.value);

              self.editedField = card;
            };

            self.doneEditingSizeField = function (card) {

              card.sizefield.showing = false;
              self.editedField = null;
            };

            self.modifyField = function (column, field) {
              $scope.tableid = column.id;
              $scope.field = field;

              var defer = $q.defer();

              $modal.open({
                templateUrl: 'views/metadata/modifyFieldDialog.html',
                controller: 'ModifyFieldCtrl',
                scope: $scope
              })
                      .result.then(function (dialogResponse) {
                        //PERSIST THE CARD TO THE DATABASE - dialogResponse is the modified field
                        self.storeCard(self.currentTemplateID, column, dialogResponse)
                                .then(function (response) {
                                  self.currentBoard = JSON.parse(response.board);

                                  defer.resolve($rootScope.mainBoard);
                                  console.log("MODIFIED FIELD " + JSON.stringify(self.currentBoard));
                                });
                      });

              return defer.promise;
            };

            self.updateRawdata = function (raw) {
              console.log("META " + JSON.stringify(raw));
              MetadataActionService.updateRawdata($cookies['email'], raw)
                      .then(function (response) {
                        growl.success(response.board, {title: 'Success', ttl: 5000});
                      }, function (dialogResponse) {
                        growl.info("Could not update metadata " + raw.raw + ".",
                                {title: 'Info', ttl: 5000});
                      });
            };

            self.setMetadataTemplate = function (file) {

              console.log("SELECTED FILE " + JSON.stringify(file));

              var templateId = file.template;
              self.currentTemplateID = templateId;
              metaHelperService.setCurrentFile(file);

              MetadataActionService.fetchTemplate($cookies['email'], templateId)
                      .then(function (response) {
                        console.log("LOADED TEMPLATE " + JSON.stringify(response.board) + " template id " + templateId);
                        self.currentBoard = JSON.parse(response.board);
                        self.initializeMetadataTabs(JSON.parse(response.board));

                        self.fetchMetadataForTemplate();
                      });
            };

            self.fetchMetadataForTemplate = function () {
              //columns are the tables in the template
              self.meta = [];

              var tables = self.currentBoard.columns;
              var currentfile = metaHelperService.getCurrentFile();

              angular.forEach(tables, function (table, key) {
                //console.log("value " + JSON.stringify(table));
                MetadataActionService.fetchMetadata($cookies['email'], table.id, currentfile.id)
                        .then(function (response) {
                          self.reconstructMetadata(table.name, JSON.parse(response.board));
                          //self.meta = JSON.parse(response.board);
                        });
              });
            };

            self.reconstructMetadata = function (tableName, rawdata) {

              $scope.tableName = rawdata.table;

              self.meta.push({name: tableName, rest: rawdata});
              self.metadataView = {};
              console.log("RECONSTRUCTED ARRAY  " + JSON.stringify(self.meta));
            };

            self.initializeMetadataTabs = function () {
              self.tabs = [];

              angular.forEach(self.currentBoard.columns, function (value, key) {
                console.log(key + ': ' + value.name);
                self.tabs.push({tableid: value.id, title: value.name, cards: value.cards});
              });

              self.currentTableId = angular.isUndefined(self.tabs[0]) ? -1 : self.tabs[0].tableid;
            };
          }
        ]);

