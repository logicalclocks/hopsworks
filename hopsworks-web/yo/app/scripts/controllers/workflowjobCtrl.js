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
 * @name hopsWorksApp.controller:WorkflowjobCtrl
 * @description
 * # WorkflowjobCtrl
 * Controller of the hopsWorksApp
 */
angular.module('hopsWorksApp')
  .controller('WorkflowJobCtrl',[ '$routeParams', '$location', 'growl','WorkflowJobService',
      function ($routeParams, $location, growl, WorkflowJobService) {
        var self = this;
        self.jobs = [];
        var projectId = $routeParams.projectID;
        var workflowId = $routeParams.workflowID;
        var executionId = $routeParams.executionID;
        var jobId = $routeParams.jobID;

        var index = function(){
            WorkflowJobService.index(projectId, workflowId, executionId).then(function(success){
                console.log(success);
                self.jobs = success.data;
            },function (error) {
                growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000})
            })
        }
        if(executionId) index();

        self.goToShow = function (id) {
            $location.path('project/' + projectId + '/workflows/' + workflowId + '/executions/' + executionId + '/jobs/' + id);
        }

        if(jobId){
            window.workflows.image('#image');
        }
}]);
