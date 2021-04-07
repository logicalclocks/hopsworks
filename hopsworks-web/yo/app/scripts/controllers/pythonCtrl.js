/*
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
 */
/**
 * Controller for the python depedencies (settings page).
 */
'use strict';

angular.module('hopsWorksApp')
    .controller('PythonCtrl', ['$scope', '$route', '$routeParams', 'growl', '$location', 'PythonService',
        'ModalService', '$interval', '$mdDialog', 'UtilsService',
        'VariablesService', 'ElasticService', 'UserService',
        function ($scope, $route, $routeParams, growl, $location, PythonService, ModalService, $interval, $mdDialog,
                  UtilsService, VariablesService, ElasticService, UserService) {


            var self = this;
            self.projectId = $routeParams.projectID;

            self.installMode = 'PYPI';

            self.uploadMode = null;
            self.uploadDepPath = "";

            self.gitDep = "";
            self.thirdPartyApiKeys = [];
            self.gitApiKey = "";
            self.gitBackend = 'GITHUB';
            self.privateGitRepo = false;

            self.active = 0;

            self.enabled = false;
            self.installed = false;

            self.loading = false;
            self.loadingText = "";

            self.loadingLibs = false;
            self.loadingCommands = false;

            self.initializingEnvironment = true;

            $scope.activeForm;
            $scope.indextab = 0;

            self.tensorflowVersion = "";

            self.condaResultsMsgShowing = false;

            self.condaResultsMsg = "";

            self.pipResultsMsgShowing = false;

            self.pipResultsMsg = "";

            self.exporting = false;
            self.errorMessage = "";
            self.errorArtifact = "";
            self.errorOp = "";
            self.showLogs = false;

            $scope.sortType = 'library';

            self.pipSearching = false;
            self.condaSearching = false;
            self.installing = {};
            self.uninstalling = {};
            self.upgrading = {};
            self.enabling = false;

            self.condaSearchResults = [];
            self.pipSearchResults = [];
            self.installedLibs = [];
            self.opsStatus = [];
            self.libraryToUpgrade = [];
            self.selectedInstallStatus = {};
            self.numEnvsNotEnabled = 0;
            self.numEnvs = 0;

            self.isRetryingFailedCondaOps = false;

            self.pythonVersion = null;
            self.pythonConflicts = false;

            //https://repo.continuum.io/pkgs/free/linux-64/
            self.condaChannel = "defaults";

            self.pipSearchEnabled = false;

            self.condaSelectedLibs = {};
            self.pipSelectedLibs = {};
            self.environmentTypes = {};

            self.commonVersionPrefixRegexp = new RegExp("^(\\d+[.]\\d+[.])");

            self.environmentImportDef = {};

            self.pipSelectedLib = {
                "channelUrl": self.condaChannel,
                "packageSource": "PIP",
                "library": "",
                "version": ""
            };
            self.condaSelectedLib = {
                "channelUrl": self.condaChannel,
                "packageSource": "CONDA",
                "library": "",
                "version": ""
            };

            self.startLoading = function(label) {
                self.loading = true;
                self.loadingText = label;
            };
            self.stopLoading = function() {
                self.loading = false;
                self.loadingText = "";
            };

            $scope.sortBy = function(sortType) {
                $scope.reverse = ($scope.sortType === sortType) ? !$scope.reverse : false;
                $scope.sortType = sortType;
            };

            var showErrorGrowl = function (error) {
                var errorMsg = typeof error.data.usrMsg !== 'undefined'? error.data.usrMsg : '';
                growl.error(errorMsg, {title: error.data.errorMsg, ttl: 10000});
            };

            self.progress = function () {
                var percent = ((self.numEnvs - self.numEnvsNotEnabled) / self.numEnvs) * 100;
                if (percent === 0) {
                    return 5;
                }
                return percent;
            };

            self.getMachineCount = function (row) {
                return 1;
            };

            var getInstalledTensorFlowVersion = function () {
              VariablesService.getVariable('tensorflow_version')
                .then(function (success) {
                  self.tensorflowVersion = success.data.successMessage;
                }, function (error) {
                  growl.error(error, {title: "Failed to get installed TensorFlow version", ttl: 10000});
              });
            };

            self.getStatus = function (row) {
                var status = typeof row.commands !== 'undefined' && row.commands.count > 0? 'ONGOING' : 'INSTALLED';
                if (status === 'ONGOING') {
                    var countFailed = 0;
                    for (var i = 0; i < row.commands.count; i++) {
                        if (row.commands.items[i].status !== 'FAILED') {
                            break;
                        }
                        countFailed++;
                    }
                    status = countFailed === row.commands.count ? 'FAILED' : status;
                }
                return status;
            };

            self.getOp = function (row) {
                for (var i = 0; i < row.commands.count; i++) {
                    return row.commands.items[i].op
                }
                return "UNKNOWN"
            };

            self.getInstallType = function (row) {
                return row.packageSource;
            };

            self.getVersion = function (row) {
                return typeof row.pythonVersion !== 'undefined'? row.pythonVersion : row.version;
            };

            var getPercent = function (status, row) {
                var full = self.getMachineCount(row);
                var part = self.getCountForStatus(status, row);
                return (part/full) * 100 ;
            };

            var loadThirdPartyApiKeys = function () {
                    UserService.load_secrets().then(
                        function (success) {
                            self.thirdPartyApiKeys = success.data.items
                        }, function (error) {
                            self.errorMsg = (typeof error.data.usrMsg !== 'undefined') ? error.data.usrMsg : "";
                            growl.error(self.errorMsg, { title: error.data.errorMsg, ttl: 5000, referenceId: 1 });
                        }
                    );
            }

            self.getCountForStatus = function (status, row) {
                var count = 0;
                if ('SUCCESS' === status) {
                    count = Math.max(self.getMachineCount(row) - row.commands.count, 0);
                    return count;
                }
                for(var i = 0; i < row.commands.count; i++) {
                    if (status === row.commands.items[i].status) {
                        count++;
                    }
                }
                return Math.min(count, self.getMachineCount(row)); // count should be less than total
            };

            self.installProgress = function(status, row) {
                switch(status) {
                    case 'Done':
                        return getPercent('SUCCESS', row);
                    case 'Ongoing':
                        return getPercent('ONGOING', row) + getPercent('NEW', row);
                    case 'Failed':
                        return getPercent('FAILED', row);
                    default:
                        return 0;
                }
            };

            self.getSyncStatus = function (version) {
                if(version) {
                     PythonService.getEnvironmentCommands(self.projectId, version).then(
                        function (success) {
                            if(success.data.items) {
                                var commands = success.data.items;
                                    for (var i = 0; i < commands.length; i++) {
                                        if (commands[i].op === 'SYNC_BASE_ENV') {
                                            var syncBaseEnv = {}
                                            syncBaseEnv['commands'] = success.data;
                                            syncBaseEnv['type'] = 'commandDTO';
                                            self.opsStatus = [syncBaseEnv]
                                            self.initializingEnvironment = true;
                                            self.loadingLibs = true;
                                            self.loadingCommands = true;
                                            return;
                                        }
                                    }
                            }
                            self.initializingEnvironment = false;
                        },
                        function (error) {
                            self.enabled = false;
                     });
                 }
            };

            self.showConflicts = function () {
                ModalService.conflicts('lg', 'Environment conflicts', self.projectId, self.pythonVersion, '?expand=conflicts')
                            .then(function (success) {}, function (error) {});
            };

            self.getInstalledLibs = function () {

                self.getSyncStatus(self.pythonVersion);

                if (!self.enabled || self.initializingEnvironment || self.pythonVersion === null) {
                    return;
                };

                PythonService.getEnvironments(self.projectId, '?expand=commands').then(
                    function (success) {
                        var envs = success.data.items;
                        var count = success.data.count;
                        if(envs) {
                            self.pythonConflicts = envs[0].pythonConflicts;
                        }
                        var opsStatusList = [];
                        for (var i = 0; i < count; i++) {
                            if (typeof envs[i].commands !== 'undefined' && envs[i].commands.count > 0) {
                                opsStatusList.push(envs[i]);
                            }
                            if(envs[i].commands.items && envs[i].commands.items[0].op === 'IMPORT') {
                                self.pythonVersion = envs[i].pythonVersion;
                            }
                        }
                        PythonService.getLibraries(self.projectId, self.pythonVersion).then(
                            function (success) {
                                self.loadingLibs = false;
                                self.loadingCommands = false;
                                self.updateInstalledLibs(success.data.items);
                                var libraryToUpgradeList = [];
                                var libCount = success.data.count;
                                for (var i = 0; i < libCount; i++) {
                                    if (typeof self.installedLibs[i].commands !== 'undefined' && self.installedLibs[i].commands.count > 0) {
                                       opsStatusList.push(self.installedLibs[i]);
                                    }
                                    if('latestVersion' in self.installedLibs[i] &&
                                       self.commonVersionPrefixRegexp.test(self.installedLibs[i].latestVersion) &&
                                       self.installedLibs[i].version !== self.installedLibs[i].latestVersion &&
                                       self.installedLibs[i].commands.count === 0) {
                                       libraryToUpgradeList.push(self.installedLibs[i]);
                                    }
                                }
                                self.libraryToUpgrade = libraryToUpgradeList;
                                self.opsStatus = opsStatusList;
                            },
                            function (error) {
                                self.loadingLibs = false;
                                self.loadingCommands = false;
                                showErrorGrowl(error);
                            });
                    }, function (error) {

                    });

            };
            self.getInstalledLibs();

            self.updateInstalledLibs = function(libs) {
                if(libs) {
                    if(self.installedLibs.length == 0 || libs.length > self.installedLibs.length) {
                        self.installedLibs = libs;
                    } else {
                        var i=0;
                          self.installedLibs = self.installedLibs.slice(0, libs.length);
                          angular.forEach(libs, function (lib, key) {
                            for (var key in self.installedLibs[i]) {
                                delete self.installedLibs[i][key];
                            }
                            for (var key in lib) {
                                self.installedLibs[i][key] = lib[key];
                            }
                            i++;
                        });
                    }
                }
            };

            //this might be a bit to frequent for refresh rate
            var getInstallationStatusInterval = $interval(function () {
                self.getInstalledLibs();
            }, 5000);

            self.loadLibraries = function () {
                if(self.installedLibs.length === 0) {
                    self.loadingLibs = true;
                }
                self.getInstalledLibs();
            };

            self.loadCommands = function () {
                if(self.opsStatus.length === 0) {
                    self.loadingCommands = true;
                }
                self.getInstalledLibs();
            }

            $scope.$on("$destroy", function () {
                $interval.cancel(getInstallationStatusInterval);
            });

            self.init = function () {
                getInstalledTensorFlowVersion();
                loadThirdPartyApiKeys();
                VariablesService.getCondaDefaultRepo(self.projectId).then(
                    function (success) {
                        self.condaChannel = success.data;
                        self.installed = true;
                    },
                    function (error) {
                        self.installed = false;
                    });
                PythonService.enabled(self.projectId).then(
                    function (success) {
                        self.enabled = true;
                        self.pythonVersion = success.data.items[0].pythonVersion;
                        self.pythonConflicts = success.data.items[0].pythonConflicts;
                        self.pipSearchEnabled = success.data.items[0].pipSearchEnabled;
                        self.getSyncStatus(self.pythonVersion);
                    },
                    function (error) {
                        self.enabled = false;
                    });
            };
            self.init();

            self.enable = function (version) {
                self.enabling = true;
                self.startLoading("Creating Anaconda environment ...")
                PythonService.createEnvironmentFromVersion(self.projectId, version).then(
                    function (success) {
                        self.stopLoading()
                        self.enabled = true;
                        self.enabling = false;
                        self.getSyncStatus(version);
                        self.pythonVersion = version;
                        self.pipSearchEnabled = success.data.pipSearchEnabled;
                        growl.success("Anaconda initializing for this project...", {
                            title: 'Done',
                            ttl: 5000
                        });
                    },
                    function (error) {
                        self.stopLoading()
                        self.enabling = false;
                        showErrorGrowl(error);
                    });
            };

            self.deleteEnvironment = function () {
                PythonService.removeEnvironment(self.projectId, self.pythonVersion).then(
                    function (success) {
                        self.enabled = false;
                        self.enabling = false;
                        self.getInstalledLibs();
                        growl.success("Anaconda removed for this project.", {
                            title: 'Done',
                            ttl: 5000
                        });
                        $route.reload();
                    },
                    function (error) {
                        self.enabling = false;
                        showErrorGrowl(error);
                    });
            }

            self.deleteEnvironmentCommands = function() {
                PythonService.deleteEnvironmentCommands(self.projectId, self.pythonVersion).then(
                    function (success) {
                        growl.success("Anaconda Environment commands deleted.", {
                            title: 'Done',
                            ttl: 5000
                        });
                        $route.reload();
                    },
                    function (error) {
                        self.enabling = false;
                        showErrorGrowl(error);
                    });
            }

            self.destroyAnaconda = function () {

                ModalService.confirm('sm', 'Remove Environment?',
                    "WARNING: Deleting this environment may cause running Jobs and Jupyter Notebooks using the environment in your project to fail. If you want to proceed press 'OK'.")
                    .then(function (success) {
                        self.enabling = true;
                        growl.success("Removing Anaconda environment for this project.....", {
                            title: 'Done',
                            ttl: 2000
                        });
                        PythonService.removeEnvironment(self.projectId, self.pythonVersion).then(
                            function (success) {
                                self.enabled = false;
                                self.enabling = false;
                                self.getInstalledLibs();
                                growl.success("Anaconda removed for this project.", {
                                    title: 'Done',
                                    ttl: 5000
                                });
                                $route.reload();
                            },
                            function (error) {
                                self.enabling = false;
                                showErrorGrowl(error);
                            });
                    }, function (error) {

                    });
            }

            self.showCommandsLogs = function (condaCommand, inProgress) {
                var projectName = UtilsService.getProjectName();
                var op = condaCommand.commands.items[0].op;
                var artifact = ""
                if (condaCommand.type == 'environmentDTO') {
                   artifact = projectName;
                } else {
                   artifact = condaCommand.library;
                }

                self.errorMessage = condaCommand.commands.items[0].errorMessage.replace(/(\n)/gm,"<br>");
                self.errorArtifact = artifact
                self.errorOp = op;
                self.showLogs = true;
            };

            self.showMainUI = function() {
                self.showLogs = false;
            };

            self.exportEnvironment = function () {
                self.exporting = true;
                $scope.indextab = 2;
                PythonService.exportEnvironment(self.projectId).then(
                    function (success) {
                        self.exporting = false;
                        growl.success("Exporting environment operation ongoing. Check your Resources dataset for the .yml file once the operation is finished.", {
                            title: 'Export Ongoing...',
                            ttl: 10000
                        });
                    },
                    function (error) {
                        self.exporting = false;
                        showErrorGrowl(error);
                    });
            }

            //Set some (semi-)constants
            self.selectFileRegexes = {
                "yml": /.yml\b/,
                "txt": /.txt\b/
            };

            self.selectFileErrorMsgs = {
                "yml": "Please select a .yml file. It should have be processable by 'conda env create' command",
                "txt": "Please select a requirements.txt file. It should have be processable by 'pip install -r requirements.txt' command"
            };

            self.selectImportFile = function (type) {
                ModalService.selectEnvironmentImport('lg', self.projectId, self.selectFileRegexes[type.toUpperCase()], self.selectFileErrorMsgs[type.toUpperCase()], type).then(
                    function (success) {
                        self.startLoading("Issuing commands to create environment ...")

                        self.environmentImportDef = success;
                        self.enabling = true;

                        PythonService.createEnvironmentFromImport(self.projectId, self.environmentImportDef).then(
                            function (success) {
                                self.stopLoading()
                                self.enabled = true;
                                self.enabling = false;
                                self.init();
                                self.getInstalledLibs();
                                growl.success("Anaconda initialized for this project.", {
                                    title: 'Done',
                                    ttl: 5000
                                });
                            }, function (error) {
                                self.stopLoading()
                                self.enabling = false;
                                showErrorGrowl(error);
                            });
                    },
                    function (error) {
                        //The user changed their mind.
                    });
            };
            
            self.retryFailedCondaEnvOp = function (row) {
                self.isRetryingFailedCondaOps = true;
                PythonService.retryEnvironmentCommand(self.projectId, self.pythonVersion).then(
                    function (success) {
                        self.isRetryingFailedCondaOps = false;
                        self.getInstalledLibs();
                        growl.success("Retried failed conda ops for this project.", {title: 'Done', ttl: 3000
                        });
                    },
                    function (error) {
                        self.isRetryingFailedCondaOps = false;
                        showErrorGrowl(error);
                    });
            };

            self.retryFailedCondaLibraryOp = function (row) {
                self.isRetryingFailedCondaOps = true;
                PythonService.retryInstallLibrary(self.projectId, self.pythonVersion, row.library).then(
                    function (success) {
                        self.isRetryingFailedCondaOps = false;
                        self.getInstalledLibs();
                        growl.success("Retried failed conda ops for this project.", {title: 'Done', ttl: 3000
                        });
                    },
                    function (error) {
                        self.isRetryingFailedCondaOps = false;
                        showErrorGrowl(error);
                    });
            };

            self.search = function (type) {

                if (type === "PIP") {

                    if (self.pipSelectedLib.library.length < 3) {
                        return;
                    }
                    self.pipSelectedLib.channelUrl = self.condaChannel;
                    self.pipSelectedLib.packageSource = "PIP"
                    self.pipSelectedLib.version = self.pythonVersion;
                    self.pipSearching = true;
                    self.pipResultsMsg = "Pip search can take a good few seconds... bear with us.";
                    self.pipResultsMessageShowing = true;
                    self.pipSearchResults = [];
                    PythonService.search(self.projectId, self.pipSelectedLib).then(
                        function (success) {
                            var count = success.data === "" ? 0 : success.data.count;
                            self.pipSearching = false;
                            self.pipSearchResults = success.data.items;
                            self.pipSelectedLibs = {};
                            if (count === 0) {
                                self.pipResultsMsg = "No results found.";
                            } else {
                                self.pipResultsMessageShowing = false;
                            }
                            for (var i = 0; i < count; i++) {
                                self.pipSearchResults[i].versions.sort(function(a,b){
                                    return new Date(b.uploadTime) - new Date(a.uploadTime);
                                });
                                self.pipSelectedLibs[self.pipSearchResults[i].library] = {
                                    "version": self.pipSearchResults[i].versions[0].version,
                                    "installing": false
                                };
                            }
                        },
                        function (error) {
                            self.pipSearching = false;
                            if (error.status == 204) {
                                self.pipResultsMsg = "No results found.";
                            } else {
                                self.pipResultsMessageShowing = false;
                                showErrorGrowl(error);
                            }
                        });
                } else if (type === "CONDA") {
                    if (self.condaSelectedLib.library.length < 3) {
                        return;
                    }
                    self.condaSelectedLib.channelUrl = self.condaChannel;
                    self.condaSelectedLib.packageSource = "CONDA"
                    self.condaSelectedLib.version = self.pythonVersion;
                    self.condaSearching = true;
                    self.condaResultsMsg = "Conda search can take a good few seconds... bear with us.";
                    self.condaResultsMessageShowing = true;
                    self.condaSearchResults = [];
                    PythonService.search(self.projectId, self.condaSelectedLib).then(
                        function (success) {
                            var count = success.data === "" ? 0 : success.data.count;
                            self.condaSearching = false;
                            self.condaSearchResults = success.data.items;
                            self.condaSelectedLibs = {};
                            if (count === 0) {
                                self.condaResultsMsg = "No results found.";
                            } else {
                                self.condaResultsMessageShowing = false;
                            }
                            for (var i = 0; i < count; i++) {
                                // no upload time in conda
                                // self.condaSearchResults[i].versions.sort(function(a,b){
                                //     return new Date(b.uploadTime) - new Date(a.uploadTime);
                                // });
                                self.condaSelectedLibs[self.condaSearchResults[i].library] = {
                                    "version": self.condaSearchResults[i].versions[0].version,
                                    "installing": false
                                };
                            }
                        },
                        function (error) {
                            self.condaSearching = false;
                            if (error.status == 204) {
                                self.condaResultsMsg = "No results found.";
                            } else {
                                self.condaResultsMessageShowing = false;
                                showErrorGrowl(error);
                            }
                        });
                }
            };

            self.selectDepRegexes = {
              "EGG": /.egg\b/,
              "WHEEL": /.whl\b/,
              "REQUIREMENTS_TXT": /.txt\b/,
              "ENVIRONMENT_YAML": /.yml\b/
            };

            self.selectFile = function (reason) {
              ModalService.selectFile('lg',  self.projectId,  self.selectDepRegexes[reason],
                      "Please select a .whl or .egg file", false).then(
                      function (success) {
                          self.uploadDepPath = success;
                      }, function (error) {
                //The user changed their mind.
              });
            };

            self.upgrade = function (library) {
              PythonService.uninstall(self.projectId, self.pythonVersion, library.library).then(
                  function (success) {
                      self.uninstalling[library.library] = false;
                      self.install(library.library, library.packageSource, library.latestVersion);
                  },
                  function (error) {
                      self.uninstalling[library.library] = false;
                      showErrorGrowl(error);
                  });

            }

            self.install = function (library, packageSource, version) {
                self.installing[library] = true;
                if (packageSource.toUpperCase() === "CONDA") {
                    var data = {
                        "channelUrl": self.condaChannel,
                        "packageSource": packageSource,
                        "library": library,
                        "version": version
                    };
                } else if (packageSource.toUpperCase() === "GIT") {
                    var data = {
                        "channelUrl": packageSource.toLowerCase(),
                        "packageSource": packageSource,
                        "library": library.substring(library.lastIndexOf("/") + 1, library.length),
                        "dependencyUrl": library
                    };
                    if(self.privateGitRepo) {
                        data.gitApiKey = self.gitApiKey.name
                        data.gitBackend = self.gitBackend
                    }
                } else if (packageSource.toUpperCase() === "EGG" || packageSource.toUpperCase() === "WHEEL" || packageSource.toUpperCase() === "REQUIREMENTS_TXT" || packageSource.toUpperCase() === "ENVIRONMENT_YAML") {
                    var data = {
                        "channelUrl": packageSource.toLowerCase(),
                        "packageSource": packageSource,
                        "library": library.substring(library.lastIndexOf("/") + 1, library.length),
                        "dependencyUrl": library
                    };
                } else {
                    var data = {
                        "channelUrl": "pypi",
                        "packageSource": packageSource,
                        "library": library,
                        "version": version
                    };
                }

                PythonService.install(self.projectId, self.pythonVersion, data).then(
                    function (success) {
                        growl.success("Started installing library " + library + ". Click on the 'Ongoing Operations' tab for installation status.", {
                            title: 'Installing',
                            ttl: 10000
                        });
                        self.pipResultsMessageShowing = false;
                        self.condaResultsMessageShowing = false;
                        self.pipSearchResults = [];
                        self.condaSearchResults = [];
                        self.installing[library] = false;
                        self.uploadDepPath = "";
                        self.uploadMode = null;
                        self.gitDep = "";
                        self.gitApiKey = "";
                        self.privateGitRepo = false;
                        self.getInstalledLibs();
                        $scope.activeForm = 2;

                        var msg = success.data;
                        if (msg !== undefined && msg !== null && msg.length > 0) {
                            growl.info(msg, {title: "Creating a new Conda Env", ttl: 10000});
                        }
                    },
                    function (error) {
                        self.installing[library] = false;
                        showErrorGrowl(error);
                    });
            };

            self.deleteCommands = function (library) {
                self.uninstalling[library.library] = true;
                if(library.library === 'tensorflow' && library.version === self.tensorflowVersion) {
                    growl.warning("You are uninstalling TensorFlow " + self.tensorflowVersion + " which is the supported version for this installation, if you encounter issues please install it again"
                     , {title: 'Uninstalling TensorFlow', ttl: 20000});
                }
                PythonService.deleteLibraryCommands(self.projectId, self.pythonVersion, library.library).then(
                    function (success) {
                        self.getInstalledLibs();
                        growl.info("Clearing conda operations", {
                            title: 'Clearing Conda Commands and Uninstalling Library',
                            ttl: 3000
                        });
                    },
                    function (error) {
                        self.uninstalling[library.library] = false;
                        showErrorGrowl(error);
                    });
            };

            self.uninstall = function (library) {
                self.uninstalling[library.library] = true;

                if(library.library === 'tensorflow' && library.version === self.tensorflowVersion) {
                    growl.warning("You are uninstalling TensorFlow " + self.tensorflowVersion + " which is the supported version for this installation, if you encounter issues please install it again"
                     , {title: 'Uninstalling TensorFlow', ttl: 20000});
                }

                PythonService.deleteLibraryCommands(self.projectId, self.pythonVersion, library.library).then(
                    function (success) {
                        self.getInstalledLibs();
                        growl.info("Clearing conda operations", {
                            title: 'Clearing Conda Commands and Uninstalling Library',
                            ttl: 3000
                        });
                        PythonService.uninstall(self.projectId, self.pythonVersion, library.library).then(
                            function (success) {
                                self.getInstalledLibs();
                                self.uninstalling[library.library] = false;
                                var msg = success.data;
                                if (msg !== undefined && msg !== null && msg.length > 0) {
                                    growl.info(msg, {title: "Creating a new Conda Env", ttl: 10000});
                                }
                            },
                            function (error) {
                                self.uninstalling[library.library] = false;
                                showErrorGrowl(error);
                            });
                    },
                    function (error) {
                        self.uninstalling[library.library] = false;
                        showErrorGrowl(error);
                    });
            };
        }
    ]);