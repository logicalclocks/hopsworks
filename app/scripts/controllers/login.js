'use strict';

angular.module('hopsWorksApp')
  .controller('LoginCtrl', ['$scope', '$location', '$http', 'AuthService', function ($scope, $location, $http, AuthService) {

    var self = this;
    self.user = {username: '', password: ''};

    self.login = function () {
      AuthService.login(self.user).then(function (success) {
        $location.path('/');
      }, function (error) {
        self.errorMessage = error.data.msg;
      });
    };


  }]);










/*
 // those are the parameters that we are passing by
 $scope.user = {username: '', password: ''};
 $scope.isLoggedIn = false;
 $scope.message = '';
 $scope.projectName = '';

 // So that we dont have to define type every time
 $http.defaults.headers.post['Content-Type'] = 'application/x-www-form-urlencoded';

 $scope.login = function () {

 $http.post(
 'http://localhost:8080/hopsworks/webresources/auth/login',
 'email=' + $scope.user.username + '&' + 'password=' + $scope.user.password
 )
 .success(function (data) {
 console.log('You are now logged in.. Great.. Really');
 console.log(data);
 $location.url('/home');

 // Save the auth_token to session
 $cookies.sessionID = data.sessionID;

 // Toggle the ng-show
 $scope.isLoggedIn = true;
 //$scope.$apply();
 })
 .error(function (data, headers) {
 console.log('Error: Sorry, you are not welcome here...');
 console.log(data);
 });

 $scope.user.password = '';
 };



 $scope.logout = function () {

 $http.post(
 'http://193.10.67.43:8080/HopsWorks/rest-api/auth/logout/',
 null, // Payload empty this time
 {headers: {'sessionID': $cookies.sessionID}} // Send the auth_token in header
 )
 .success(function (data) {
 console.log('Success, logged out the user');
 console.log(data);

 // Delete the stored auth token
 delete $cookies['sessionID'];
 console.log('RemoveSession, Removes sessionID...');

 // Toggle the ng-show when logged out
 $scope.isLoggedIn = false;
 //$scope.$apply();
 })
 .error(function () {
 console.log('Error: Could not logout');
 });
 };


 $scope.doPostMethod = function () {
 $http.post('http://localhost:8080/HopsWorks/rest-api/auth/postMethod/',
 null, // Payload empty this time
 {headers: {'sessionID': $cookies.sessionID,
 'project': $scope.projectName}
 }
 )
 .success(function (data) {
 console.log('postMethod ran successfully');
 $scope.message = data.message;
 })
 .error(function (data) {
 console.log('Error: In postMethod');
 $scope.message = 'Error: In postMethod';
 });
 };


 $scope.doGetMethod = function () {
 $http.get('http://localhost:8080/hopsworks/webresources/auth/ping/',
 {headers: {'SESSIONID': $cookies.sessionID}} // Send the auth_token in header
 )
 .success(function (data) {
 console.log('getMethod ran successfully');
 console.log(data);
 //$scope.message = data;
 })
 .error(function (data) {
 console.log('Error: In getMethod');
 console.log(data);
 //$scope.message = 'Error: In getMethod';
 });
 };


 */





