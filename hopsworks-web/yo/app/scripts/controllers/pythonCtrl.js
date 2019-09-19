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
        'VariablesService', 'MachineTypeService', 'ElasticService',
        function ($scope, $route, $routeParams, growl, $location, PythonService, ModalService, $interval, $mdDialog,
                  UtilsService, VariablesService, MachineTypeService, ElasticService) {


            var self = this;
            self.projectId = $routeParams.projectID;

            self.active = 0;

            self.enabled = false;
            self.installed = false;

            self.loading = false;
            self.loadingText = "";

            $scope.activeForm;
            $scope.indextab = 0;

            self.condaResultsMsgShowing = false;

            self.condaResultsMsg = "";

            self.pipResultsMsgShowing = false;

            self.pipResultsMsg = "";

            self.exporting = false;
            self.kibanaUI = "";
            self.showLogs = false;

            $scope.sortType = 'preinstalled';

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
            self.selectedInstallStatus = {};
            self.numEnvsNotEnabled = 0;
            self.numEnvs = 0;

            self.isRetryingFailedCondaOps = false;

            self.pythonVersion = "0.0";


            //            https://repo.continuum.io/pkgs/free/linux-64/
            self.condaChannel = "defaults";

            self.condaSelectedLibs = {};
            self.pipSelectedLibs = {};

            self.machineTypeALL = true;
            self.machineTypeCPU = false;
            self.machineTypeGPU = false;
            self.machineTypeVal = "ALL"

            self.environmentTypes = {};

            self.environmentYmlDef = {};
            self.machines = {};

            self.pipSelectedLib = {
                "channelUrl": self.condaChannel,
                "installType": "PIP",
                "lib": "",
                "version": ""
            };
            self.condaSelectedLib = {
                "channelUrl": self.condaChannel,
                "installType": "CONDA",
                "lib": "",
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

            //bit ugly code but some custom behaviour was needed to fix the checkboxes:
            //1. unchecking GPU/CPU should default to ALL
            //2. it should not be possible to uncheck the ALL option
            self.select = function (machineType) {
                // you selected CPU only
                if (self.machineTypeCPU === false && machineType === "CPU") {
                    self.machineTypeVal = "CPU";
                    self.machineTypeCPU = true;
                    self.machineTypeGPU = false;
                    self.machineTypeALL = false;
                    // you selected GPU only
                } else if (self.machineTypeGPU === false && machineType === "GPU") {
                    self.machineTypeVal = "GPU";
                    self.machineTypeCPU = false;
                    self.machineTypeGPU = true;
                    self.machineTypeALL = false;
                    // you unchecked CPU
                } else if (self.machineTypeCPU === true && machineType === "CPU") {
                    self.machineTypeVal = "ALL";
                    self.machineTypeCPU = false;
                    self.machineTypeGPU = false;
                    self.machineTypeALL = true;
                    // you unchecked GPU
                } else if (self.machineTypeGPU === true && machineType === "GPU") {
                    self.machineTypeVal = "ALL";
                    self.machineTypeCPU = false;
                    self.machineTypeGPU = false;
                    self.machineTypeALL = true;
                } else {
                    self.machineTypeVal = "ALL";
                    self.machineTypeCPU = false;
                    self.machineTypeGPU = false;
                    self.machineTypeALL = true;
                }
            };

            self.progress = function () {
                var percent = ((self.numEnvs - self.numEnvsNotEnabled) / self.numEnvs) * 100;
                if (percent === 0) {
                    return 5;
                }
                return percent;
            };

            self.getMachineCount = function (row) {
                return typeof row.machine !== 'undefined'? self.machines[row.machine] : self.machines['ALL'];
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

            self.getVersion = function (row) {
                return typeof row.pythonVersion !== 'undefined'? row.pythonVersion : row.version;
            };

            var getPercent = function (status, row) {
                var full = self.getMachineCount(row);
                var part = self.getCountForStatus(status, row);
                return (part/full) * 100 ;
            };

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

            var getInstalledLibs = function () {
                if (!self.enabled) {
                    return;
                };
                PythonService.getEnvironments(self.projectId).then(
                    function (success) {
                        var envs = success.data.items;
                        var count = success.data.count;
                        var opsStatusList = [];
                        for (var i = 0; i < count; i++) {
                            if (typeof envs[i].commands !== 'undefined' && envs[i].commands.count > 0) {
                                opsStatusList.push(envs[i]);
                            }
                            if(envs[i].commands.items && envs[i].commands.items[0].op === 'YML') {
                                self.pythonVersion = envs[i].pythonVersion;
                            }
                        }
                        PythonService.getLibraries(self.projectId, self.pythonVersion).then(
                            function (success) {
                                self.installedLibs = success.data.items;
                                var libCount = success.data.count;
                                for (var i = 0; i < libCount; i++) {
                                    if (typeof self.installedLibs[i].commands !== 'undefined' && self.installedLibs[i].commands.count > 0) {
                                        opsStatusList.push(self.installedLibs[i]);
                                    }
                                }
                                self.opsStatus = opsStatusList;
                            },
                            function (error) {
                                showErrorGrowl(error);
                            });
                    }, function (error) {

                    });

            };
            getInstalledLibs();

            //this might be a bit to frequent for refresh rate
            var getInstallationStatusInterval = $interval(function () {
                getInstalledLibs();
            }, 5000);

            self.getInstalled = function () {
                getInstalledLibs();
            };
            $scope.$on("$destroy", function () {
                $interval.cancel(getInstallationStatusInterval);
            });

            self.init = function () {
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
                    },
                    function (error) {
                        self.enabled = false;
                    });
                MachineTypeService.machineTypes().then(
                    function (success) {
                        var environmentTypesDTO = success.data.items;
                        var i;
                        for (i = 0; i < environmentTypesDTO.length; i++) {
                            self.environmentTypes[environmentTypesDTO[i].machineType] = environmentTypesDTO[i].numMachines > 0;
                            self.machines[environmentTypesDTO[i].machineType] = environmentTypesDTO[i].numMachines;
                        }
                    },
                    function (error) {
                        showErrorGrowl(error);
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
                        self.getInstalled();
                        self.pythonVersion = version;
                        growl.success("Anaconda initialized for this project.", {
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
                        self.enabled = true;
                        self.enabling = false;
                        self.getInstalled();
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
                                self.enabled = true;
                                self.enabling = false;
                                self.getInstalled();
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
                var artifact_version = ""
                if (condaCommand.type == 'environmentDTO') {
                   artifact = projectName;
                   artifact_version = condaCommand.pythonVersion;
                } else {
                   artifact = condaCommand.library;
                   artifact_version = condaCommand.version;
                }

                ElasticService.getJwtToken(self.projectId).then(
                  function (success) {
                   var kibanaUrl = success.data.kibanaUrl;
                   self.kibanaUI = kibanaUrl + "projectId=" + self.projectId
                                     + "#/discover?_g=()&_a=(columns:!(operation,artifact,artifact_version,return_code),filters:!(('$state':(store:appState),meta:(alias:!n,disabled:!f,"
                                     + "index:'"+ projectName.toLowerCase() +"_kagent-*',key:operation,negate:!f,params:(query:" + op +"),type:phrase,"
                                     + "value:" + op + "),query:(match:(operation:(query:"+ op +",type:phrase)))),('$state':(store:appState),meta:(alias:!n,disabled:!f,"
                                     + "index:'"+ projectName.toLowerCase() + "_kagent-*',key:artifact,negate:!f,params:(query:"+ artifact +"),type:phrase,"
                                     + "value:"+ artifact +"),query:(match:(artifact:(query:"+ artifact +",type:phrase)))),('$state':(store:appState),meta:(alias:!n,disabled:!f,"
                                     + "index:'"+ projectName.toLowerCase() + "_kagent-*',key:artifact_version,negate:!f,params:(query:'" + artifact_version + "'),type:phrase,value:'" + artifact_version + "'),query:(match:(artifact_version:(query:'" + artifact_version + "',type:phrase))))),"
                                     + "index:'"+ projectName.toLowerCase() + "_kagent-*',interval:auto,query:(language:kuery,query:''),sort:!(_score,desc))";
                    self.showLogs = true;
                  }, function (error) {
                    showErrorGrowl(error);
                 });
            };

            self.showMainUI = function() {
                self.showLogs = false;
            };

            self.exportEnvironment = function () {
                self.exporting = true;
                $scope.indextab = 3;
                PythonService.exportEnvironment(self.projectId).then(
                    function (success) {
                        self.exporting = false;
                        growl.success("Exporting environment operation ongoing. Check your Resources dataset for the .yml file(s) once the operation is finished.", {
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
                "yml": /.yml\b/
            };

            self.selectFileErrorMsgs = {
                "yml": "Please select a .yml file. It should have be processable by 'conda env create' command"
            };

            self.selectYmlFile = function () {
                ModalService.selectEnvironmentYml('lg', self.projectId, self.selectFileRegexes['yml'.toUpperCase()], self.selectFileErrorMsgs['yml'.toUpperCase()]).then(
                    function (success) {
                        self.startLoading("Issuing commands to create environment ...")

                        self.environmentYmlDef = success;
                        self.enabling = true;

                        PythonService.createEnvironmentFromYml(self.projectId, self.environmentYmlDef).then(
                            function (success) {
                                self.stopLoading()
                                self.enabled = true;
                                self.enabling = false;
                                self.getInstalled();
                                self.pythonVersion = '0.0';
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

            self.retryFailedCondaOp = function (row) {
                self.isRetryingFailedCondaOps = true;
                PythonService.retryInstallLibrary(self.projectId, self.pythonVersion, row.library).then(
                    function (success) {
                        self.isRetryingFailedCondaOps = false;
                        self.getInstalled();
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

                    if (self.pipSelectedLib.lib.length < 3) {
                        return;
                    }
                    self.pipSelectedLib.channelUrl = self.condaChannel;
                    self.pipSelectedLib.installType = "PIP"
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
                    if (self.condaSelectedLib.lib.length < 3) {
                        return;
                    }
                    self.condaSelectedLib.channelUrl = self.condaChannel;
                    self.condaSelectedLib.installType = "CONDA"
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

            self.install = function (lib, installType, machineType, version) {
                if (version === undefined || version === null || version.length === 0 || version.toUpperCase() === "NONE") {
                    growl.error("Select a version to install from the dropdown list", {
                        title: 'Error',
                        ttl: 3000
                    });
                    return;
                }
                self.installing[lib] = true;
                if (installType === "conda") {
                    var data = {
                        "channelUrl": self.condaChannel,
                        "installType": installType,
                        "machineType": machineType,
                        "lib": lib,
                        "version": version
                    };
                } else {
                    var data = {
                        "channelUrl": "pypi",
                        "installType": installType,
                        "machineType": machineType,
                        "lib": lib,
                        "version": version
                    };
                }

                PythonService.install(self.projectId, self.pythonVersion, data).then(
                    function (success) {
                        growl.success("Click on the 'Installed Python Libraries' tab for more info.", {
                            title: 'Installing',
                            ttl: 5000
                        });
                        self.pipResultsMessageShowing = false;
                        self.condaResultsMessageShowing = false;
                        self.pipSearchResults = [];
                        self.condaSearchResults = [];
                        self.installing[lib] = false;
                        self.getInstalled();
                        $scope.activeForm = 2;
                        var msg = success.data;
                        if (msg !== undefined && msg !== null && msg.length > 0) {
                            growl.info(msg, {title: "Creating a new Conda Env", ttl: 10000});
                        }
                    },
                    function (error) {
                        self.installing[lib] = false;
                        showErrorGrowl(error);
                    });
            };

            self.uninstall = function (lib) {
                self.uninstalling[lib.library] = true;
                PythonService.deleteLibraryCommands(self.projectId, self.pythonVersion, lib.library).then(
                    function (success) {
                        self.getInstalled();
                        growl.info("Clearing conda operations", {
                            title: 'Clearing Conda Commands and Uninstalling Library',
                            ttl: 3000
                        });
                        PythonService.uninstall(self.projectId, self.pythonVersion, lib.library).then(
                            function (success) {
                                self.getInstalled();
                                self.uninstalling[lib.library] = false;
                                var msg = success.data;
                                if (msg !== undefined && msg !== null && msg.length > 0) {
                                    growl.info(msg, {title: "Creating a new Conda Env", ttl: 10000});
                                }
                            },
                            function (error) {
                                self.uninstalling[lib.library] = false;
                                showErrorGrowl(error);
                            });
                    },
                    function (error) {
                        self.uninstalling[lib.library] = false;
                        showErrorGrowl(error);
                    });
            };


            self.upgrade = function (condaChannel, machineType, installType, lib, version) {
                self.upgrading[lib] = true;
                growl.success("VAL: " + machineType, {
                    title: 'ERROR',
                    ttl: 3000
                });
                var data = {
                    "channelUrl": condaChannel,
                    "installType": installType,
                    "machineType": machineType,
                    "lib": lib,
                    "version": version
                };
                PythonService.upgrade(self.projectId, data).then(
                    function (success) {
                        growl.success("Sending command to update: " + lib, {
                            title: 'Updating',
                            ttl: 3000
                        });
                        self.getInstalled();
                        self.upgrading[lib] = false;
                    },
                    function (error) {
                        self.upgrading[lib] = false;
                        showErrorGrowl(error);
                    });
            };

        }
    ]);