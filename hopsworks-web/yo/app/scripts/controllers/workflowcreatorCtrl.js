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

'use strict';

/**
 * @ngdoc function
 * @name hopsWorksApp.controller:WorkflowcreatorCtrl
 * @description
 * # WorkflowcreatorCtrl
 * Controller of the hopsWorksApp
 */
angular.module('hopsWorksApp')
  .controller('WorkflowCreatorCtrl', [ '$routeParams', '$uibModalInstance', 'growl','WorkflowService', function ($routeParams, $uibModalInstance, growl, WorkflowService) {
      var self = this;
      var projectId = $routeParams.projectID;
      self.workflow = {"name": ""};

      self.create = function () {
          WorkflowService.create(projectId, self.workflow)
              .then(function (success) {
                      $uibModalInstance.close(success);
                  },
                  function (error) {
                      growl.error(error.data.errorMsg, {title: 'Error', ttl: 15000});
                  });
      };

      self.close = function () {
          $uibModalInstance.dismiss('cancel');
      };
  }]);
