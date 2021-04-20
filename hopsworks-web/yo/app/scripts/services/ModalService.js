/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
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
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

'use strict';

angular.module('hopsWorksApp')
    .factory('ModalService', ['$uibModal', function ($uibModal) {
        return {
            confirm: function (size, title, msg, projectId) {
                var modalInstance = $uibModal.open({
                    templateUrl: 'views/confirmModal.html',
                    controller: 'ModalCtrl as modalCtrl',
                    size: size,
                    resolve: {
                        title: function () {
                            return title;
                        },
                        msg: function () {
                            return msg;
                        },
                        projectId: function () {
                            return projectId;
                        }
                    }
                });
                return modalInstance.result;
            },
            conflicts: function (size, title, projectId, pythonVersion, query) {
                var modalInstance = $uibModal.open({
                    templateUrl: 'views/conflictModal.html',
                    controller: 'ConflictCtrl as conflictCtrl',
                    size: size,
                    resolve: {
                        title: function () {
                            return title;
                        },
                        projectId: function () {
                            return projectId;
                        },
                        pythonVersion: function () {
                            return pythonVersion;
                        },
                        query: function () {
                            return query;
                        }
                    }
                });
                return modalInstance.result;
            },
            certs: function (size, title, msg, projectId) {
                var modalInstance = $uibModal.open({
                    templateUrl: 'views/certsModal.html',
                    controller: 'ModalCtrl as modalCtrl',
                    size: size,
                    resolve: {
                        title: function () {
                            return title;
                        },
                        msg: function () {
                            return msg;
                        },
                        projectId: function () {
                            return projectId;
                        }
                    }
                });
                return modalInstance.result;
            },
            certsPassword: function (size,  password) {
                var modalInstance = $uibModal.open({
                    templateUrl: 'views/certsPasswordModal.html',
                    controller: 'CertsPasswordModalCtrl as certsPasswordModalCtrl',
                    size: size,
                    resolve: {
                        password: function () {
                            return password;
                        }
                    }
                });
                return modalInstance.result;
            },
            reportIssueModal: function (size, title, msg, projectId) {
                var modalInstance = $uibModal.open({
                    templateUrl: 'views/reportIssue.html',
                    controller: 'ModalCtrl as modalCtrl',
                    size: size,
                    resolve: {
                        title: function () {
                            return title;
                        },
                        msg: function () {
                            return msg;
                        },
                        projectId: function () {
                            return projectId;
                        }
                    }
                });
                return modalInstance.result;
            },
            uberPrice: function (size, title, msg, generalPrice, gpuPrice) {
                var modalInstance = $uibModal.open({
                    templateUrl: 'views/uberModal.html',
                    controller: 'UberCtrl as uberCtrl',
                    size: size,
                    resolve: {
                        auth: ['$q','AuthGuardService',
                            function ($q, AuthGuardService) {
                                return AuthGuardService.guardSession($q).then(
                                    function(success){
                                    }, function (error) {
                                        return $q.reject(error)
                                    });
                            }],
                        title: function () {
                            return title;
                        },
                        msg: function () {
                            return msg;
                        },
                        generalPrice: function () {
                            return generalPrice;
                        },
                        gpuPrice: function () {
                            return gpuPrice;
                        }
                    }
                });
                return modalInstance.result;
            },
            jobArgs: function (size, title, msg, args) {
                var modalInstance = $uibModal.open({
                    templateUrl: 'views/jobArgsModal.html',
                    controller: 'JobArgsCtrl as jobArgsCtrl',
                    size: size,
                    resolve: {
                        title: function () {
                            return title;
                        },
                        msg: function () {
                            return msg;
                        },
                        args: function () {
                            return args;
                        }
                    }
                });
                return modalInstance.result;
            },
            alert: function (size, title, msg) {
                var modalInstance = $uibModal.open({
                    templateUrl: 'views/alertModal.html',
                    controller: 'AlertCtrl as alertCtrl',
                    size: size,
                    resolve: {
                        title: function () {
                            return title;
                        },
                        msg: function () {
                            return msg;
                        }
                    }
                });
                return modalInstance.result;
            },
            json: function (size, title, json) {
                var modalInstance = $uibModal.open({
                    templateUrl: 'views/json.html',
                    controller: 'JSONCtrl as jsonCtrl',
                    size: size,
                    resolve: {
                        auth: ['$q','AuthGuardService',
                            function ($q, AuthGuardService) {
                                return AuthGuardService.guardSession($q).then(
                                    function(success){
                                    }, function (error) {
                                        return $q.reject(error)
                                    });
                            }],
                        title: function () {
                            return title;
                        },
                        json: function () {
                            return json;
                        }
                    }
                });
                return modalInstance.result;
            },
            confirmShare: function (size, title, msg, projectId) {
                var modalInstance = $uibModal.open({
                    templateUrl: 'views/confirmShareModal.html',
                    controller: 'ModalCtrl as modalCtrl',
                    size: size,
                    resolve: {
                        auth: ['$q','AuthGuardService',
                            function ($q, AuthGuardService) {
                                return AuthGuardService.guardSession($q).then(
                                    function(success){
                                    }, function (error) {
                                        return $q.reject(error)
                                    });
                            }],
                        title: function () {
                            return title;
                        },
                        msg: function () {
                            return msg;
                        },
                        projectId: function () {
                            return projectId;
                        }
                    }
                });
                return modalInstance.result;
            },
            createProject: function (size) {
                var modalInstance = $uibModal.open({
                    templateUrl: 'views/projectCreateModal.html',
                    controller: 'ProjectCreatorCtrl as projectCreatorCtrl',
                    size: size,
                    resolve: {
                        auth: ['$q','AuthGuardService',
                            function ($q, AuthGuardService) {
                                return AuthGuardService.guardSession($q).then(
                                    function(success){
                                    }, function (error) {
                                        return $q.reject(error)
                                    });
                            }]
                    }
                });
                return modalInstance.result;
            },
            editApiKey: function (size, key) {
                var modalInstance = $uibModal.open({
                    templateUrl: 'views/apiKeyModal.html',
                    controller: 'ApiKeyCtrl as apiKeyCtrl',
                    size: size,
                    resolve: {
                        auth: ['$q','AuthGuardService',
                            function ($q, AuthGuardService) {
                                return AuthGuardService.guardSession($q).then(
                                    function(success){
                                    }, function (error) {
                                        return $q.reject(error)
                                    });
                            }],
                        key: function () {
                            return key;
                        }
                    }
                });
                return modalInstance.result;
            },
            showApiKey: function (size, key) {
                var modalInstance = $uibModal.open({
                    templateUrl: 'views/apiKeyModal.html',
                    controller: 'ApiKeyCtrl as apiKeyCtrl',
                    size: size,
                    backdrop  : 'static',
                    keyboard  : false,
                    resolve: {
                        auth: ['$q','AuthGuardService',
                            function ($q, AuthGuardService) {
                                return AuthGuardService.guardSession($q).then(
                                    function(success){
                                    }, function (error) {
                                        return $q.reject(error)
                                    });
                            }],
                        key: function () {
                            return key;
                        }
                    }
                });
                return modalInstance.result;
            },
            sshKeys: function (size) {
                var modalInstance = $uibModal.open({
                    templateUrl: 'views/sshKeys.html',
                    controller: 'SshKeysCtrl as sshKeysCtrl',
                    size: size,
                    resolve: {
                        auth: ['$q','AuthGuardService',
                            function ($q, AuthGuardService) {
                                return AuthGuardService.guardSession($q).then(
                                    function(success){
                                    }, function (error) {
                                        return $q.reject(error)
                                    });
                            }],
                    }
                });
                return modalInstance.result;
            },
            projectMembers: function (size, projectId) {
                var modalInstance = $uibModal.open({
                    templateUrl: 'views/members.html',
                    controller: 'MemberCtrl as memberCtrl',
                    size: size,
                    resolve: {
                        auth: ['$q','AuthGuardService',
                            function ($q, AuthGuardService) {
                                return AuthGuardService.guardSession($q).then(
                                    function(success){
                                    }, function (error) {
                                        return $q.reject(error)
                                    });
                            }],
                        projectId: function () {
                            return projectId;
                        }
                    }
                });
                return modalInstance.result;
            },
            shareDataset: function (size, datasetPath, dsType, permissions) {
                var modalInstance = $uibModal.open({
                    templateUrl: 'views/shareDataset.html',
                    controller: 'ShareDatasetCtrl as shareDatasetCtrl',
                    size: size,
                    resolve: {
                        auth: ['$q','AuthGuardService',
                            function ($q, AuthGuardService) {
                                return AuthGuardService.guardSession($q).then(
                                    function(success){
                                    }, function (error) {
                                        return $q.reject(error)
                                    });
                            }],
                        datasetPath: function () {
                            return datasetPath;
                        },
                        permissions: function () {
                            return permissions;
                        },
                        dsType: function(){
                           return dsType;
                        }
                    }
                });
                return modalInstance.result;
            },
            sharedWith: function (size, datasetPath, dsType) {
                var modalInstance = $uibModal.open({
                    templateUrl: 'views/sharedWith.html',
                    controller: 'SharedWithCtrl as sharedWithCtrl',
                    size: size,
                    resolve: {
                        auth: ['$q','AuthGuardService',
                            function ($q, AuthGuardService) {
                                return AuthGuardService.guardSession($q).then(
                                    function(success){
                                    }, function (error) {
                                        return $q.reject(error)
                                    });
                            }],
                        datasetPath: function () {
                            return datasetPath;
                        },
                        dsType: function(){
                            return dsType;
                        }
                    }
                });
                return modalInstance.result;
            },
            permissions: function (size, datasetPath, dsType, permissions) {
                var modalInstance = $uibModal.open({
                    templateUrl: 'views/datasetPermissions.html',
                    controller: 'ShareDatasetCtrl as shareDatasetCtrl',
                    size: size,
                    resolve: {
                        auth: ['$q','AuthGuardService',
                            function ($q, AuthGuardService) {
                                return AuthGuardService.guardSession($q).then(
                                    function(success){
                                    }, function (error) {
                                        return $q.reject(error)
                                    });
                            }],
                        datasetPath: function () {
                            return datasetPath;
                        },
                        permissions: function () {
                            return permissions;
                        },
                        dsType: function(){
                            return dsType;
                        }
                    }
                });
                return modalInstance.result;
            },
            unshareDataset: function (size, datasetPath, dsType) {
                var modalInstance = $uibModal.open({
                    templateUrl: 'views/unshareDataset.html',
                    controller: 'UnshareDatasetCtrl as unshareDatasetCtrl',
                    size: size,
                    resolve: {
                        auth: ['$q','AuthGuardService',
                            function ($q, AuthGuardService) {
                                return AuthGuardService.guardSession($q).then(
                                    function(success){
                                    }, function (error) {
                                        return $q.reject(error)
                                    });
                            }],
                        datasetPath: function () {
                            return datasetPath;
                        },
                        dsType: function(){
                            return dsType;
                        }
                    }
                });
                return modalInstance.result;
            },
            viewSearchResult: function (size, response, result, projects) {
                var modalInstance = $uibModal.open({
                    templateUrl: 'views/searchResultDetail.html',
                    controller: 'ViewSearchResultCtrl as viewSearchResultCtrl',
                    size: size,
                    resolve: {
                        auth: ['$q','AuthGuardService',
                            function ($q, AuthGuardService) {
                                return AuthGuardService.guardSession($q).then(
                                    function(success){
                                    }, function (error) {
                                        return $q.reject(error)
                                    });
                            }],
                        response: function () {
                            return response;
                        },
                        result: function () {
                            return result;
                        },
                        projects: function () {
                            return projects;
                        }
                    }
                });
                return modalInstance.result;
            },
            requestAccess: function (size, response, projects) {
                var modalInstance = $uibModal.open({
                    templateUrl: 'views/search/requestAccess.html',
                    controller: 'RequestAccessCtrl as requestAccessCtrl',
                    size: size,
                    resolve: {
                        auth: ['$q', 'AuthGuardService',
                            function ($q, AuthGuardService) {
                                return AuthGuardService.guardSession($q).then(
                                    function (success) {
                                    }, function (error) {
                                        return $q.reject(error)
                                    });
                            }],
                        response: function () {
                            return response;
                        },
                        projects: function () {
                            return projects;
                        }
                    }
                });
                return modalInstance.result;
            },
            viewPublicDataset: function (size, projects, datasetDto) {
                var modalInstance = $uibModal.open({
                    templateUrl: 'views/viewPublicDataset.html',
                    controller: 'ViewPublicDatasetCtrl as viewPublicDatasetCtrl',
                    size: size,
                    resolve: {
                        auth: ['$q','AuthGuardService',
                            function ($q, AuthGuardService) {
                                return AuthGuardService.guardSession($q).then(
                                    function(success){
                                    }, function (error) {
                                        return $q.reject(error)
                                    });
                            }],
                        projects: function () {
                            return projects;
                        },
                        datasetDto: function () {
                            return datasetDto;
                        }
                    }
                });
                return modalInstance.result;
            },
            /**
             * Open a dialog to allow creating a new folder at the given path (excluding the new folder's name).
             * @param {type} size
             * @param {type} location
             * @returns {$uibModal@call;open.result}
             */
            newFolder: function (size, path, datasetType, isDataset) {
                var modalInstance = $uibModal.open({
                    templateUrl: 'views/newDataSet.html',
                    controller: 'DataSetCreatorCtrl as datasetCreatorCtrl',
                    size: size,
                    resolve: {
                        auth: ['$q','AuthGuardService',
                            function ($q, AuthGuardService) {
                                return AuthGuardService.guardSession($q).then(
                                    function(success){
                                        return $q.promise;
                                    }, function (error) {
                                         return $q.reject(error)
                                });
                            }],
                        path: function () {
                            return path;
                        },
                        datasetType: function () {
                            return datasetType;
                        },
                        isDataset: function () {
                            return isDataset;
                        }
                    }
                });
                return modalInstance.result;
            },
            /**
             * Open a modal to preview the file contents.
             * @param {type} size
             * @param {type} location
             * @returns {$uibModal@call;open.result}
             */
            filePreview: function (size, fileName, filePath, projectId, mode) {
                var modalInstance = $uibModal.open({
                    templateUrl: 'views/filePreview.html',
                    controller: 'FilePreviewCtrl as filePreviewCtrl',
                    size: size,
                    resolve: {
                        auth: ['$q','AuthGuardService',
                            function ($q, AuthGuardService) {
                                return AuthGuardService.guardSession($q).then(
                                    function(success){
                                    }, function (error) {
                                        return $q.reject(error)
                                    });
                            }],
                        fileName: function () {
                            return fileName;
                        },
                        filePath: function () {
                            return filePath;
                        },
                        projectId: function () {
                            return projectId;
                        },
                        mode: function () {
                            return mode;
                        }
                    }
                });
                return modalInstance.result;
            },
            upload: function (size, projectId, path, datasetType) {
                var modalInstance = $uibModal.open({
                    templateUrl: 'views/fileUpload.html',
                    controller: 'FileUploadCtrl as fileUploadCtrl',
                    size: size,
                    backdrop: 'static', //prevent user interaction with the background
                    keyboard: false,
                    resolve: {
                        auth: ['$q','AuthGuardService',
                            function ($q, AuthGuardService) {
                                return AuthGuardService.guardSession($q).then(
                                    function(success){
                                    }, function (error) {
                                        return $q.reject(error)
                                    });
                            }],
                        projectId: function () {
                            return projectId;
                        },
                        path: function () {
                            return path;
                        },
                        datasetType: function () {
                            return datasetType;
                        }
                    }
                });
                return modalInstance.result;
            },
            selectFile: function (size, projectId, regex, errorMsg, dirAllowed) {
                var modalInstance = $uibModal.open({
                    templateUrl: 'views/selectFile.html',
                    controller: 'SelectFileCtrl as selectFileCtrl',
                    size: size,
                    resolve: {
                        auth: ['$q','AuthGuardService',
                            function ($q, AuthGuardService) {
                                return AuthGuardService.guardSession($q).then(
                                    function(success){
                                    }, function (error) {
                                        return $q.reject(error)
                                    });
                            }],
                        projectId: function () {
                            return projectId;
                        },
                        regex: function () {
                            return regex;
                        },
                        errorMsg: function () {
                            return errorMsg;
                        },
                        dirAllowed: function () {
                            return dirAllowed;
                        }
                    }
                });
                return modalInstance.result;
            },
            selectEnvironmentImport: function (size, projectId, regex, errorMsg, type) {
                var modalInstance = $uibModal.open({
                    templateUrl: 'views/selectEnvImport.html',
                    controller: 'SelectEnvImportCtrl as selectEnvImportCtrl',
                    size: size,
                    resolve: {
                        auth: ['$q','AuthGuardService',
                            function ($q, AuthGuardService) {
                                return AuthGuardService.guardSession($q).then(
                                    function(success){
                                    }, function (error) {
                                        return $q.reject(error)
                                    });
                            }],
                        projectId: function () {
                            return projectId;
                        },
                        regex: function () {
                            return regex;
                        },
                        errorMsg: function () {
                            return errorMsg;
                        },
                        type: function () {
                            return type;
                        }
                    }
                });
                return modalInstance.result;
            },
            selectDir: function (size, projectId, regex, errorMsg, dirAllowed) {
                var modalInstance = $uibModal.open({
                    templateUrl: 'views/selectDir.html',
                    controller: 'SelectFileCtrl as selectFileCtrl',
                    size: size,
                    resolve: {
                        auth: ['$q','AuthGuardService',
                            function ($q, AuthGuardService) {
                                return AuthGuardService.guardSession($q).then(
                                    function(success){
                                    }, function (error) {
                                        return $q.reject(error)
                                    });
                            }],
                        projectId: function () {
                            return projectId;
                        },
                        regex: function () {
                            return regex;
                        },
                        errorMsg: function () {
                            return errorMsg;
                        },
                        dirAllowed: function () {
                            return dirAllowed;
                        }
                    }
                });
                return modalInstance.result;
            },
            selectProject: function (size, global, projectId, msg, fetchProjectInfo) {
                var modalInstance = $uibModal.open({
                    templateUrl: 'views/selectProject.html',
                    controller: 'SelectProjectCtrl as selectProjectCtrl',
                    size: size,
                    resolve: {
                        auth: ['$q','AuthGuardService',
                            function ($q, AuthGuardService) {
                                return AuthGuardService.guardSession($q).then(
                                    function(success){
                                    }, function (error) {
                                        return $q.reject(error)
                                    });
                            }],
                        projectId: function () {
                            return projectId;
                        },
                        global: function () {
                            return global;
                        },
                        msg: function () {
                            return msg;
                        },
                        fetchProjectInfo: function(){
                            return fetchProjectInfo;
                        }
                    }
                });
                return modalInstance.result;
            },
            createSchema: function (size, projectId, projectIsGuide) {
                var modalInstance = $uibModal.open({
                    templateUrl: 'views/schemaCreate.html',
                    controller: 'SchemaCreateCtrl as schemaCreateCtrl',
                    size: size,
                    resolve: {
                        auth: ['$q','AuthGuardService',
                            function ($q, AuthGuardService) {
                                return AuthGuardService.guardSession($q).then(
                                    function(success){
                                    }, function (error) {
                                        return $q.reject(error)
                                    });
                            }],
                        projectId: function () {
                            return projectId;
                        },
                        projectIsGuide: function () {
                            return projectIsGuide;
                        }
                    }
                });
                return modalInstance.result;
            },
            viewSchemaContent: function (size, projectId, schemaName, schemaVersion) {
                var modalInstance = $uibModal.open({
                    templateUrl: 'views/schemaViewContent.html',
                    controller: 'SchemaViewContentCtrl as schemaViewContentCtrl',
                    size: size,
                    resolve: {
                        auth: ['$q','AuthGuardService',
                            function ($q, AuthGuardService) {
                                return AuthGuardService.guardSession($q).then(
                                    function(success){
                                    }, function (error) {
                                        return $q.reject(error)
                                    });
                            }],
                        projectId: function () {
                            return projectId;
                        },
                        schemaName: function () {
                            return schemaName;
                        },
                        schemaVersion: function () {
                            return schemaVersion;
                        }
                    }
                });
                return modalInstance.result;
            },
            viewSharedSchemaContent: function (size, projectId, schemaName, schemaContent) {
              var modalInstance = $uibModal.open({
                templateUrl: 'views/sharedSchemaViewContent.html',
                controller: 'SharedSchemaViewContentCtrl as sharedSchemaViewContentCtrl',
                size: size,
                resolve: {
                  auth: ['$q','AuthGuardService',
                    function ($q, AuthGuardService) {
                      return AuthGuardService.guardSession($q).then(
                        function(success){
                        }, function (error) {
                          return $q.reject(error)
                        });
                    }],
                  projectId: function () {
                    return projectId;
                  },
                  schemaName: function () {
                    return schemaName;
                  },
                  schemaContent: function () {
                    return schemaContent;
                  }
                }
              });
              return modalInstance.result;
            },

            updateSchemaContent: function (size, projectId, schemaName, schemaVersion) {
                var modalInstance = $uibModal.open({
                    templateUrl: 'views/schemaUpdateContent.html',
                    controller: 'SchemaUpdateContentCtrl as schemaUpdateContentCtrl',
                    size: size,
                    resolve: {
                        auth: ['$q','AuthGuardService',
                            function ($q, AuthGuardService) {
                                return AuthGuardService.guardSession($q).then(
                                    function(success){
                                    }, function (error) {
                                        return $q.reject(error)
                                    });
                            }],
                        projectId: function () {
                            return projectId;
                        },
                        schemaName: function () {
                            return schemaName;
                        },
                        schemaVersion: function () {
                            return schemaVersion;
                        }
                    }
                });
                return modalInstance.result;
            },

            createTopic: function (size, projectId, projectIsGuide) {
                var modalInstance = $uibModal.open({
                    templateUrl: 'views/createTopic.html',
                    controller: 'CreateTopicCtrl as createTopicCtrl',
                    size: size,
                    resolve: {
                        auth: ['$q','AuthGuardService',
                            function ($q, AuthGuardService) {
                                return AuthGuardService.guardSession($q).then(
                                    function(success){
                                    }, function (error) {
                                        return $q.reject(error)
                                    });
                            }],
                        projectId: function () {
                            return projectId;
                        },
                        projectIsGuide: function () {
                            return projectIsGuide;
                        }
                    }
                });
                return modalInstance.result;
            },
            createTopicAcl: function (size, projectId, topicName, projectName) {
                var modalInstance = $uibModal.open({
                    templateUrl: 'views/createTopicAcl.html',
                    controller: 'CreateAclCtrl as createAclCtrl',
                    size: size,
                    resolve: {
                        auth: ['$q','AuthGuardService',
                            function ($q, AuthGuardService) {
                                return AuthGuardService.guardSession($q).then(
                                    function(success){
                                    }, function (error) {
                                        return $q.reject(error)
                                    });
                            }],
                        projectId: function () {
                            return projectId;
                        },
                        topicName: function () {
                            return topicName;
                        },
                        projectName: function () {
                            return projectName;
                        }
                    }
                });
                return modalInstance.result;
            },
            jobDetails: function (size, job, projectId) {
                var modalInstance = $uibModal.open({
                    templateUrl: 'views/jobDetails.html',
                    controller: 'JobDetailCtrl as jobDetailCtrl',
                    size: size,
                    resolve: {
                        auth: ['$q','AuthGuardService',
                            function ($q, AuthGuardService) {
                                return AuthGuardService.guardSession($q).then(
                                    function(success){
                                    }, function (error) {
                                        return $q.reject(error)
                                    });
                            }],
                        job: function () {
                            return job;
                        },
                        projectId: function () {
                            return projectId;
                        }
                    }
                });
                return modalInstance.result;
            },
            executionDetails: function (size, job, execution) {
                var modalInstance = $uibModal.open({
                    templateUrl: 'views/executionDetails.html',
                    controller: 'ExecutionDetailsCtrl as executionDetailsCtrl',
                    size: size,
                    resolve: {
                        auth: ['$q', '$location', 'AuthService',
                            function ($q, $location, AuthService) {
                                return AuthService.session().then(
                                    function (success) {
                                    },
                                    function (err) {
                                        $location.path('/login');
                                        $location.replace();
                                        return $q.reject(err);
                                    });
                            }],
                        job: function () {
                            return job;
                        },
                        execution: function () {
                            return execution;
                        }
                    }
                });
                return modalInstance.result;
            },
            jobUI: function (size, job, projectId) {
                var modalInstance = $uibModal.open({
                    templateUrl: 'views/jobUI.html',
                    controller: 'jobUICtrl as jobUICtrl',
                    size: size,
                    resolve: {
                        auth: ['$q','AuthGuardService',
                            function ($q, AuthGuardService) {
                                return AuthGuardService.guardSession($q).then(
                                    function(success){
                                    }, function (error) {
                                        return $q.reject(error)
                                    });
                            }],
                        job: function () {
                            return job;
                        },
                        projectId: function () {
                            return projectId;
                        }
                    }
                });
                return modalInstance.result;
            },
            enterName: function (size, title, newName) {
                var modalInstance = $uibModal.open({
                    templateUrl: 'views/enterNameModal.html',
                    controller: 'EnterNameCtrl as enterNameCtrl',
                    size: size,
                    backdrop: 'static',
                    resolve: {
                        auth: ['$q','AuthGuardService',
                            function ($q, AuthGuardService) {
                                return AuthGuardService.guardSession($q).then(
                                    function(success){
                                    }, function (error) {
                                        return $q.reject(error)
                                    });
                            }],
                        title: function () {
                            return title;
                        },
                        newName: function () {
                            return newName;
                        }
                    }
                });
                return modalInstance.result;
            },
            messages: function (size, selected) {
                var modalInstance = $uibModal.open({
                    templateUrl: 'views/messageModal.html',
                    controller: 'MessageCtrl as messageCtrl',
                    size: size,
                    resolve: {
                        auth: ['$q','AuthGuardService',
                            function ($q, AuthGuardService) {
                                return AuthGuardService.guardSession($q).then(
                                    function(success){
                                    }, function (error) {
                                        return $q.reject(error)
                                    });
                            }],
                        selected: function () {
                            return selected;
                        }
                    }
                });
                return modalInstance.result;
            },

            setupDownload: function (size, projectId, params) {
                var modalInstance = $uibModal.open({
                    templateUrl: 'views/setupDownload.html',
                    controller: 'SetupDownloadCtrl as setupDownloadCtrl',
                    size: size,
                    resolve: {
                        auth: ['$q','AuthGuardService',
                            function ($q, AuthGuardService) {
                                return AuthGuardService.guardSession($q).then(
                                    function(success){
                                    }, function (error) {
                                        return $q.reject(error)
                                    });
                            }],
                        projectId: function () {
                            return projectId;
                        },
                        datasetId: function () {
                            return params.publicId;
                        },
                        defaultDatasetName: function () {
                            return params.name;
                        },
                        bootstrap: function () {
                            return params.bootstrap;
                        }
                    }
                });
                return modalInstance.result;
            },
            noteCreate: function (size, title, msg, val) {
                var modalInstance = $uibModal.open({
                    templateUrl: 'views/noteCreateModal.html',
                    controller: 'NoteCreateCtrl as noteCreateCtrl',
                    size: size,
                    resolve: {
                        auth: ['$q','AuthGuardService',
                            function ($q, AuthGuardService) {
                                return AuthGuardService.guardSession($q).then(
                                    function(success){
                                    }, function (error) {
                                        return $q.reject(error)
                                    });
                            }],
                        title: function () {
                            return title;
                        },
                        msg: function () {
                            return msg;
                        },
                        val: function () {
                            return val;
                        }
                    }
                });
                return modalInstance.result;
            },
            transformGraph: function (size, servingId, inGraph, outGraph) {
                var modalInstance = $uibModal.open({
                    templateUrl: 'views/transformGraphModal.html',
                    controller: 'TransformGraphCtrl as transformGraphCtrl',
                    size: size,
                    resolve: {
                        servingId: function () {
                            return servingId;
                        },
                        inGraph: function () {
                            return inGraph;
                        },
                        outGraph: function () {
                            return outGraph;
                        }
                    }
                });
                return modalInstance.result;
            },
            remoteUserConsent: function (size, data, val) {
                var modalInstance = $uibModal.open({
                    templateUrl: 'views/remoteUserConsentModal.html',
                    controller: 'RemoteUserConsentModalCtrl as remoteUserConsentModalCtrl',
                    size: size,
                    resolve: {
                        data: function () {
                            return data;
                        },
                        val: function () {
                            return val;
                        }
                    }
                });
                return modalInstance.result;
            },

            viewFeaturestoreStatistic: function (size, projectId, featuregroup, statisticType, statisticData, trainingDataset) {
                var modalInstance = $uibModal.open({
                    templateUrl: 'views/featurestoreStatisticModal.html',
                    controller: 'featurestoreStatisticModalCtrl as featurestoreStatisticModalCtrl',
                    size: size,
                    resolve: {
                        auth: ['$q','AuthGuardService',
                            function ($q, AuthGuardService) {
                                return AuthGuardService.guardSession($q).then(
                                    function(success){
                                    }, function (error) {
                                        return $q.reject(error)
                                    });
                            }],
                        projectId: function () {
                            return projectId;
                        },
                        featuregroup: function () {
                            return featuregroup;
                        },
                        statisticType: function () {
                            return statisticType;
                        },
                        statisticData: function () {
                            return statisticData;
                        },
                        trainingDataset: function () {
                            return trainingDataset;
                        }
                    }
                });
                return modalInstance.result;
            },
            updateFeaturestoreStatistic: function (size, projectId, featuregroup, trainingDataset, projectName,
                                                   featurestore, settings) {
                var modalInstance = $uibModal.open({
                    templateUrl: 'views/featurestoreUpdateStatisticModal.html',
                    controller: 'updateFeaturestoreStatisticModalCtrl as updateFeaturestoreStatisticModalCtrl',
                    size: size,
                    resolve: {
                        auth: ['$q','AuthGuardService',
                            function ($q, AuthGuardService) {
                                return AuthGuardService.guardSession($q).then(
                                    function(success){
                                    }, function (error) {
                                        return $q.reject(error)
                                    });
                            }],
                        projectId: function () {
                            return projectId;
                        },
                        featuregroup: function () {
                            return featuregroup;
                        },
                        trainingDataset: function () {
                            return trainingDataset;
                        },
                        featurestore: function () {
                            return featurestore;
                        },
                        projectName: function () {
                            return projectName;
                        },
                        settings: function () {
                            return settings;
                        }
                    }
                });
                return modalInstance.result;
            },
            storageConnectorViewInfo: function (size, projectId, storageConnector, featurestore, settings) {
                var modalInstance = $uibModal.open({
                    templateUrl: 'views/storageConnectorViewInfo.html',
                    controller: 'storageConnectorViewInfoCtrl as storageConnectorViewInfoCtrl',
                    size: size,
                    resolve: {
                        auth: ['$q','AuthGuardService',
                            function ($q, AuthGuardService) {
                                return AuthGuardService.guardSession($q).then(
                                    function(success){
                                    }, function (error) {
                                        return $q.reject(error)
                                    });
                            }],
                        projectId: function () {
                            return projectId;
                        },
                        storageConnector: function () {
                            return storageConnector;
                        },
                        featurestore: function () {
                            return featurestore;
                        },
                        settings: function () {
                            return settings;
                        }
                    }
                });
                return modalInstance.result;
            },
            selectFeatureType: function (size, online, settings) {
                var modalInstance = $uibModal.open({
                    templateUrl: 'views/selectFeatureType.html',
                    controller: 'selectFeatureTypeCtrl as selectFeatureTypeCtrl',
                    size: size,
                    resolve: {
                        auth: ['$q','AuthGuardService',
                            function ($q, AuthGuardService) {
                                return AuthGuardService.guardSession($q).then(
                                    function(success){
                                    }, function (error) {
                                        return $q.reject(error)
                                    });
                            }],
                        online: function() {
                            return online;
                        },
                        settings: function () {
                            return settings;
                        },
                    }
                });
                return modalInstance.result;
            },
            viewServingInfo: function (size, projectId, serving, isKubernetes) {
                var modalInstance = $uibModal.open({
                    templateUrl: 'views/servingViewInfo.html',
                    controller: 'servingViewInfoCtrl as servingViewInfoCtrl',
                    size: size,
                    resolve: {
                        auth: ['$q','AuthGuardService',
                            function ($q, AuthGuardService) {
                                return AuthGuardService.guardSession($q).then(
                                    function(success){
                                    }, function (error) {
                                        return $q.reject(error)
                                    });
                            }],
                        projectId: function () {
                            return projectId;
                        },
                        serving: function () {
                            return serving;
                        },
                        isKubernetes: function () {
                            return isKubernetes;
                        }
                    }
                });
                return modalInstance.result;
            },
            viewModelInfo: function (size, projectId, model) {
                var modalInstance = $uibModal.open({
                    templateUrl: 'views/modelViewInfo.html',
                    controller: 'modelViewInfoCtrl as modelViewInfoCtrl',
                    size: size,
                    resolve: {
                        auth: ['$q','AuthGuardService',
                            function ($q, AuthGuardService) {
                                return AuthGuardService.guardSession($q).then(
                                    function(success){
                                    }, function (error) {
                                        return $q.reject(error)
                                    });
                            }],
                        projectId: function () {
                            return projectId;
                        },
                        model: function () {
                            return model;
                        }
                    }
                });
                return modalInstance.result;
            },
            addOperator2AirflowDag: function(size, operator, jobs, addedOperators) {
                var modalInstance = $uibModal.open({
                    templateUrl: 'views/airflowDagOperatorProperties.html',
                    controller: 'DagComposerModalsCtrl as dagComposerModalsCtrl',
                    size: size,
                    resolve: {
                        auth: ['$q', '$location', 'AuthService',
                            function ($q, $location, AuthService) {
                                return AuthService.session().then(
                                    function (success) {
                                    },
                                    function (err) {
                                        $location.path('/login');
                                        $location.replace();
                                        return $q.reject(err);
                                    });
                            }],
                            operator: function() {
                                return operator;
                            },
                            jobs: function() {
                                return jobs;
                            },
                            addedOperators: function() {
                                return addedOperators;
                            }
                    }
                });
                return modalInstance.result;
            },
            selectFromList: function(size, msg, list) {
                var modalInstance = $uibModal.open({
                    templateUrl: 'views/selectFromList.html',
                    controller: 'SelectFromListCtrl as selectFromListCtrl',
                    size: size,
                    resolve: {
                        auth: ['$q', '$location', 'AuthService',
                            function ($q, $location, AuthService) {
                                return AuthService.session().then(
                                    function (success) {
                                    },
                                    function (err) {
                                        $location.path('/login');
                                        $location.replace();
                                        return $q.reject(err);
                                    });
                            }],
                        msg: function() {
                            return msg;
                        },
                        list: function() {
                            return list;
                        }
                    }
                });
                return modalInstance.result;
            },
            notebookAttachedJupyterConfigurationViewInfo: function (size, jupyterConfig) {
                var modalInstance = $uibModal.open({
                    templateUrl: 'views/notebookAttachedJupyterConfigInfo.html',
                    controller: 'NotebookAttachedJupyterConfigCtrl as notebookAttachedJupyterConfigCtrl',
                    size: size,
                    resolve: {
                        auth: ['$q', '$location', 'AuthService',
                            function ($q, $location, AuthService) {
                                return AuthService.session().then(
                                    function (success) {
                                    },
                                    function (err) {
                                        $location.path('/login');
                                        $location.replace();
                                        return $q.reject(err);
                                    });
                            }],
                        jupyterConfig: function () {
                            return jupyterConfig
                        }
                    }
                });
                return modalInstance.result;
            }
        };
    }]);
