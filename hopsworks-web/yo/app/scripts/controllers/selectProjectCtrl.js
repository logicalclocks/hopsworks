/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
 */

angular.module('hopsWorksApp')
        .controller('SelectProjectCtrl', ['$uibModalInstance', 'ProjectService', 'growl', 'global', 'projectId', 'msg',
          function ($uibModalInstance, ProjectService, growl, global, projectId, msg) {

            var self = this;
            self.global = global;
            self.projectId = parseInt(projectId);
            self.msg = msg;
            self.selectedProject;
            self.projects = [];

            self.init = function () {
              if (global) {
                ProjectService.getAll().$promise.then(
                        function (success) {
                          var j=0;
                          for(var i=0;i<success.length;i++){
                              if(success[i].id !== self.projectId){
                                  self.projects[j++] =success[i];
                              }
                          }
                        }, function (error) {
                  growl.error(error.data.errorMsg, {title: 'Could not get list of Projects', ttl: 5000, referenceId: 21});
                });
              } else {
                ProjectService.query().$promise.then(
                        function (success) {
                          var j=0;
                          for(var i=0;i<success.length;i++){
                              if(success[i].id !== self.projectId){
                                  self.projects[j++] =success[i];
                              }
                          }
                        }, function (error) {
                  growl.error(error.data.errorMsg, {title: 'Could not get list of Projects', ttl: 5000, referenceId: 21});
                });
              }
            };

            self.init();

            self.selectProject = function () {
              if (self.selectedProject === undefined || self.selectedProject === "") {
                growl.error("Could not select a project", {title: 'Error', ttl: 5000, referenceId: 21});
                return;
              }

              ProjectService.getProjectInfo({projectName: self.selectedProject.name}).$promise.then(
                      function (success) {
                        $uibModalInstance.close(success);
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Error', ttl: 10000});
              });
            };

            self.close = function () {
              $uibModalInstance.dismiss('cancel');
            };
          }]);

