'use strict';

angular.module('hopsWorksApp')
  .controller('ProjectCtrl', ['$scope','$location', '$routeParams', 'growl', 'ProjectService', 'UserService',
    function ($scope, $location, $routeParams, growl, ProjectService, UserService) {

    var self = this;
    self.currentProject = [];

    ProjectService.get({}, {'id': $routeParams.projectID}).$promise.then(
      function(success){
        self.currentProject = success;
      }, function(error){
        $location.path('/');
      }
    );


  }]);







/*******************************/
/* TESTING ALL CRUD OPERATIONS */
/*******************************/

// GET /api/project/
// $scope.projects = ProjectService.query();
// console.log($scope.projects);

// GET /api/project/1
// $scope.specificProject = ProjectService.get({}, {'id': 1});

// PUT /api/project/1
// $scope.specificProject.description = 'TESTING TO CHANGE VALUE';
// ProjectService.update({ id:$scope.specificProject.id }, $scope.specificProject );

// POST /api/project/
/*
 $scope.newProject = {
 'description':'Created a new project',
 'name':'TestProject',
 'status':0,
 'type':'Spark'
 }
 */

// POST /api/project/
// ProjectService.save($scope.newProject);

// DELETE /api/project/ THELATEST id
// ProjectService.delete({}, {'id': 35 });





