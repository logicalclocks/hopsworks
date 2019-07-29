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

'use strict'

angular.module('hopsWorksApp')
        .controller('ProfileCtrl', ['UserService', '$location', '$scope', 'md5', 'growl', '$uibModalInstance','$cookies', 'ProjectService', 'SecurityQuestions',
          function (UserService, $location, $scope, md5, growl, $uibModalInstance, $cookies, ProjectService, SecurityQuestions) {

            var self = this;
            self.working = false;
            self.credentialWorking = false;
            self.twoFactorWorking = false;
            self.secretsWorking = false;
            self.securityQAWorking = false;
            self.qrCodeWorking = false;
            self.noPassword = false;
            self.otp = $cookies.get('otp');
            self.emailHash = '';
            self.master = {};
            self.masterTwoFactor = {};
            self.user = {
              firstname: '',
              lastname: '',
              email: '',
              phoneNumber: '',
              twoFactor: ''
            };

            self.loginCredes = {
              oldPassword: '',
              newPassword: '',
              confirmedPassword: ''
            };

            self.securityQA = {
                oldPassword: '',
                securityQuestion: '',
                securityAnswer: ''
            };
            
            self.secrets = [];

            self.secret = {
              name: '',
              secret: '',
              visibility: 'PRIVATE',
              scope: -1
            };

            self.twoFactorAuth = {
              password: '',
              twoFactor: ''
            };

            SecurityQuestions.getQuestions().then(function(success) {
               self.securityQuestions = success.data;
            });

            self.profile = function () {
              UserService.profile().then(
                      function (success) {
                        self.user = success.data;
                        self.emailHash = md5.createHash(self.user.email || '');
                        self.master = angular.copy(self.user);
                        self.twoFactorAuth.twoFactor = self.master.twoFactor;
                        self.user.numRemainingProjects = self.user.maxNumProjects-self.user.numCreatedProjects;
                      },
                      function (error) {
                        self.errorMsg = error.data.errorMsg;
                      });
            };

            self.profile();

            self.updateProfile = function () {
              self.working = true;
              UserService.updateProfile(self.user).then(
                      function (success) {
                        self.working = false;
                        self.user = success.data;
                        self.master = angular.copy(self.user);
                        growl.success("Your profile is now saved.", {title: 'Success', ttl: 5000, referenceId: 1});
                        $scope.profileForm.$setPristine();
                      }, function (error) {
                        self.working = false;
                        self.errorMsg = (typeof error.data.usrMsg !== 'undefined')? error.data.usrMsg : "";
                        growl.error(self.errorMsg, {title: error.data.errorMsg, ttl: 5000, referenceId: 1});
              });
            };

            self.credentialsMsg = {
              successMessage: '',
              errorMessage:''
            };
            self.changeLoginCredentials = function (form) {
              self.credentialsMsg = {
                successMessage: '',
                errorMessage:''
              };
              self.credentialWorking = true;
              if (form.$valid) {
                UserService.changeLoginCredentials(self.loginCredes).then(
                        function (success) {
                          self.credentialWorking = false;
                          self.loginCredes.oldPassword= '';
                          self.loginCredes.newPassword= '';
                          self.loginCredes.confirmedPassword= '';
                          form.$setPristine();
                          self.credentialsMsg.successMessage = success.data.successMessage;
                        }, function (error) {
                          self.credentialWorking = false;
                          setErrorMsg(error, self.credentialsMsg);
                });
              }
            };

            self.load_secrets = function () {
              self.secretsWorking = true;
              UserService.load_secrets().then(
                function (success) {
                  self.secrets = success.data.items
                  if (!self.secrets) {
                    self.secrets = []
                  }
                  self.secretsWorking = false;
                }, function (error) {
                  self.secretsWorking = false;
                  self.errorMsg = (typeof error.data.usrMsg !== 'undefined')? error.data.usrMsg : "";
                  growl.error(self.errorMsg, {title: error.data.errorMsg, ttl: 5000, referenceId: 1});
                }
              );
            }

            self.delete_secret = function (secret) {
              self.secretsWorking = true;
              UserService.delete_secret(secret.name).then(
                function (success) {
                  self.load_secrets();
                  self.secretsWorking = false;
                }, function (error) {
                  self.secretsWorking = false;
                  self.errorMsg = (typeof error.data.usrMsg !== 'undefined')? error.data.usrMsg : "";
                  growl.error(self.errorMsg, {title: error.data.errorMsg, ttl: 5000, referenceId: 1});
                }
              );
            }

            self.selected_secret_project_vis

            self.add_secret = function(isFormValid) {
              self.secretsWorking = true;
              if (isFormValid) {
                if (self.secret.visibility.toUpperCase() === 'PROJECT') {
                  if (!self.selected_secret_project_vis) {
                    growl.error('Visibility is Project but no project has been selected',
                      {title: 'Could not add Secret', ttl: 5000, referenceId: 1})
                      return;
                  }
                  self.secret.scope = self.selected_secret_project_vis.id
                }
                UserService.add_secret(self.secret).then(
                  function (success) {
                    self.load_secrets();
                    self.secretsWorking = false;
                    growl.success("Added new secret", {title: 'Success', ttl: 3000, referenceId: 1});
                  }, function (error) {
                    self.secretsWorking = false;
                    self.errorMsg = (typeof error.data.usrMsg !== 'undefined')? error.data.usrMsg : "";
                    growl.error(self.errorMsg, {title: error.data.errorMsg, ttl: 5000, referenceId: 1});
                  }
                );
              }
            };

            self.user_projects = [];
            self.project_visibility_selected = function () {
              self.user_projects = []
              ProjectService.projects().$promise.then(
                function(success) {
                  for (var i = 0; i < success.length; i++) {
                    var name = success[i].project.name
                    var id = success[i].project.id
                    var project = {
                      name: name,
                      id: id
                    }
                    self.user_projects.push(project)
                  }
                }, function(error) {
                  self.errorMsg = (typeof error.data.usrMsg !== 'undefined')? error.data.usrMsg : "";
                  growl.error(self.errorMsg, {title: error.data.errorMsg, ttl: 3000, referenceId: 1});
                }
              )
            }

            self.private_visibility_selected = function () {
              self.user_projects = []
            }

            self.delete_all_secrets = function() {
              UserService.delete_all_secrets().then(
                function (success) {
                  self.secrets = []
                }, function (error) {
                  self.errorMsg = (typeof error.data.usrMsg !== 'undefined')? error.data.usrMsg : "";
                  growl.error(self.errorMsg, {title: error.data.errorMsg, ttl: 5000, referenceId: 1});
                }
              );
            };

            self.securityQAMsg = {
                successMessage: '',
                errorMessage:''
            };
            self.changeSecurityQA = function (form) {
                self.securityQAMsg = {
                    successMessage: '',
                    errorMessage:''
                };
                self.securityQAWorking = true;
                if (form.$valid) {
                    UserService.changeSecurityQA(self.securityQA).then(
                      function (success) {
                          self.securityQAWorking = false;
                          self.securityQA.oldPassword = '';
                          self.securityQA.securityQuestion= '';
                          self.securityQA.securityAnswer= '';
                          form.$setPristine();
                          self.securityQAMsg.successMessage = success.data.successMessage;
                      }, function (error) {
                          self.securityQAWorking = false;
                          setErrorMsg(error, self.securityQAMsg);
                      });
                }
            };

            self.twoFactorAuthMsg = {
                successMessage: '',
                errorMessage:''
            };
            self.changeTwoFactor = function(form) {
              self.twoFactorAuthMsg = {
                  successMessage: '',
                  errorMessage:''
              };

              if (self.twoFactorAuth.twoFactor !== self.master.twoFactor) {
                self.twoFactorWorking = true;
                UserService.changeTwoFactor(self.twoFactorAuth).then(
                        function (success) {
                          self.twoFactorWorking = false;
                          self.twoFactorAuth.password = '';
                          form.$setPristine();
                          if (success.data.QRCode !== undefined) {
                            self.close();
                            $location.path("/qrCode/profile/" + success.data.QRCode);
                          } else if (success.data.successMessage !== undefined) { 
                            self.master.twoFactor = false;
                            self.twoFactorAuthMsg.successMessage = success.data.successMessage;
                          }
                        }, function (error) {
                           self.twoFactorWorking = false;
                           setErrorMsg(error, self.twoFactorAuthMsg);
                });
              }
            };
            
            self.getQR = function() {
                if (self.twoFactorAuth.password === undefined || 
                    self.twoFactorAuth.password === '') {
                    self.noPassword = true;
                    return;
                }
                self.noPassword = false;
                self.qrCodeWorking = true;
                UserService.getQR(self.twoFactorAuth.password).then(
                        function (success) {
                          self.qrCodeWorking = false;
                          self.twoFactorAuth.password = '';
                          if (success.data.QRCode !== undefined) {
                            self.close();
                            $location.path("/qrCode/profile/" + success.data.QRCode);
                          } 
                        }, function (error) {
                          self.qrCodeWorking = false;
                          setErrorMsg(error, self.twoFactorAuthMsg);
                });
            };

            var setErrorMsg = function (error, msg) {
                var errorMsg = (typeof error.data.usrMsg !== 'undefined')? error.data.usrMsg : error.data.errorMsg;
                msg.errorMessage = errorMsg;
            }
            
            self.externalAccountType = function () {
                return self.user.accountType !== 'M_ACCOUNT_TYPE';
            };

            self.reset = function () {
              self.user = angular.copy(self.master);
              $scope.profileForm.$setPristine();
            };

            self.close = function () {
              $uibModalInstance.dismiss('cancel');
            };

          }]);
