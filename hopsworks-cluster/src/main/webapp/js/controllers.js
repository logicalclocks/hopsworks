/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

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