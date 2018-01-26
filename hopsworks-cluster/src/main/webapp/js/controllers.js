'use strict';

var controllers = angular.module("controllers", []);

controllers.controller("HomeController", ['$scope', function ($scope) {
    $scope.title = 'Cluster management';
  }]);

controllers.controller("RegisterController", ['$scope', 'ClusterService', function ($scope, ClusterService) {
    $scope.title = 'Register your cluster';
    $scope.working = false;
    $scope.successMessage = '';
    $scope.errorMessage = '';
    $scope.newUser = {organizationName: '',
                      organizationalUnitName: '',
                      email: '',
                      chosenPassword: '',
                      repeatedPassword: '',
                      tos: ''};//Terms of service
    $scope.validationKey = '';
    $scope.register = function () {
      if ($scope.newUser.commonName === '' || $scope.newUser.email === '' ||
          $scope.newUser.organizationName === '' || $scope.newUser.organizationalUnitName === '' ||
          $scope.newUser.chosenPassword === '' || $scope.newUser.repeatedPassword === '' || 
          $scope.newUser.chosenPassword !== $scope.newUser.repeatedPassword) {
        return;
      }
      $scope.successMessage = '';
      $scope.errorMessage = '';
      $scope.working = true;
      ClusterService.register($scope.newUser).then(
        function (success) {
          $scope.successMessage = success.data.successMessage;
          $scope.working = false;
        }, function (error) {
          $scope.errorMessage = error.data.errorMsg;
          $scope.working = false;
      });
    };
    $scope.registerCluster = function () {
      if ($scope.newUser.commonName === '' || $scope.newUser.email === '' ||
          $scope.newUser.organizationName === '' || $scope.newUser.organizationalUnitName === '' ||
          $scope.newUser.chosenPassword === '') {
        return;
      }
      $scope.successMessage = '';
      $scope.errorMessage = '';
      $scope.working = true;
      ClusterService.registerCluster($scope.newUser).then(
        function (success) {
          $scope.successMessage = success.data.successMessage;
          $scope.working = false;
        }, function (error) {
          $scope.errorMessage = error.data.errorMsg;
          $scope.working = false;
      });
    };
    $scope.confirmRegister = function () {
      if ($scope.validationKey === '' || $scope.validationKey.length < 64) {
        $scope.errorMessage = 'Key not set or too short.';
        return;
      }
      $scope.successMessage = '';
      $scope.errorMessage = '';
      $scope.working = true;
      ClusterService.confirmRegister($scope.validationKey).then(
        function (success) {
          $scope.successMessage = success.data.successMessage;
          $scope.working = false;
        }, function (error) {
          $scope.errorMessage = error.data.errorMsg;
          $scope.working = false;
      });
    };

    $scope.$watch('newUser.email', function (newValue, oldValue) {
      var orgAndUnit;
      var index;
      if (newValue !== undefined) {
        index = newValue.indexOf("@");
        if (index !== -1) {
          orgAndUnit = newValue.split("@");
          if ($scope.registerForm.org_name.$pristine) {
            $scope.newUser.organizationName = orgAndUnit[1];
          }
          if ($scope.registerForm.org_unit_name.$pristine) {
            $scope.newUser.organizationalUnitName = orgAndUnit[0];
          }
        }
      }
    }, true);

  }]);

controllers.controller("UnregisterController", ['$scope', 'ClusterService', function ($scope, ClusterService) {
    $scope.title = 'Unregister your cluster';
    $scope.working = false;
    $scope.successMessage = '';
    $scope.errorMessage = '';
    $scope.user = {email: '',
                   organizationName: '',
                   organizationalUnitName: '',
                   chosenPassword: ''};
    $scope.validationKey = '';
    $scope.unregister = function () {
      if ($scope.user.email === '' || $scope.user.organizationName === '' || 
          $scope.user.organizationalUnitName === '' || $scope.user.chosenPassword === '') {
        return;
      }
      $scope.successMessage = '';
      $scope.errorMessage = '';
      $scope.working = true;
      ClusterService.unregister($scope.user).then(
        function (success) {
          $scope.successMessage = success.data.successMessage;
          $scope.working = false;
        }, function (error) {
          $scope.errorMessage = error.data.errorMsg;
          $scope.working = false;
      });
    };

    $scope.confirmUnregister = function () {
      if ($scope.validationKey === '' || $scope.validationKey.length < 64) {
        $scope.errorMessage = 'Key not set or too short.';
        return;
      }
      $scope.successMessage = '';
      $scope.errorMessage = '';
      $scope.working = true;
      ClusterService.confirmUnregister($scope.validationKey).then(
        function (success) {
          $scope.successMessage = success.data.successMessage;
          $scope.working = false;
        }, function (error) {
          $scope.errorMessage = error.data.errorMsg;
          $scope.working = false;
      });
    };
  }]);

controllers.controller("RegisteredClusters", ['$scope', 'ClusterService', function ($scope, ClusterService) {
    $scope.title = 'Registered clusters';
    $scope.working = false;
    $scope.successMessage = '';
    $scope.errorMessage = '';
    $scope.user = {email: '',
                   chosenPassword: ''};
    $scope.clusters = undefined;
    
    $scope.getClusters = function () {
      if ($scope.user.chosenPassword === '' || $scope.user.email === '') {
        return;
      }
      $scope.successMessage = '';
      $scope.errorMessage = '';
      $scope.working = true;
      ClusterService.getAllClusters($scope.user).then(
        function (success) {
          $scope.clusters = success.data;
          $scope.working = false;
        }, function (error) {
          $scope.clusters = [];
          $scope.errorMessage = error.data.errorMsg;
          $scope.working = false;
      });
    };
    
    $scope.certStatus = function (serial) {
      return serial === undefined? "Certificate not yet signed." : "Certificate signed.";
    };
    
  }]);