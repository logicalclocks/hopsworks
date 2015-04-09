'use strict';

angular.module('hopsWorksApp')
  .controller('MainCtrl', ['$scope', '$location', 'AuthService', 'ProjectService', function ($scope, $location, AuthService, ProjectService) {

    var self = this;

    self.authService = AuthService;

    self.isLoggedIn = AuthService.isLoggedIn;

    self.logout = function () {
      console.log(self.user);
      AuthService.logout(self.user).then(function (success) {
        $location.url('/login');
      }, function (error) {
        self.errorMessage = error.data.msg;
      });
    };

    // Load all projects
    self.projects = ProjectService.query();






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




  }]);
