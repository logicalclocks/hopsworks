'use strict';

angular.module('hopsWorksApp')
		.controller('CharonCtrl', ['$scope', '$routeParams',
		  'growl', 'ModalService', 'CharonService', '$modalStack',
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
			self.path = "";
			self.availableSiteIds = "";
			self.shareSiteID = "";
			self.sharedRepos = "";
			self.token = "";
			self.permissions = "";
			self.regex = /^(?!.*?__|.*?&|.*? |.*?\/|.*\\|.*?\?|.*?\*|.*?:|.*?\||.*?'|.*?\"|.*?<|.*?>|.*?%|.*?\(|.*?\)|.*?\;|.*?#).*$/;
			self.name = "";
			self.id = "";
			self.email = "";
			self.addr = "";
			self.selectedCharonPath = "";
			self.isOpen = false;

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
			  var sitesAvailable = true;
			  if (Object.prototype.toString.call(self.availableSiteIds) === '[object Array]') {
				if (self.availableSiteIds.length < 1) {
				  sitesAvailable = false;
				}
			  } else {
				sitesAvailable = false;
			  }
			  if (sitesAvailable === false) {
				growl.error("You have no sites available to share with. First, add a site.", {title: 'Info', ttl: 5000});
				return;
			  }
			  ModalService.createRepository('lg').then(
					  function () {
						$scope.$broadcast("refreshCharon", {});
						self.listShares();
					  }, function () {
				growl.info("Closed without saving.", {title: 'Info', ttl: 5000});
			  });
			};

			self.addSite = function () {
			  ModalService.addSite('lg').then(
					  function () {
						self.listSiteIds();
						//loadProjects();
						//loadActivity();
					  }, function () {
				growl.info("Closed without saving.", {title: 'Info', ttl: 5000});
			  });
			};

			self.importRepository = function () {
			  ModalService.remoteRepository('lg').then(
					  function () {
						//loadProjects();
						//loadActivity();
						self.listShares();
						self.listSiteIds();
						$scope.$broadcast("refreshCharon", {});
					  }, function () {
				growl.info("Closed without saving.", {title: 'Info', ttl: 5000});
			  });
			};



			self.shareRepository = function () {
			  ModalService.shareRepository('lg').then(
					  function (success) {
						self.permission = success.permission;
						self.granteeId = success.granteeId;

						if (self.selectedCharonPath === "") {
						  growl.error("Invalid repository name.", {title: 'Error', ttl: 5000});
						  return;
						}

						var string = (self.selectedCharonPath).replace("/srv/Charon/charon_fs", "");
						var op = {
						  "token": "",
						  "path": string,
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
						console.log("Success getting my Site ID " + success);
					  }, function (error) {
				console.log("Error getting my Site ID ");
				console.log(error);
			  });
			};

			self.listSiteIds = function () {
			  charonService.listSiteIds().then(
					  function (success) {
						self.availableSiteIds = success.data.registeredSites;
						console.log("Success getting available Site IDs " + success);
						console.log(self.availableSiteIds);
					  }, function (error) {
				console.log("Error getting available Site IDs ");
				console.log(error);
			  });
			};

			self.listShares = function () {
			  charonService.listShares().then(
					  function (success) {
						self.sharedRepos = success.data.sharedRepos;
						console.log("Success getting shared Repos" + success);
					  }, function (error) {
				console.log("Error getting shared Repos");
				console.log(error);
			  });
			};

			self.addSiteId = function () {
			  var op = {
				"name": self.name,
				"siteId": self.id,
				"email": self.email,
				"addr": self.addr
			  };
			  charonService.addSiteId(op)
					  .then(function (success) {
						self.working = false;
						growl.success(success.data.successMessage, {title: 'Success', ttl: 2000});
						$modalStack.getTop().key.close();
						self.listSiteIds();
					  },
							  function (error) {
								self.working = false;
								growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000});
							  });
			};

			$scope.$on("refreshCharon", function (event, args) {
			  self.listSiteIds();
			  self.listShares();
			});



			self.removeSiteId = function (siteId) {
			  charonService.removeSiteId(siteId)
					  .then(function (success) {
						self.working = false;
						growl.success(success.data.successMessage, {title: 'Success', ttl: 2000});
						self.listSiteIds();
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
			  // Copy read/write check button options to a permssions attribute
			  self.getPermissions();
			  var op = {
				"granteeId": self.granteeId.siteId,
				"token": "",
				"path": self.selectedCharonPath,
				"permissions": self.permissions
			  };
			  charonService.createSharedRepository(op)
					  .then(function (success) {
						self.working = false;
						growl.success(success.data.successMessage, {title: 'Success', ttl: 2000});
						$modalStack.getTop().key.close();
						self.listShares();
					  },
							  function (error) {
								self.working = false;
								growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000});
								$modalStack.getTop().key.close();
							  });
			};

			self.removeShare = function (siteId, path) {

			  var op = {
				"granteeId": siteId,
				"path": path,
				"permissions": "",
				"token": ""
			  };
			  charonService.removeShare(op)
					  .then(function (success) {
						self.working = false;
						growl.success(success.data.successMessage, {title: 'Success', ttl: 2000});
						self.listShares();
					  },
					  function (error) {
						self.working = false;
						growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000});
					  });
			};


			self.importRepo = function () {
			  charonService.importRepo(self.token).then(
					  function (success) {
						console.log("Success importing Repo. It will appear shortly." + success);
						self.listSiteIds();
					  }, function (error) {
				console.log("Error importing repo with token: " + self.token);
				console.log(error);
			  });
			};




//			self.share = function () {
//			  self.getPermissions();
//			  var op = {
//				"permissions": self.permissions,
//				"granteeId": self.granteeId
//			  };
//			  $modalStack.getTop().key.close(op);
//			};

			self.close = function () {
			  $modalStack.getTop().key.dismiss();
			};

			self.getPermissions = function () {
			  if (self.isReadChecked && !self.isWriteChecked) {
				self.permissions = "r";
			  } else {
				self.permissions = "rw";
			  }
			};

			self.createRepo = function () {
			  if (!self.isReadChecked && !self.isWriteChecked) {
				self.mkdir();
			  } else {
				self.createSharedRepository();
			  }
			};

			self.init = function () {
			  getMySiteId();
			  self.listSiteIds();
			  self.listShares();
			};

			self.init();

		  }]);
