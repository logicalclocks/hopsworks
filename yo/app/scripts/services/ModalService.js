'use strict';

angular.module('hopsWorksApp')
        .factory('ModalService', ['$modal', function ($modal) {
            return {
              confirm: function (size, title, msg) {
                var modalInstance = $modal.open({
                  templateUrl: 'views/confirmModal.html',
                  controller: 'ModalCtrl as ctrl',
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
              alert: function (size, title, msg) {
                var modalInstance = $modal.open({
                  templateUrl: 'views/alertModal.html',
                  controller: 'AlertCtrl as ctrl',
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
              confirmShare: function (size, title, msg) {
                var modalInstance = $modal.open({
                  templateUrl: 'views/confirmShareModal.html',
                  controller: 'ModalCtrl as ctrl',
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
              createProject: function (size) {
                var modalInstance = $modal.open({
                  templateUrl: 'views/projectModal.html',
                  controller: 'ProjectCreatorCtrl as projectCreatorCtrl',
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
                      }]
                  }
                });
                return modalInstance.result;
              },
              projectSettings: function (size) {
                var modalInstance = $modal.open({
                  templateUrl: 'views/projectSettingsModal.html',
                  controller: 'ProjectCtrl as projectCtrl',
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
                      }]
                  }
                });
                return modalInstance.result;
              },
              profile: function (size) {
                var modalInstance = $modal.open({
                  templateUrl: 'views/profile.html',
                  controller: 'ProfileCtrl as profileCtrl',
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
                      }]
                  }
                });
                return modalInstance.result;
              },
              sshKeys: function (size) {
                var modalInstance = $modal.open({
                  templateUrl: 'views/sshKeys.html',
                  controller: 'SshKeysCtrl as sshKeysCtrl',
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
                      }]
                  }
                });
                return modalInstance.result;
              },
              projectMembers: function (size, projectId) {
                var modalInstance = $modal.open({
                  templateUrl: 'views/members.html',
                  controller: 'MemberCtrl as memberCtrl',
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
                    projectId: function () {
                      return projectId;
                    }
                  }
                });
                return modalInstance.result;
              },
              shareDataset: function (size, dsName) {
                var modalInstance = $modal.open({
                  templateUrl: 'views/shareDataset.html',
                  controller: 'ShareDatasetCtrl as shareDatasetCtrl',
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
                    dsName: function () {
                      return dsName;
                    }
                  }
                });
                return modalInstance.result;
              },
              viewSearchResult: function (size, result, datatype, projects) {
                var modalInstance = $modal.open({
                  templateUrl: 'views/viewSearchResult.html',
                  controller: 'ViewSearchResultCtrl as viewSearchResultCtrl',
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
                    result: function () {
                      return result;
                    },
                    datatype: function () {
                      return datatype;
                    },
                    projects: function () {
                      return projects;
                    }
                  }
                });
                return modalInstance.result;
              },
              /**
               * Open a dialog to allow creating a new folder at the given path (excluding the new folder's name).
               * @param {type} size
               * @param {type} location
               * @returns {$modal@call;open.result}
               */
              newFolder: function (size, path) {
                var modalInstance = $modal.open({
                  templateUrl: 'views/newDataSet.html',
                  controller: 'DataSetCreatorCtrl as datasetCreatorCtrl',
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
                    path: function () {
                      return path;
                    }
                  }
                });
                return modalInstance.result;
              },
              upload: function (size, projectId, path, templateId) {
                var modalInstance = $modal.open({
                  templateUrl: 'views/fileUpload.html',
                  controller: 'FileUploadCtrl as fileUploadCtrl',
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
                    projectId: function () {
                      return projectId;
                    },
                    path: function () {
                      return path;
                    },
                    templateId: function () {
                      return templateId;
                    }
                  }
                });
                return modalInstance.result;
              },
              selectFile: function (size, regex, errorMsg) {
                var modalInstance = $modal.open({
                  templateUrl: 'views/selectFile.html',
                  controller: 'SelectFileCtrl as selectFileCtrl',
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
                    regex: function () {
                      return regex;
                    },
                    errorMsg: function () {
                      return errorMsg;
                    }
                  }
                });
                return modalInstance.result;
              },
              selectDir: function (size, regex, errorMsg) {
                var modalInstance = $modal.open({
                  templateUrl: 'views/selectDir.html',
                  controller: 'SelectDirCtrl as selectDirCtrl',
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
                    regex: function () {
                      return regex;
                    },
                    errorMsg: function () {
                      return errorMsg;
                    }
                  }
                });
                return modalInstance.result;
              },
              jobDetails: function (size, job, projectId) {
                var modalInstance = $modal.open({
                  templateUrl: 'views/jobDetails.html',
                  controller: 'JobDetailCtrl as jobDetailCtrl',
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
                    projectId: function () {
                      return projectId;
                    }
                  }
                });
                return modalInstance.result;
              },
              modifyField: function (scope) {
                var modalInstance = $modal.open({
                  templateUrl: 'views/metadata/modifyFieldDialog.html',
                  controller: 'ModifyFieldCtrl as modifyFieldCtrl',
                  scope: scope,
                  size: 'md',
                  backdrop: 'static',
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
                    scope: function () {
                      return scope;
                    }
                  }
                });
                return modalInstance.result;
              },
              importTemplate: function (size) {
                var modalInstance = $modal.open({
                  templateUrl: 'views/importTemplate.html',
                  controller: 'ImportTemplateCtrl as importTemplateCtrl',
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
                      }]
                  }
                });
                return modalInstance.result;
              },
              addNewField: function (scope) {
                var modalInstance = $modal.open({
                  templateUrl: 'views/metadata/newFieldModal.html',
                  controller: 'NewFieldCtrl as newFieldCtrl',
                  scope: scope,
                  size: 'md',
                  backdrop: 'static',
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
                    scope: function () {
                      return scope;
                    }
                  }
                });
                return modalInstance.result;
              },
              attachTemplate: function (size, file, templateId) {
                var modalInstance = $modal.open({
                  templateUrl: 'views/metadata/attachTemplateDialog.html',
                  controller: 'AttachTemplateCtrl as attachTemplateCtrl',
                  size: size,
                  backdrop: 'static',
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
                    templateId: function () {
                      return templateId;
                    },
                    file: function () {
                      return file;
                    }
                  }
                });
                return modalInstance.result;
              },
              detachTemplate: function (size, file, templateId) {
                var modalInstance = $modal.open({
                  templateUrl: 'views/metadata/detachTemplateDialog.html',
                  controller: 'DetachTemplateCtrl as detachTemplateCtrl',
                  size: size,
                  backdrop: 'static',
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
                    templateId: function () {
                      return templateId;
                    },
                    file: function () {
                      return file;
                    }
                  }
                });
                return modalInstance.result;
              },
              messages: function (size, selected) {
                var modalInstance = $modal.open({
                  templateUrl: 'views/messageModal.html',
                  controller: 'MessageCtrl as messageCtrl',
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
                    selected: function () {
                      return selected;
                    }
                  }
                });
                return modalInstance.result;
              }
            };
          }]);