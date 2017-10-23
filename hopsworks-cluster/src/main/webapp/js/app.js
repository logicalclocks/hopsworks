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
            .when('/unregister', {
              templateUrl: 'partials/unregister.html',
              controller: 'UnregisterController'
            })
            .otherwise({
              redirectTo: '/'
            });
  }]);