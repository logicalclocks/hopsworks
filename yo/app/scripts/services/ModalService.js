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
              sshkeys: function (size) {
                var modalInstance = $modal.open({
                  templateUrl: 'views/sshkeys.html',
                  controller: 'SshkeysCtrl as sshkeysCtrl',
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
              viewSearchResult: function (size, result, projectOrDataset) {
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
                    projectOrDataset: function () {
                      return projectOrDataset;
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
              selectTemplate: function (size, showSkipButton, templateId) {
                var modalInstance = $modal.open({
                  templateUrl: 'views/templateDropdown.html',
                  controller: 'TemplateDropdownCtrl as templateDropdownCtrl',
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
                    showSkipButton: function () {
                      return showSkipButton;
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
                  controller: 'ModifyFieldCtrl',
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
              }
            };
          }]);
