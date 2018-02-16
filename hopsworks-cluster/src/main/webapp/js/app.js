/*
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
 *
 */

'use strict';


var app = angular.module('app', [
  'ngRoute',
  'ui.bootstrap',
  'directives',
  'services',
  'controllers'
]);


app.config(['$routeProvider',
  function ($routeProvider) {
    $routeProvider
            .when('/', {
              templateUrl: 'partials/home.html',
              controller: 'HomeController'
            })
            .when('/register', {
              templateUrl: 'partials/register.html',
              controller: 'RegisterController'
            })
            .when('/registerCluster', {
              templateUrl: 'partials/registerCluster.html',
              controller: 'RegisterController'
            })
            .when('/unregister', {
              templateUrl: 'partials/unregister.html',
              controller: 'UnregisterController'
            })
            .when('/view', {
              templateUrl: 'partials/viewRegisteredClusters.html',
              controller: 'RegisteredClusters'
            })
            .otherwise({
              redirectTo: '/'
            });
  }]);
