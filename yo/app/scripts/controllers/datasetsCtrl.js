/**
 * Created by AMore on 2015-04-24.
 */

'use strict';


angular.module('hopsWorksApp')
        .controller('DatasetsCtrl', ['$rootScope', '$modal', '$scope', '$timeout', '$mdSidenav', '$mdUtil', '$log', '$websocket', 'WSComm',
          'DataSetService', '$routeParams', 'ModalService', 'growl', 'ProjectService', '$location',
          function ($rootScope, $modal, $scope, $timeout, $mdSidenav, $mdUtil, $log, $websocket, WSComm, DataSetService, $routeParams, ModalService, growl, ProjectService, $location) {

            var self = this;

            self.datasets = [];
            self.currentDataSet = "";
            self.currentProject = "";
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
            }
            );

            /*
             * Get all datasets under the current project.
             * @returns {undefined}
             */
            self.getAll = function () {
              dataSetService.getAll().then(
                      function (success) {
                        self.datasets = success.data;
                        //To allow displaying datasets as folders.
                        self.files = success.data;
                        console.log(success);
                      }, function (error) {
                console.log("getAll error");
                console.log(error);
              });
            };
            /**
             * Get all directories under the DS with given name.
             * @param {type} datasetName
             * @returns {undefined}
             */
            var getDir = function (datasetName) {
              var newPath = "";
              if (self.currentPath && datasetName) {
                newPath = self.currentPath + '/' + datasetName;
              } else if (self.currentPath) {
                newPath = self.currentPath;
              } else if (datasetName) {
                newPath = datasetName;
              } else {
                self.getAll();
              }
              //Reset the selected file
              self.selected = null;
              self.fileDetail = null;
              dataSetService.getDir(newPath).then(
                      function (success) {
                        self.files = success.data;
                        self.currentPath = newPath;
                        self.pathParts = newPath.split('/');
                        if (datasetName) {
                          self.currentDataSet = datasetName;
                        }
                        console.log(success);
                      }, function (error) {
                console.log("getDir error");
                console.log(error);
              });
            };

            /**
             * Download a file.
             * @param {type} file Can either be a filename or a path.
             * @returns {undefined}
             */
            var download = function (file) {
              dataSetService.download(file).then(
                      function (data) {
                        var file = new Blob([data], {type: 'application/txt'});
                        //saveAs(file, 'filename');
                      }, function (error) {
                console.log("download error");
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
                        getDir();
                      }, function (error) {
                console.log("upload error");
                console.log(error);
              });
            };

            /**
             * Remove the inode at the given path. If called on a folder, will 
             * remove the folder and all its contents recursively.
             * @param {type} path
             * @returns {undefined}
             */
            var removeInode = function (path) {
              dataSetService.removeDataSetDir(path).then(
                      function (success) {
                        growl.success(success.data.successMessage, {title: 'Success', ttl: 15000});
                        getDir();
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Error', ttl: 15000});
              });
            };

            /*
             * Load the datasets/folder contents to be displayed.
             * if in dataset browser show current dataset content
             * else show datasets in project
             */
            var load = function (path) {
              if (path) {
                getDir(path);
              } else {
                self.getAll();
              }
            };
            load(currentDS);

            /**
             * Open a modal dialog for DS creation.
             * @returns {undefined}
             */
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

            /**
             * Delete the file with the given name. If currently in a Dataset, 
             * will prepend the current path, otherwise will remove the dataset 
             * with given name. If called on a folder, will remove the folder 
             * and all its contents recursively.
             * @param {type} fileName
             * @returns {undefined}
             */
            self.deleteFile = function (fileName) {
              if (currentDS) {
                removeInode(self.currentPath + '/' + fileName);
              } else {
                removeInode(fileName);
              }
            };

            /**
             * Opens a modal dialog for file upload.
             * @returns {undefined}
             */
            self.uploadFile = function () {
              ModalService.upload('lg', self.currentProject.projectId, self.currentPath).then(
                      function (success) {
                        growl.success(success.data.successMessage, {title: 'Success', ttl: 15000});
                        getDir();
                      }, function (error) {
                getDir();
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
                getDir(name);
              } else {
                ModalService.confirm('sm', 'Confirm', 'Do you want to download this file?').then(
                        function (success) {
                          download(self.currentPath + '/' + name);
                        }
                );
              }
            };

            /**
             * Go up to parent directory.
             * @returns {undefined}
             */
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

            /**
             * Go to the folder at the index in the pathparts array.
             * @param {type} index
             * @returns {undefined}
             */
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

            self.getAllTemplates = function () {
              WSComm.send({
                sender: 'evsav',
                type: 'TemplateMessage',
                action: 'fetch_templates',
                message: JSON.stringify({})
              }).then(
                      function (data) {
                        self.availableTemplates = JSON.parse(data.board).templates;
                      }
              );
            }

            self.addNewTemplate = function () {
              return WSComm.send({
                sender: 'evsav',
                type: 'TemplateMessage',
                action: 'add_new_template',
                message: JSON.stringify({templateName: self.newTemplateName})
              }).then(
                      function (data) {
                        self.newTemplateName = "";
                        self.getAllTemplates();
                        console.log(data);
                      }
              );
            }

            self.removeTemplate = function (templateId) {
              return WSComm.send({
                sender: 'evsav',
                type: 'TemplateMessage',
                action: 'remove_template',
                message: JSON.stringify({templateId: templateId})
              }).then(
                      function (data) {
                        self.getAllTemplates();
                        console.log(data);
                      }
              );
            }

            $scope.$watch('extendedFrom', function (newID) {
              if (typeof newID == "string") {
                self.selectChanged(newID);
              }
            });

            self.selectChanged = function (extendFromThisID) {
              console.log('selectChanged - start: ' + extendFromThisID);
              return WSComm.send({
                sender: 'evsav',
                type: 'TemplateMessage',
                action: 'fetch_template',
                message: JSON.stringify({tempid: parseInt(extendFromThisID)})
              }).then(
                      function (success) {
                        console.log('Fetched data - success.board.column:');
                        self.extendedFromBoard = JSON.parse(success.board);
                        console.log(self.extendedFromBoard);

                        console.log('Fetched data - success:');
                        console.log(success);

                      }, function (error) {
                console.log('Fetched data - error:');
                console.log(error);
              }
              )
            }

            self.extendTemplate = function () {
              return WSComm.send({
                sender: 'evsav',
                type: 'TemplateMessage',
                action: 'add_new_template',
                message: JSON.stringify({templateName: self.newTemplateName})
              }).then(
                      function (data) {
                        var tempTemplates = JSON.parse(data.board);
                        var newlyCreatedID = tempTemplates.templates[tempTemplates.numberOfTemplates - 1].id;
                        console.log('add_new_templatE');
                        console.log(data);

                        console.log('Sent message: ');
                        console.log(self.extendedFromBoard);

                        return WSComm.send({
                          sender: 'evsav',
                          type: 'TemplateMessage',
                          action: 'extend_template',
                          message: JSON.stringify({tempid: newlyCreatedID, bd: self.extendedFromBoard})
                        }).then(
                                function (data) {
                                  self.newTemplateName = "";
                                  self.getAllTemplates();

                                  console.log('Response from extending template: ');
                                  console.log(data);
                                }
                        );
                      }
              );
            }

            self.fetchTemplate = function (templateId) {
              self.currentTemplateID = templateId;
              return WSComm.send({
                sender: 'evsav',
                type: 'TemplateMessage',
                action: 'fetch_template',
                message: JSON.stringify({tempid: templateId})
              }).then(
                      function (success) {
                        console.log('fetchTemplate - success');
                        self.currentBoard = JSON.parse(success.board);
                        console.log(self.currentBoard);
                      }, function (error) {
                console.log('fetchTemplate - error');
                console.log(JSON.parse(error));
              }
              );
            };

            self.storeTemplate = function () {
              return WSComm.send({
                sender: 'evsav',
                type: 'TemplateMessage',
                action: 'store_template',
                message: JSON.stringify({tempid: self.currentTemplateID, bd: self.currentBoard})
              }).then(
                      function (success) {
                        console.log(success);
                      }, function (error) {
                console.log(error);
              }
              );
            }
            self.deleteList = function (column) {
              return WSComm.send({
                sender: 'evsav',
                type: 'TablesMessage',
                action: 'delete_table',
                message: JSON.stringify({
                  tempid: self.currentTemplateID,
                  id: column.id,
                  name: column.name,
                  forceDelete: column.forceDelete
                })
              }).then(
                      function (success) {
                        console.log(success);
                        self.fetchTemplate(self.currentTemplateID)
                      }, function (error) {
                console.log(error);
              }
              );
            }

            self.storeCard = function (column, card) {
              return WSComm.send({
                sender: 'evsav',
                type: 'FieldsMessage',
                action: 'store_field',
                message: JSON.stringify({
                  tempid: self.currentTemplateID,
                  tableid: column.id,
                  tablename: column.name,
                  id: card.id,
                  name: card.title,
                  type: 'VARCHAR(50)',
                  searchable: card.find,
                  required: card.required,
                  sizefield: card.sizefield,
                  description: card.description,
                  fieldtypeid: card.fieldtypeid,
                  fieldtypeContent: card.fieldtypeContent
                })
              })
            }

            self.addCard = function (column) {
              $scope.currentColumn = column;

              $modal.open({
                templateUrl: 'views/metadata/newCardModal.html',
                controller: 'NewCardCtrl',
                scope: $scope
              }).result.then(
                      function (card) {

                        console.log('Created card, ready to send:');
                        console.log(card);

                        self.storeCard(column, card).then(
                                function (success) {
                                  console.log(success);
                                  self.fetchTemplate(self.currentTemplateID)
                                }, function (error) {
                          console.log(error);
                        }
                        );

                      }, function (error) {
                console.log(error);
              }
              );
            };

            self.deleteCard = function (column, card) {
              return WSComm.send({
                sender: 'evsav',
                type: 'FieldsMessage',
                action: 'delete_field',
                message: JSON.stringify({
                  tempid: self.currentTemplateID,
                  id: card.id,
                  tableid: column.id,
                  tablename: column.name,
                  name: card.title,
                  type: 'VARCHAR(50)',
                  sizefield: card.sizefield,
                  searchable: card.find,
                  required: card.required,
                  forceDelete: card.forceDelete,
                  description: card.description,
                  fieldtypeid: card.fieldtypeid,
                  fieldtypeContent: card.fieldtypeContent
                })
              }).then(
                      function (success) {
                        console.log(success);
                        self.fetchTemplate(self.currentTemplateID)
                      }, function (error) {
                console.log(error);
              });
            }

            self.addNewList = function () {
              $scope.template = self.currentTemplateID;
              $modal.open({
                templateUrl: 'views/metadata/newListModal.html',
                controller: 'NewListCtrl',
                scope: $scope
              }).result.then(
                      function (card) {
                        console.log('Created card, ready to send:');
                        console.log(card);

                      }, function (error) {
                console.log(error);
              }
              );
            }

            /* TESTING RECEIVE BROADCAST FROM WEBSOCKET SERVICE */
            $rootScope.$on('andreTesting', function (event, data) {
              console.log('BroadcastReceived BOARD:');
              console.log(JSON.parse(data.response.board));
              //self.getAllTemplates();
            });
          }]);
