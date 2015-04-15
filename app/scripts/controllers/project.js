'use strict';

angular.module('hopsWorksApp')
  .controller('ProjectCtrl', ['$modalStack', '$scope', 'growl', 'ProjectService', 'UserService', function ($modalStack, $scope, growl, ProjectService, UserService) {

    var self = this;

    self.card = {};
    self.cards = [];
    self.projectMembers = [];
    // We could instead implement a service to get all the available types but this will do it for now
    self.projectTypes = ['Cuneiform', 'Samples', 'Spark', 'Adam', 'Mapreduce', 'Yarn', 'Zeppelin'];
    self.selectionProjectTypes = [];
    self.projectName = '';
    self.projectDesc = '';

    UserService.allcards().then(
      function (success) {
        self.cards = success.data;
      }, function (error) {
        self.errorMsg = error.data.msg;
      }
    );

    $scope.$watch('projectCtrl.card.selected', function (selected) {
      if (selected !== undefined) {
        if (self.projectMembers.indexOf(selected.email) == -1) {
          self.projectMembers.push(selected.email);
        }
        self.card.selected = undefined;
      }
    });

    self.removeMember = function (member) {
      self.projectMembers.splice(self.projectMembers.indexOf(member), 1);
    };

    self.toggleTypeSelection = function toggleTypeSelection(projectType) {
      var idx = self.selectionProjectTypes.indexOf(projectType);

      if (idx > -1) {
        self.selectionProjectTypes.splice(idx, 1);
      } else {
        self.selectionProjectTypes.push(projectType);
      }
    };


    self.createProject = function () {

      $scope.newProject = {
        'description': self.projectDesc,
        'name': self.projectName,
        'status': 0,
        'types': self.selectionProjectTypes,
        'members': self.projectMembers
      };

      ProjectService.save($scope.newProject);

      $modalStack.dismissAll();

      growl.success("Successfully created project: " + self.projectName, {title: 'Success', ttl: 5000});
    };

    self.close = function(){
      $modalStack.dismissAll();
      growl.info("Closed project without saving.", {title: 'Info', ttl: 5000});
    };

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





