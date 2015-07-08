/**
 * Created by AMore on 2015-04-24.
 */

'use strict';


angular.module('hopsWorksApp')
        .controller('DatasetsCtrl', ['$rootScope', '$modal', '$scope', '$q', '$timeout', '$mdSidenav', '$mdUtil', '$log', 'WSComm',
          'DataSetService', '$routeParams', 'ModalService', 'growl', 'ProjectService', 'MetadataActionService',
          function ($rootScope, $modal, $scope, $q, $timeout, $mdSidenav, $mdUtil, $log, WSComm, DataSetService, $routeParams,
                  ModalService, growl, ProjectService, MetadataActionService) {

            var self = this;

            self.datasets = [];
            self.tabs = [];
            self.currentDataSet = "";
            self.currentProject = "";
            self.currentFile = {};
            self.metaData = {};
            self.meta = [];
            self.metadataView = {};
            self.currentPath;
            self.pathParts;
            self.selected;
            self.fileDetail;

            self.dataSet = {};
            var file = {name: "", owner: 'Some One', modified: "", filesize: '4 GB', path: "", dir: ""};

            self.files = [file];
            var pId = $routeParams.projectID;
            var currentDS = $routeParams.datasetName;
            var dataSetService = DataSetService(pId);

            //this can be removed if we use project name instead of project id
            ProjectService.get({}, {'id': pId}).$promise.then(
                    function (success) {
                      console.log(success);
                      self.currentProject = success;
                    }, function (error) {
              $location.path('/');
            });

            var getAll = function () {
              dataSetService.getAll().then(
                      function (success) {
                        self.datasets = success.data;
                        console.log(success);
                      }, function (error) {
                console.log("getAll error");
                console.log(error);
              });
            };

            var getDir = function (name) {
              var newPath = "";
              if (self.currentPath && name) {
                newPath = self.currentPath + '/' + name;
              } else if (self.currentPath) {
                newPath = self.currentPath;
              } else if (name) {
                newPath = name;
              } else {
                getAll();
              }
              dataSetService.getDir(newPath).then(
                      function (success) {
                        self.files = success.data;
                        self.currentPath = newPath;
                        self.pathParts = newPath.split('/');
                        if (name) {
                          self.currentDataSet = name;
                        }
                        console.log(success);
                      }, function (error) {
                console.log("getDir error");
                console.log(error);
              });
            };

            self.setMetadataTemplate = function (file) {

              console.log("SELECTED FILE " + JSON.stringify(file));

              var templateId = file.template;
              self.currentTemplateID = templateId;
              self.currentFile = file;

              MetadataActionService.fetchTemplate(templateId)
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
              angular.forEach(tables, function (table, key) {
                //console.log("value " + JSON.stringify(table));
                MetadataActionService.fetchMetadata(table.id, self.currentFile.id)
                        .then(function (response) {
                          //console.log("METADATA FOR TABLE " + table.name);
                          //console.log("ARE " + JSON.stringify(response.board));
                          self.reconstructMetadata(table.name, JSON.parse(response.board));
                          //self.meta = JSON.parse(response.board);
                        });
              });
            };

            self.reconstructMetadata = function (tableName, rawdata) {

              $scope.tableName = rawdata.table;
//                    self.metadataView = results;
//                    self.meta.push({name: tableName, tuples: self.metadataView});

              self.meta.push({name: tableName, rest: rawdata});
              self.metadataView = {};
              console.log(JSON.stringify(self.meta));
              //self.print(self.meta);
              console.log("RECONSTRUCTED ARRAY  " + JSON.stringify(self.meta));
            };

            self.print = function (meta) {
              angular.forEach(meta, function (table) {
                var field = table.tuples.headers;
                console.log("IN TABLE " + table.name);

                angular.forEach(field, function (fieldName) {

                  console.log("IN FIELD " + fieldName);

                  var valueArray = table.tuples.values;

                  angular.forEach(valueArray, function (value) {
                    console.log("printing " + JSON.stringify(value[0].raw));
                  });
                });
              });
            };

            self.initializeMetadataTabs = function () {
              self.tabs = [];

              angular.forEach(self.currentBoard.columns, function (value, key) {
                console.log(key + ': ' + value.name);
                self.tabs.push({title: value.name, cards: value.cards});
              });
              //console.log("initialized tabs " + JSON.stringify(self.tabs));
            };

            /*
             * submit form data when the 'save' button is clicked
             */
            self.submitMetadata = function () {
              if (!self.metaData) {
                return;
              }

              self.metaData.inodeid = self.currentFile.id;
              console.log("saving " + JSON.stringify(self.metaData));

              MetadataActionService.storeMetadata(self.metaData)
                      .then(function (response) {
                        console.log("Metadata saved " + response.status);
                      });

              //truncate metaData object
              self.metaData = {};
            };

            var download = function (file) {
              dataSetService.download(file).then(
                      function (success) {
                        console.log("download success");
                        console.log(success);
                      }, function (error) {
                console.log("download error");
                console.log(error);
              });
            };

            var upload = function (path) {
              dataSetService.upload(path).then(
                      function (success) {
                        console.log("upload success");
                        console.log(success);
                      }, function (error) {
                console.log("upload error");
                console.log(error);
              });
            };

            var removeDataSetDir = function (path) {
              dataSetService.removeDataSetDir(path).then(
                      function (success) {
                        growl.success(success.data.successMessage, {title: 'Success', ttl: 15000});
                        getDir();
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Error', ttl: 15000});
              });
            };

            //if in dataset browser show current dataset content
            //else show datasets in project
            var load = function (path) {
              if (path) {
                getDir(path);
              } else {
                getAll();
              }
            };

            load(currentDS);

            self.newDataSetModal = function () {
              ModalService.newDataSet('md', self.currentPath).then(
                      function (success) {
                        growl.success(success.data.successMessage, {title: 'Success', ttl: 15000});
                        getDir();
                      }, function (error) {
                growl.info("Closed without saving.", {title: 'Info', ttl: 5000});
                getDir();
              });
            };

            self.attachTemplate = function (file) {
              var templateId = -1;
              console.log(JSON.stringify(file));

              ModalService.selectTemplate('sm', templateId).then(
                      function (success) {
                        templateId = success.templateId;
                        console.log("RETURNED TEMPLATE ID " + templateId);
                      });
            };

            self.deleteFile = function (fileName) {
              if (currentDS) {
                removeDataSetDir(self.currentPath + '/' + fileName);
              } else {
                removeDataSetDir(fileName);
              }
            };

            self.uploadFile = function () {
              var templateId = -1;

              ModalService.selectTemplate('sm', templateId).then(
                      function (success) {
                        templateId = success.templateId;
                        console.log("RETURNED TEMPLATE ID " + templateId);

                        ModalService.upload('lg', self.currentProject.projectId, self.currentPath, templateId).then(
                                function (success) {
                                  growl.success(success.data.successMessage, {title: 'Success', ttl: 15000});
                                  getDir();
                                }, function (error) {
                          growl.info("Closed without saving.", {title: 'Info', ttl: 5000});
                          getDir();
                        });
                      });
            };

            self.openDir = function (name, isDir) {
              if (isDir) {
                getDir(name);
              } else {
                ModalService.confirm('sm', 'Confirm', 'Do you want to download this file?').then(
                        function (success) {
                          download(self.currentPath + '/' + name);
                        }
                );
              }
            };

            self.back = function () {

              if (self.pathParts.length > 1) {
                self.pathParts.pop();
                self.currentPath = self.pathParts.join('/');
                self.currentDataSet = self.pathParts[self.pathParts.length - 1];
                if (self.currentPath) {
                  getDir();
                }
              } else {
                $location.path('/project/' + self.currentProject.projectId + '/datasets');
              }
            };

            self.goToFolder = function (index) {
              var parts = self.currentPath.split('/');
              if (index > -1) {
                var newPath = self.pathParts.splice(0, index + 1);
                self.currentPath = newPath.join('/');
                self.currentDataSet = parts[index];
                if (self.currentPath) {
                  getDir();
                }
              }
            };

            self.select = function (selectedIndex, file) {
              self.selected = selectedIndex;
              self.fileDetail = file;
            };

            /* Metadata designer */

            self.toggleLeft = buildToggler('left');
            self.toggleRight = buildToggler('right');

            function buildToggler(navID) {
              var debounceFn = $mdUtil.debounce(function () {
                $mdSidenav(navID)
                        .toggle()
                        .then(function () {
                          self.getAllTemplates();
                        });
              }, 300);
              return debounceFn;
            }
            ;

            self.close = function () {
              $mdSidenav('right').close()
                      .then(function () {
                        $log.debug("Closed metadata designer");
                      });
            };

            self.availableTemplates = [];
            self.newTemplateName = "";
            $scope.extendedFrom = {};

            self.extendedFromBoard = {};

            self.currentTemplateID = "";
            self.currentBoard = {};

            self.editedField = null;

            self.getAllTemplates = function () {
              MetadataActionService.fetchTemplates()
                      .then(function (data) {
                        self.availableTemplates = JSON.parse(data.board).templates;
                      });
            };

            self.addNewTemplate = function () {
              MetadataActionService.addNewTemplate(self.newTemplateName)
                      .then(function (data) {
                        self.newTemplateName = "";
                        self.getAllTemplates();
                        console.log(data);
                      });
            };

            self.removeTemplate = function (templateId) {
              MetadataActionService.removeTemplate(templateId)
                      .then(function (data) {
                        self.getAllTemplates();
                        console.log(data);
                      });
            };

            self.selectChanged = function (extendFromThisID) {
              console.log('selectChanged - start: ' + extendFromThisID);

              MetadataActionService.fetchTemplate(parseInt(extendFromThisID))
                      .then(function (success) {
                        console.log('Fetched data - success.board.column:');
                        self.extendedFromBoard = JSON.parse(success.board);
                        console.log(self.extendedFromBoard);

                        console.log('Fetched data - success:');
                        console.log(success);

                      }, function (error) {
                        console.log('Fetched data - error:');
                        console.log(error);
                      });
            };

            self.extendTemplate = function () {
              MetadataActionService.addNewTemplate(self.newTemplateName)
                      .then(function (data) {
                        var tempTemplates = JSON.parse(data.board);
                        var newlyCreatedID = tempTemplates.templates[tempTemplates.numberOfTemplates - 1].id;
                        console.log('add_new_templatE');
                        console.log(data);

                        console.log('Sent message: ');
                        console.log(self.extendedFromBoard);

                        MetadataActionService.extendTemplate(newlyCreatedID, self.extendedFromBoard)
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

              MetadataActionService.fetchTemplate(templateId)
                      .then(function (success) {
                        //update the currentBoard upon template retrieval
                        self.currentBoard = JSON.parse(success.board);
                        //console.log('fetchTemplate - success CURRENTBOARD ' + JSON.stringify(self.currentBoard));
                      }, function (error) {
                        console.log('fetchTemplate - error');
                        console.log(JSON.parse(error));
                      });
            };

            self.storeTemplate = function (closeSlideout) {

              MetadataActionService.storeTemplate(self.currentTemplateID, self.currentBoard)
                      .then(function (response) {
                        console.log("TEMPLATE SAVED SUCCESSFULLY " + JSON.stringify(response));
                        self.currentBoard = JSON.parse(response.board);
                        if (closeSlideout === 'true') {
                          self.close();
                        }
                      }, function (error) {
                        console.log(error);
                      });
            };

            self.deleteList = function (column) {
              MetadataActionService.deleteList(self.currentTemplateID, column)
                      .then(function (success) {
                        console.log(success);
                        self.fetchTemplate(self.currentTemplateID);
                      }, function (error) {
                        console.log(error);
                      });
            };

            self.storeCard = function (templateId, column, card) {

              return MetadataActionService.storeCard(templateId, column, card);
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

                        MetadataActionService.storeCard(self.currentTemplateID, column, card)
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


            self.back = function () {

              if (self.pathParts.length > 1) {
                self.pathParts.pop();
                self.currentPath = self.pathParts.join('/');
                self.currentDataSet = self.pathParts[self.pathParts.length - 1];
                if (self.currentPath) {
                  getDir();
                }
              } else {
                $location.path('/project/' + self.currentProject.projectId + '/datasets');
              }
            };

            self.goToFolder = function (index) {
              var parts = self.currentPath.split('/');
              if (index > -1) {
                var newPath = self.pathParts.splice(0, index + 1);
                self.currentPath = newPath.join('/');
                self.currentDataSet = parts[index];
                if (self.currentPath) {
                  getDir();
                }
              }
            };

            self.select = function (selectedIndex, file) {
              self.selected = selectedIndex;
              self.fileDetail = file;
            };

            /* Metadata designer */
            self.deleteCard = function (column, card) {
              MetadataActionService.deleteCard(self.currentTemplateID, column, card)
                      .then(function (success) {
                        console.log(success);
                        self.fetchTemplate(self.currentTemplateID);
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

                          //{tempid:2,bd:{name:MainBoard,numberOfColumns:3,columns:[{id:-1,name:newTable,cards:[]}],backlogs:[]}}}
                          //we need to add the new table into the mainboard object
                          self.currentBoard.columns.push(list);
                          //console.log("CURRENT LIST AFTER TABLE ADDITION " + JSON.stringify(self.currentBoard));

                          MetadataActionService.storeTemplate(self.currentTemplateID, self.currentBoard)
                                  .then(function (response) {
                                    console.log("TEMPLATE STORED SUCCESSFULLY " /*+ JSON.stringify(response)*/);
                                    self.currentBoard = JSON.parse(response.board);
                                    //defer.resolve($rootScope.mainBoard);
                                  }, function (error) {
                                    console.log(error);
                                  });
                        }
                      });
            };

            /* CARD MANIPULATION FUNCTIONS */
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
              console.log("SAVING " + JSON.stringify(column));
              $scope.tableid = column.id;
              $scope.field = field;

              var defer = $q.defer();

              //necessary data to modify the field definition
              //data: {table: column.id, field: field}};

              $modal.open({
                templateUrl: 'views/partials/modifyFieldDialog.html',
                controller: 'ModifyFieldCtrl',
                scope: $scope
              })
                      .result.then(function (dialogResponse) {
                        //PERSIST THE CARD TO THE DATABASE - dialogResponse is the modified field
                        self.storeCard(self.currentTemplateID, column, dialogResponse)
                                .then(function (response) {
                                  $rootScope.mainBoard = JSON.parse(response.board);

                                  defer.resolve($rootScope.mainBoard);
                                  $rootScope.$broadcast('refreshApp', $rootScope.mainBoard);
                                });
                      }, function (dialogResponse) {
                        console.log("don't modify " + JSON.stringify(dialogResponse));
                      });

              return defer.promise;
            };
          }]);