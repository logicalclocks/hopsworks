/**
 * Created by AMore on 2015-04-24.
 */

'use strict';


angular.module('hopsWorksApp')
        .controller('DatasetsCtrl', ['$rootScope', '$modal', '$scope', '$q', '$mdSidenav', '$mdUtil', '$log',
          'DataSetService', '$routeParams', 'ModalService', 'growl', 'MetadataActionService', '$location', '$filter',
          function ($rootScope, $modal, $scope, $q, $mdSidenav, $mdUtil, $log, DataSetService, $routeParams,
                  ModalService, growl, MetadataActionService, $location, $filter) {

            var self = this;

            //Some variables to keep track of state.
            self.files = []; //A list of files currently displayed to the user.
            self.projectId = $routeParams.projectID; //The id of the project we're currently working in.
            self.pathArray; //An array containing all the path components of the current path. If empty: project root directory.
            self.selected; //The index of the selected file in the files array.
            self.fileDetail; //The details about the currently selected file.

            var dataSetService = DataSetService(self.projectId); //The datasetservice for the current project.

            self.tabs = [];
            self.currentFile = {};
            self.metaData = {};
            self.meta = [];
            self.metadataView = {};
            self.currentTableId = -1;

            /*
             * Get all datasets under the current project.
             * @returns {undefined}
             */
            self.getAllDatasets = function () {
              //Get the path for an empty patharray: will get the datasets
              var path = getPath([]);
              dataSetService.getContents(path).then(
                      function (success) {
                        self.files = success.data;
                        self.pathArray = [];
                        console.log(success);
                      }, function (error) {
                console.log("Error getting all datasets in project " + self.projectId);
                console.log(error);
              });
            };

            /**
             * Get the contents of the directory at the path with the given path components and load it into the frontend.
             * @param {type} The array of path compontents to fetch. If empty, fetches the current path.
             * @returns {undefined}
             */
            var getDirContents = function (pathComponents) {
              //Construct the new path array
              var newPathArray;
              if (pathComponents) {
                newPathArray = pathComponents;
              } else {
                newPathArray = self.pathArray;
              }
              //Convert into a path
              var newPath = getPath(newPathArray);
              //Get the contents and load them
              dataSetService.getContents(newPath).then(
                      function (success) {
                        //Reset the selected file
                        self.selected = null;
                        self.fileDetail = null;
                        //Set the current files and path
                        self.files = success.data;
                        self.pathArray = newPathArray;
                        console.log(success);
                      }, function (error) {
                console.log("Error getting the contents of the path " + getPath(newPathArray));
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
                self.tabs.push({tableid: value.id, title: value.name, cards: value.cards});
              });

              self.currentTableId = angular.isUndefined(self.tabs[0]) ? -1 : self.tabs[0].tableid;
            };

            /*
             * submit form data when the 'save' button is clicked
             */
            self.submitMetadata = function () {
              if (!self.metaData) {
                return;
              }

              self.metaData.inodeid = self.currentFile.id;
              self.metaData.tableid = self.currentTableId;
              console.log("saving " + JSON.stringify(self.metaData));

              MetadataActionService.storeMetadata(self.metaData)
                      .then(function (response) {
                        console.log("Metadata saved " + response.status);
                      });

              //truncate metaData object
              self.metaData = {};
            };

            var init = function () {
              //Check if the current dataset is set
              if ($routeParams.datasetName) {
                //Dataset is set: get the contents
                self.pathArray = [$routeParams.datasetName];
              } else {
                //No current dataset is set: get all datasets.
                self.pathArray = [];
              }
              getDirContents();
            };

            init();

            /**
             * Download a file.
             * @param {type} file Can either be a filename or a path.
             * @returns {undefined}
             */
            var download = function (file) {
              dataSetService.download(file).then(
                      function (success) {
                        console.log("download success");
                        console.log(success);
                      }, function (error) {
                console.log("Error downloading file " + file);
                console.log(error);
              });
            };

            /**
             * Upload a file to the specified path.
             * @param {type} path
             * @returns {undefined}
             */
            var upload = function (path) {
              dataSetService.upload(path).then(
                      function (success) {
                        console.log("upload success");
                        console.log(success);
                        getDirContents();
                      }, function (error) {
                console.log("upload error");
                console.log(error);
              });
            };

            /**
             * Remove the inode at the given path. If called on a folder, will 
             * remove the folder and all its contents recursively.
             * @param {type} path. The project-relative path to the inode to be removed.
             * @returns {undefined}
             */
            var removeInode = function (path) {
              dataSetService.removeDataSetDir(path).then(
                      function (success) {
                        growl.success(success.data.successMessage, {title: 'Success', ttl: 15000});
                        getDirContents();
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Error', ttl: 15000});
              });
            };

            /**
             * Open a modal dialog for folder creation. The folder is created at the current path.
             * @returns {undefined}
             */
            self.newDataSetModal = function () {
              ModalService.newFolder('md', getPath(self.pathArray)).then(
                      function (success) {
                        growl.success(success.data.successMessage, {title: 'Success', ttl: 15000});
                        getDirContents();
                      }, function (error) {
                //The user changed his/her mind. Don't really need to do anything.
                getDirContents();
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

            /**
             * Delete the file with the given name under the current path. If called on a folder, will remove the folder 
             * and all its contents recursively.
             * @param {type} fileName
             * @returns {undefined}
             */
            self.deleteFile = function (fileName) {
              var removePathArray = self.pathArray.slice(0);
              removePathArray.push(fileName);
              removeInode(getPath(removePathArray));
            };

            /**
             * Opens a modal dialog for file upload.
             * @returns {undefined}
             */
            self.uploadFile = function () {
              var templateId = -1;

              ModalService.selectTemplate('sm', true, templateId).then(
                      function (success) {
                        templateId = success.templateId;
                        console.log("RETURNED TEMPLATE ID " + templateId);

                        ModalService.upload('lg', self.projectId, getPath(self.pathArray), templateId).then(
                                function (success) {
                                  growl.success(success.data.successMessage, {title: 'Success', ttl: 15000});
                                  getDirContents();
                                }, function (error) {
                          growl.info("Closed without saving.", {title: 'Info', ttl: 5000});
                          getDirContents();
                        });
                      });
            };
            /**
             * Opens a modal dialog for sharing.
             * @returns {undefined}
             */
            self.share = function (name) {
              ModalService.shareDataset('md', name).then(
                      function (success) {
                        growl.success(success.data.successMessage, {title: 'Success', ttl: 15000});
                      }, function (error) {
              });
            };
            /**
             * Upon click on a inode in the browser:
             *  + If folder: open folder, fetch contents from server and display.
             *  + If file: open a confirm dialog prompting for download.
             * @param {type} name
             * @param {type} isDir
             * @returns {undefined}
             */
            self.openDir = function (name, isDir) {
              if (isDir) {
                var newPathArray = self.pathArray.slice(0);
                newPathArray.push(name);
                getDirContents(newPathArray);
              } else {
                ModalService.confirm('sm', 'Confirm', 'Do you want to download this file?').then(
                        function (success) {
                          var downloadPathArray = self.pathArray.slice(0);
                          downloadPathArray.push(name);
                          download(getPath(downloadPathArray));
                        }
                );
              }
            };

            /**
             * Go up to parent directory.
             * @returns {undefined}
             */
            self.back = function () {
              var newPathArray = self.pathArray.slice(0);
              newPathArray.pop();
              if (newPathArray.length == 0) {
                $location.path('/project/' + self.projectId + '/datasets');
              } else {
                getDirContents(newPathArray);
              }
            };

            /**
             * Go to the folder at the index in the pathArray array.
             * @param {type} index
             * @returns {undefined}
             */
            self.goToFolder = function (index) {
              var newPathArray = self.pathArray.slice(0);
              newPathArray.splice(index + 1, newPathArray.length - index - 1);
              getDirContents(newPathArray);
            };

            /**
             * Select an inode; updates details panel.
             * @param {type} selectedIndex
             * @param {type} file
             * @returns {undefined}
             */
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

              MetadataActionService.storeTemplate(self.currentTemplateID, self.currentBoard)
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

                          //we need to add the new table into the mainboard object
                          self.currentBoard.columns.push(list);

                          MetadataActionService.storeTemplate(self.currentTemplateID, self.currentBoard)
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

            self.onTabSelect = function (tab) {

              self.currentTableId = tab.tableid;
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
              $scope.tableid = column.id;
              $scope.field = field;

              var defer = $q.defer();

              //necessary data to modify the field definition
              //data: {table: column.id, field: field}};

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
                      }, function (dialogResponse) {
                        console.log("don't modify " + JSON.stringify(dialogResponse));
                      });

              return defer.promise;
            };
          }]);

/**
 * Turn the array <i>pathArray</i> containing, path components, into a path string.
 * @param {type} pathArray
 * @returns {String}
 */
var getPath = function (pathArray) {
  return pathArray.join("/");
};
