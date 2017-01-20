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
