'use strict';

angular.module('hopsWorksApp')
  .controller('MainCtrl', ['$scope', 'AuthService', function ($scope, AuthService) {

    var self = this;
    self.authService = AuthService;

    // Check if the user is logged in when the application loads
    // User Service will automatically update isLoggedIn after this call finishes
    AuthService.session();
    //self.userService.logout();




    $scope.projects = [
      {name: 'HumanGenome', 'private': true},
      {name: 'DNACalc', 'private': false},
      {name: 'FinanceDepartment', 'private': true},
      {name: 'StatisticsHops', 'private': false}
    ];





  }]);

