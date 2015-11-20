/**
 * Created by AMore on 2015-04-24.
 */

'use strict';


angular.module('hopsWorksApp')
        .controller('LocalFsCtrl', ['$scope', '$q', '$mdSidenav', '$mdUtil', '$log',
          'LocalFsService', '$routeParams', 'ModalService', 'growl', '$location',
          ,
          function ($scope, $q, $mdSidenav, $mdUtil, $log, LocalFsService, $routeParams,
                  ModalService, growl, $location) {

            var self = this;
            self.working = false;
            //Some variables to keep track of state.
            self.files = []; //A list of files currently displayed to the user.
            self.projectId = $routeParams.projectID; //The id of the project we're currently working in.
            self.pathArray; //An array containing all the path components of the current path. If empty: project root directory.
            self.selected; //The index of the selected file in the files array.
            self.fileDetail; //The details about the currently selected file.

            var localFilesystemService = LocalFsService(self.projectId); //The datasetservice for the current project.

            self.metadataView = {};
            self.availableTemplates = [];
            self.closeSlider = false;

            /*
             * Get all datasets under the current project.
             * @returns {undefined}
             */
            self.getAllDatasets = function () {
              //Get the path for an empty patharray: will get the datasets
              var path = getPath([]);
              localFilesystemService.getContents(path).then(
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
              self.working = true;
              //Get the contents and load them
              localFilesystemService.getContents(newPath).then(
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
             * Remove the inode at the given path. If called on a folder, will 
             * remove the folder and all its contents recursively.
             * @param {type} path. The project-relative path to the inode to be removed.
             * @returns {undefined}
             */
            var removeInode = function (path) {
              localFilesystemService.removeDataSetDir(path).then(
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
                getDirContents();
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
                          localFilesystemService.checkFileExist(filePath).then(
                                  function (success) {
                                    localFilesystemService.fileDownload(filePath);
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

            self.close = function () {
              $mdSidenav('right').close()
                      .then(function () {
                        $log.debug("Closed metadata designer (right)");
                      });

              $mdSidenav('left').close()
                      .then(function () {
                        $log.debug("Closed metadata designer (left)");
                      });
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
