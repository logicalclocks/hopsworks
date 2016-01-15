'use strict';

angular.module('hopsWorksApp')
    .controller('CharonCtrl', ['$scope', '$routeParams',
      'growl', 'ModalService', 'CharonService','$modalStack',
      function ($scope, $routeParams, growl, ModalService, CharonService, $modalStack) {

        var self = this;
        self.projectId = $routeParams.projectID;
        var charonService = CharonService(self.projectId);

        self.working = false;
        self.selectedHdfsPath = "";
        self.toHDFS = true;
        self.charonFilename = "";
        self.mySiteID = "";
        self.string = "";
        self.isReadChecked = false;
        self.isWriteChecked = false;
        self.granteeId = "";
        self.availableSiteIDs = "";
        self.permissions = "";
        self.regex = /^(?!.*?__|.*?&|.*? |.*?\/|.*\\|.*?\?|.*?\*|.*?:|.*?\||.*?'|.*?\"|.*?<|.*?>|.*?%|.*?\(|.*?\)|.*?\;|.*?#).*$/;

        $scope.switchDirection = function (projectName) {
          self.toHDFS = !self.toHDFS;
          self.selectedCharonPath = "";
          self.selectedHdfsPath = "";
          if (!self.toHDFS) {
            self.selectedCharonPath = "/srv/Charon/charon_fs/" + projectName;
          }
        }
        /**
         * Callback for when the user selected a file.
         * @param {String} reason
         * @param {String} path
         * @returns {undefined}
         */
        self.onCharonPathSelected = function (path) {
          self.selectedCharonPath = path;
        };

        self.onHdfsPathSelected = function (path) {
          self.selectedHdfsPath = path;
        };

        self.copyFile = function () {
          self.working = true;
          if (self.toHDFS === true) {
            var op = {
              "charonPath": self.selectedCharonPath,
              "hdfsPath": self.selectedHdfsPath
            };
            charonService.copyFromCharonToHdfs(op)
                .then(function (success) {
                    self.working = false;
                  growl.success(success.data.successMessage, {title: 'Success', ttl: 2000});
                  $scope.$broadcast("copyFromCharonToHdfs", {});
                },
                    function (error) {
                      self.working = false;
                      growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000});
                    });
          } else {
            var op = {
              "charonPath": self.selectedCharonPath,
              "hdfsPath": self.selectedHdfsPath
            };
            charonService.copyFromHdfsToCharon(op)
                .then(function (success) {
                    self.working = false;
                  growl.success(success.data.successMessage, {title: 'Success', ttl: 2000});
                  $scope.$broadcast("refreshCharon", {});
                },
                    function (error) {
                      self.working = false;
                      growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000});
                    });
          }
        };

        self.selectHdfsFile = function () {
          ModalService.selectFile('lg', "/[^]*/",
              "problem selecting file").then(
              function (success) {
                self.onFileSelected("hdfs:/" + success);
              }, function (error) {
            //The user changed their mind.
            growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000});
          });
        };

        self.selectHdfsDir = function () {
          ModalService.selectDir('lg', "/[^]*/",
              "problem selecting file").then(
              function (success) {
                self.onDirSelected("hdfs:/" + success);
              }, function (error) {
            //The user changed their mind.
            growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000});
          });
        };


        self.selectCharonFile = function () {
          ModalService.selectLocalFile('lg', "/[^]*/",
              "problem selecting file").then(
              function (success) {
                self.onFileSelected(success);
              }, function (error) {
            //The user changed their mind.
            growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000});
          });
        };

        self.selectCharonDir = function () {
          ModalService.selectLocalDir('lg', "/[^]*/",
              "problem selecting file").then(
              function (success) {
                self.onDirSelected(success);
              }, function (error) {
            //The user changed their mind.
            growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000});
          });
        };

        self.newRepository = function () {
          ModalService.createRepository('lg').then(
            function () {
              $scope.$broadcast("refreshCharon", {});
            }, function () {
              growl.info("Closed without saving.", {title: 'Info', ttl: 5000});
            });
        };

        self.addSite = function () {
          ModalService.addSite('lg').then(
            function () {
              //loadProjects();
              //loadActivity();
            }, function () {
              growl.info("Closed without saving.", {title: 'Info', ttl: 5000});
            });
        };

        self.remoteRepository = function () {
          ModalService.remoteRepository('lg').then(
            function () {
              //loadProjects();
              //loadActivity();
            }, function () {
              growl.info("Closed without saving.", {title: 'Info', ttl: 5000});
            });
        };

        self.shareRepository = function () {
          ModalService.shareRepository('lg').then(
            function (success) {
              self.permission = success.permission;
              self.granteeId = success.granteeId;

              var string = (self.selectedCharonPath).replace("/srv/Charon/charon_fs", "");
              var op = {
                "string": string,
                "permissions": self.permissions,
                "granteeId": self.granteeId
              };
              charonService.share(op)
                .then(function (success) {
                    self.working = false;
                    growl.success(success.data.successMessage, {title: 'Success', ttl: 2000});
                  },
                  function (error) {
                    self.working = false;
                    growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000});
                  });
            }, function () {
              growl.info("Closed without saving.", {title: 'Info', ttl: 5000});
            });
        };

        var getMySiteId = function () {
          charonService.getMySiteId().then(
            function (success) {
              self.mySiteID = success.data;
              console.log("Success getting my Site ID "+success);
            }, function (error) {
              console.log("Error getting my Site ID ");
              console.log(error);
            });
        };

        var listSiteIds = function () {
          charonService.listSiteIds().then(
            function (success) {
              self.availableSiteIDs = success.data;
              console.log("Success getting available Site IDs "+success);
            }, function (error) {
              console.log("Error getting available Site IDs ");
              console.log(error);
            });
        };

        self.addSiteID = function () {
            var op = {
              "string": self.string
            };
            charonService.addSiteID(op)
              .then(function (success) {
                  self.working = false;
                  growl.success(success.data.successMessage, {title: 'Success', ttl: 2000});
                  $modalStack.getTop().key.close();
                },
                function (error) {
                  self.working = false;
                  growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000});
                });
        };

        self.mkdir = function () {
          var op = {
            "string": self.string
          };
          charonService.mkdir(op)
            .then(function (success) {
                self.working = false;
                growl.success(success.data.successMessage, {title: 'Success', ttl: 2000});
                $modalStack.getTop().key.close();
              },
              function (error) {
                self.working = false;
                growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000});
              });
        };

        self.createSharedRepository = function () {
          self.getPermissions();
          var op = {
            "string": self.string,
            "permissions": self.permissions,
            "granteeId": self.granteeId
          };
          charonService.createSharedRepository(op)
            .then(function (success) {
                self.working = false;
                growl.success(success.data.successMessage, {title: 'Success', ttl: 2000});
                $modalStack.getTop().key.close();
              },
              function (error) {
                self.working = false;
                growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000});
              });
        };

        self.share = function () {
          self.getPermissions();
          var op = {
            "permissions": self.permissions,
            "granteeId": self.granteeId
          };
          $modalStack.getTop().key.close(op);
        };

        self.close = function () {
          $modalStack.getTop().key.dismiss();
        };

        self.getPermissions = function () {
          if (self.isReadChecked && !self.isWriteChecked){
            self.permissions = "r";
          } else {
            self.permissions = "rw";
          }
        };

        self.createRepo = function () {
          if (!self.isReadChecked && !self.isWriteChecked){
            self.mkdir();
          } else {
            self.createSharedRepository();
          }
        };

        self.init = function () {
          getMySiteId();
          listSiteIds();
        };

        self.init();

      }]);
