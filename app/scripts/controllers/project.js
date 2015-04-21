'use strict';

angular.module('hopsWorksApp')
  .controller('ProjectCtrl', ['$scope', '$modalStack', '$location', '$routeParams', 'growl', 'ProjectService', 'ModalService',
    function ($scope, $modalStack, $location, $routeParams, growl, ProjectService, ModalService) {

      var self = this;
      self.currentProject = [];

      self.card = {};
      self.cards = [];
      self.projectMembers = [];

      // We could instead implement a service to get all the available types but this will do it for now
      self.projectTypes = ['CUNEIFORM', 'SPARK', 'ADAM', 'MAPREDUCE', 'ZEPPELIN'];
      self.selectionProjectTypes = [];


      ProjectService.get({}, {'id': $routeParams.projectID}).$promise.then(
        function (success) {
          self.currentProject = success;

          self.currentProject.projectTypeCollection.forEach(function(entry) {
            self.selectionProjectTypes.push(entry.type);
          });

        }, function (error) {
          $location.path('/');
        }
      );


      self.exists = function (item) {
        return self.selectionProjectTypes.indexOf(item) > -1;
      };


      self.toggle = function toggle(item) {
        var idx = self.selectionProjectTypes.indexOf(item);

        if (idx > -1) self.selectionProjectTypes.splice(idx, 1);
        else self.selectionProjectTypes.push(item);

        console.log(self.selectionProjectTypes);
      };



      self.projectSettingModal = function () {
        ModalService.projectSettings('md', 'Project settings', '').then(
          function (success) {
            growl.success("Successfully saved project: " + success.name, {title: 'Success', ttl: 5000});
          }, function () {
            growl.success("Successfully saved project.", {title: 'Success', ttl: 5000});
          });
      };


      self.saveProject = function () {
        $scope.newProject = {
          'description': self.currentProject.description,
          'name': self.currentProject.name,
          'status': 0,
          'types': self.selectionProjectTypes
        };

        ProjectService.update({id:self.currentProject.id}, $scope.newProject).$promise.then(
          function(success){
            $modalStack.dismissAll();
          }, function(error){
            console.log('Error: ' + error)
          }
        );

      };


      self.close = function () {
        $modalStack.dismissAll();
      };


      $scope.showHamburger = $location.path().indexOf("project") > -1;

        self.items = [];
        for (var i=0; i < 100; i++){
            self.items.push(i);
        }







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





