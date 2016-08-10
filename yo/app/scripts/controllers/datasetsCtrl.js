'use strict';

angular.module('hopsWorksApp')
        .controller('DatasetsCtrl', ['$scope', '$q', '$mdSidenav', '$mdUtil', '$log',
          'DataSetService', '$routeParams','$route', 'ModalService', 'growl', '$location',
          'MetadataHelperService',
          function ($scope, $q, $mdSidenav, $mdUtil, $log, DataSetService, $routeParams,
                  $route, ModalService, growl, $location, MetadataHelperService) {

            var self = this;
            self.working = false;
            //Some variables to keep track of state.
            self.files = []; //A list of files currently displayed to the user.
            self.projectId = $routeParams.projectID; //The id of the project we're currently working in.
            self.pathArray; //An array containing all the path components of the current path. If empty: project root directory.
            self.sharedPathArray; //An array containing all the path components of a path in a shared dataset 
            self.selected = null; //The index of the selected file in the files array.
            self.selectedList = []; //The index of the selected file in the files array.
            self.fileDetail; //The details about the currently selected file.
            self.sharedPath; //The details about the currently selected file.
            self.routeParamArray = [];
            self.fileContent;
            var dataSetService = DataSetService(self.projectId); //The datasetservice for the current project.

            $scope.isPublic = true;
            
            $scope.tgState = true;

            $scope.status = {
              isopen: false
            };

            self.onSuccess = function (e) {
              growl.success("Copied to clipboard", {title: '', ttl: 1000});
              e.clearSelection();
            };

            $scope.toggleDropdown = function ($event) {
              $event.preventDefault();
              $event.stopPropagation();
              $scope.status.isopen = !$scope.status.isopen;
            };


            self.metadataView = {};
            self.availableTemplates = [];
            self.closeSlider = false;

            self.breadcrumbLen = function () {
              if (self.pathArray === undefined || self.pathArray === null) {
                return 0;
              }
              var displayPathLen = 10;
              if (self.pathArray.length <= displayPathLen) {
                return self.pathArray.length - 1;
              }
              return displayPathLen;
            }

            self.cutBreadcrumbLen = function () {
              if (self.pathArray === undefined || self.pathArray === null) {
                return false;
              }
              if (self.pathArray.length - self.breadcrumbLen() > 0) {
                return true;
              }
              return false;
            };

            self.selectInode = function (inode) {
              // add to selectedList
            };

            self.selectInode = function (inode) {
              // splice
            };

            $scope.sort = function (keyname) {
              $scope.sortKey = keyname;   //set the sortKey to the param passed
              $scope.reverse = !$scope.reverse; //if true make it false and vice versa
            };

            /**
             * watch for changes happening in service variables from the other controller
             */
            $scope.$watchCollection(MetadataHelperService.getAvailableTemplates, function (availTemplates) {
              if (!angular.isUndefined(availTemplates)) {
                self.availableTemplates = availTemplates;
              }
            });


            $scope.$watch(MetadataHelperService.getDirContents, function (response) {
              if (response === "true") {
                getDirContents();
                MetadataHelperService.setDirContents("false");
              }
            });

            self.isShared = function () {
              var top = self.pathArray[0].split("::");
              if (top.length === 1) {
                return false;
              }
              return true;
            };
            
            self.sharedDatasetPath = function () {
              var top = self.pathArray[0].split("::");
              if (top.length === 1) {
                self.sharedPathArray = [];
                return;
              }
              // /proj::shared_ds/path/to  -> /proj/ds/path/to
              // so, we add '1' to the pathLen
              self.sharedPathArray = new Array(self.pathArray.length + 1);
              self.sharedPathArray[0] = top[0];
              self.sharedPathArray[1] = top[1];
              for (var i=1; i<pathArray.length; i++ ) {
                self.sharedPathArray[i+1] = pathArray[i];
              }
              return self.sharedPathArray;
            };            


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

            $scope.$on("copyFromCharonToHdfs", function (event, args) {
              var newPathArray = self.pathArray;
              //Convert into a path
              var newPath = getPath(newPathArray);
              self.working = true;
              //Get the contents and load them
              dataSetService.getContents(newPath).then(
                      function (success) {
                        //Reset the selected file
                        self.selected = null;
                        self.fileDetail = null;
                        //Set the current files and path
                        self.files = success.data;
                        self.pathArray = newPathArray;
                        self.working = false;
                        console.log(success);
                      }, function (error) {
                self.working = false;
                console.log("Error getting the contents of the path " + getPath(newPathArray));
                console.log(error);
              });
            });

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
              } else if (self.routeParamArray){
                newPathArray = self.pathArray.concat(self.routeParamArray);
              } else {
                newPathArray = self.pathArray;
              }
              //Convert into a path
              var newPath = getPath(newPathArray);
              self.working = true;
              //Get the contents and load them
              dataSetService.getContents(newPath).then(
                      function (success) {
                        //Reset the selected file
                        self.selected = null;
                        self.fileDetail = null;
                        //Set the current files and path
                        self.files = success.data;
                        self.pathArray = newPathArray;
                        self.working = false;
                        console.log(success);
                      }, function (error) {
                        if (error.data.errorMsg.indexOf("Path is not a directory.") > -1) {
                          var popped = newPathArray.pop();
                          console.log(popped);
                          self.openDir({name:popped, dir:false, underConstruction:false});
                          self.pathArray = newPathArray;
                          self.routeParamArray = [];
                          //growl.info(error.data.errorMsg, {title: 'Info', ttl: 2000});
                          getDirContents();
                        } else if (error.data.errorMsg.indexOf("Path not found :") > -1) {
                          self.routeParamArray = [];
                          //$route.updateParams({fileName:''});
                          growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000});
                          getDirContents();
                        }
                        self.working = false;
                        console.log("Error getting the contents of the path " 
                                     + getPath(newPathArray));
                        console.log(error);
              });
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
              if ($routeParams.datasetName && $routeParams.fileName) {
                //file name is set: get the contents
                var paths = $routeParams.fileName.split("/");
                paths.forEach(function(entry) {
                  self.routeParamArray.push(entry);
                });               
              } 
              getDirContents();
              $scope.tgState = true;
            };

            init();

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
                        growl.success(success.data.successMessage, {title: 'Success', ttl: 1000});
                        getDirContents();
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000});
              });
            };

            /**
             * Open a modal dialog for folder creation. The folder is created at the current path.
             * @returns {undefined}
             */
            self.newDataSetModal = function () {
              ModalService.newFolder('md', getPath(self.pathArray)).then(
                      function (success) {
                        growl.success(success.data.successMessage, {title: 'Success', ttl: 1000});
                        getDirContents();
                      }, function (error) {
                //The user changed his/her mind. Don't really need to do anything.
//                getDirContents();
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
             * Makes the dataset public for anybody within the local cluster or any outside cluster.
             * @param id inodeId
             */
            self.makePublic = function (id) {

              ModalService.confirm('sm', 'Confirm', 'Are you sure you want to make this DataSet public? \n\
This will make all its files available for any registered user to download and process.').then(
                      function (success) {
                        dataSetService.makePublic(id).then(
                                function (success) {
                                  growl.success(success.data.successMessage, {title: 'The DataSet is now Public.', ttl: 1500});
                                  getDirContents();
                                }, function (error) {
                          growl.error(error.data.errorMsg, {title: 'Error', ttl: 1000});
                        });

                      }
              );

            };
            
             self.removePublic = function (id) {

              ModalService.confirm('sm', 'Confirm', 'Are you sure you want to make this DataSet private? \n\
This will make all its files unavailable to other projects unless you share it explicitly.').then(
                      function (success) {
                        dataSetService.removePublic(id).then(
                                function (success) {
                                  growl.success(success.data.successMessage, {title: 'The DataSet is now Private.', ttl: 1500});
                                  getDirContents();
                                }, function (error) {
                          growl.error(error.data.errorMsg, {title: 'Error', ttl: 1000});
                        });

                      }
              );
            };           
            

            self.parentPathArray = function () {
              var newPathArray = self.pathArray.slice(0);
              var clippedPath = newPathArray.splice(1, newPathArray.length - 1);
              return clippedPath;
            };

            self.filePreview = function (fileName) {
              var previewPathArray = self.pathArray.slice(0);
              previewPathArray.push(fileName);
              var filePath = getPath(previewPathArray);
              //Retrieve the preview of the file and display it with the Modal
              dataSetService.filePreview(filePath).then(
                                function (success) {
                                  self.fileContent = success.data.successMessage;
                                  ModalService.filePreview('lg', filePath, self.fileContent).then(
                                    function (success) {
                                    });
                                }, function (error) {
                          growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000});
                        });
              
            };
            
            self.move = function (inodeId, name) {
              ModalService.selectDir('lg', "/[^]*/",
                      "problem selecting file").then(
                      function (success) {
                        var destPath = success;
                        // Get the relative path of this DataSet, relative to the project home directory
                        // replace only first occurrence 
                        var relPath = destPath.replace("/Projects/" + self.projectId + "/", "");
                        var finalPath = relPath + "/" + name;

                        dataSetService.move(inodeId, finalPath).then(
                                function (success) {
//                                  self.openDir(relPath);
                                  getDirContents();
                                  growl.success(success.data.successMessage, {title: 'Moved successfully. Opened dest dir: ' + relPath, ttl: 2000});
                                }, function (error) {
                          growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000});
                        });


                      }, function (error) {
                //The user changed their mind.
              });

            };


            self.rename = function (inodeId) {

              var pathComponents = self.pathArray.slice(0);
              var newPath = getPath(pathComponents);
              var destPath = newPath + '/';
              var newName = "New Name";
              ModalService.enterName('lg', "Rename File or Directory", newName).then(
                      function (success) {
                        var fullPath = destPath + success.newName;
                        dataSetService.move(inodeId, fullPath).then(
                                function (success) {
                                  getDirContents();
                                }, function (error) {
                          growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000});
                        });

                      });
            };
            /**
             * Opens a modal dialog for file upload.
             * @returns {undefined}
             */
            self.uploadFile = function () {
              var templateId = -1;

              ModalService.upload('lg', self.projectId, getPath(self.pathArray), templateId).then(
                      function (success) {
                        growl.success(success.data.successMessage, {title: 'Success', ttl: 1000});
                        getDirContents();
                      }, function (error) {
//                growl.info("Closed without saving.", {title: 'Info', ttl: 5000});
                getDirContents();
              });
            };

            /**
             * Sends a request to erasure code a file represented by the given path.
             * It checks
             * .. if the given path resolves to a file or a dir
             * .. if the given path is an existing file
             * .. if the given file is large enough (comprises more than 10 blocks)
             * 
             * If all of the above are met, the compression takes place in an asynchronous operation
             * and the user gets notified when it finishes via a message
             * 
             * @param {type} file
             * @returns {undefined}
             */
            self.compress = function (file) {
              var pathArray = self.pathArray.slice(0);
              pathArray.push(file.name);
              var filePath = getPath(pathArray);

              //check if the path is a dir
              dataSetService.isDir(filePath).then(
                      function (success) {
                        var object = success.data.successMessage;
                        switch (object) {
                          case "DIR":
                            ModalService.alert('sm', 'Alert', 'You can only compress files');
                            break;
                          case "FILE":
                            //if the path is a file go on
                            dataSetService.checkFileExist(filePath).then(
                                    function (successs) {
                                      //check the number of blocks in the file
                                      dataSetService.checkFileBlocks(filePath).then(
                                              function (successss) {
                                                var noOfBlocks = parseInt(successss.data);
                                                console.log("NO OF BLOCKS " + noOfBlocks);
                                                if (noOfBlocks >= 10) {
                                                  ModalService.alert('sm', 'Confirm', 'This operation is going to run in the background').then(
                                                          function (modalSuccess) {
                                                            console.log("FILE PATH IS " + filePath);
                                                            dataSetService.compressFile(filePath);
                                                          });
                                                } else {
                                                  growl.error("The requested file is too small to be compressed", {title: 'Error', ttl: 5000});
                                                }
                                              }, function (error) {
                                        growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000});
                                      });
                                    });
                        }

                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000});
              });
            };

            /**
             * Opens a modal dialog for sharing.
             * @returns {undefined}
             */
            self.share = function (name) {
              ModalService.shareDataset('md', name).then(
                      function (success) {
                        growl.success(success.data.successMessage, {title: 'Success', ttl: 1000});
                      }, function (error) {
              });
            };

            /**
             * Upon click on a inode in the browser:
             *  + If folder: open folder, fetch contents from server and display.
             *  + If file: open a confirm dialog prompting for download.
             * @param {type} file
             * @returns {undefined}
             */
            self.openDir = function (file) {
              if (file.dir) {
                var newPathArray = self.pathArray.slice(0);
                newPathArray.push(file.name);
                getDirContents(newPathArray);
              } else if (!file.underConstruction) {
                ModalService.confirm('sm', 'Confirm', 'Do you want to download this file?').then(
                        function (success) {
                          var downloadPathArray = self.pathArray.slice(0);
                          downloadPathArray.push(file.name);
                          var filePath = getPath(downloadPathArray);
                          dataSetService.checkFileExist(filePath).then(
                                  function (success) {
                                    dataSetService.fileDownload(filePath);
                                  }, function (error) {
                            growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000});
                          });
                        }
                );
              } else {
                growl.info("File under construction.", {title: 'Info', ttl: 5000});
              }
            };

            /**
             * Go up to parent directory.
             * @returns {undefined}
             */
            self.back = function () {
              var newPathArray = self.pathArray.slice(0);
              newPathArray.pop();
              if (newPathArray.length === 0) {
                $location.path('/project/' + self.projectId + '/datasets');
              } else {
                getDirContents(newPathArray);
              }
            };
            self.goToDataSetsDir = function () {
              $location.path('/project/' + self.projectId + '/datasets');
            }
            /**
             * Go to the folder at the index in the pathArray array.
             * @param {type} index
             * @returns {undefined}
             */
            self.goToFolder = function (index) {
              var newPathArray = self.pathArray.slice(0);
              newPathArray.splice(index, newPathArray.length - index);
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

            self.deselect = function () {
              self.selected = null;
              self.fileDetail = null;
              self.sharedPath = null;
            };

            self.toggleLeft = buildToggler('left');
            self.toggleRight = buildToggler('right');

            function buildToggler(navID) {
              var debounceFn = $mdUtil.debounce(function () {
                $mdSidenav(navID).toggle()
                        .then(function () {
                          MetadataHelperService.fetchAvailableTemplates()
                                  .then(function (response) {
                                    self.availableTemplates = JSON.parse(response.board).templates;
                                  });
                        });
              }, 300);
              return debounceFn;
            }
            ;

          }]);

/**
 * Turn the array <i>pathArray</i> containing, path components, into a path string.
 * @param {type} pathArray
 * @returns {String}
 */
var getPath = function (pathArray) {
  return pathArray.join("/");
};
