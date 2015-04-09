'use strict';

angular.module('hopsWorksApp', [
  'ngAnimate',
  'ngCookies',
  'ngResource',
  'ngRoute',
  'ngSanitize',
  'ngTouch',
  'angular-md5'
])
  .config(['$routeProvider', '$httpProvider', function ($routeProvider, $httpProvider) {

    // Responseinterceptor for authentication
    $httpProvider.interceptors.push('AuthInterceptorService');

    // Requestinterceptor to transform some of the requests
    $httpProvider.interceptors.push('RequestInterceptorService');

    // Set the content type to be FORM type for all general post requests and override them explicit if needed
    $httpProvider.defaults.headers.post['Content-Type'] = 'application/x-www-form-urlencoded';


    $routeProvider
      .when('/', {
        templateUrl: 'views/main.html',
        controller: 'MainCtrl as mainCtrl',
        resolve: {
          auth: ['$q', '$location', 'AuthService',
            function ($q, $location, AuthService) {
              return AuthService.session().then(
                function (success) {
                },
                function (err) {
                  $location.path('/login');
                  $location.replace();
                  return $q.reject(err);
                });
            }]
        }
      })
      .when('/login', {
        templateUrl: 'views/login.html',
        controller: 'LoginCtrl as loginCtrl',
        resolve: {
          auth: ['$q', '$location', 'AuthService',
            function ($q, $location, AuthService) {
              return AuthService.session().then(
                function (success) {
                  $location.path('/');
                  $location.replace();
                  return $q.when(success);
                },
                function (err) {
                });
            }]
        }
      })
      .when('/register', {
        templateUrl: 'views/register.html',
        controller: 'RegCtrl as regCtrl',
        resolve: {
          auth: ['$q', '$location', 'AuthService',
            function ($q, $location, AuthService) {
              return AuthService.session().then(
                function (success) {
                  $location.path('/');
                  $location.replace();
                  return $q.when(success);
                },
                function (err) {

                });
            }]
        }
      })
      .when('/project', {
        templateUrl: 'views/project.html',
        controller: 'ProjectCtrl as projectCtrl',
        resolve: {
          auth: ['$q', '$location', 'AuthService',
            function ($q, $location, AuthService) {
              return AuthService.session().then(
                function (success) {
                },
                function (err) {
                  $location.path('/login');
                  $location.replace();
                  return $q.reject(err);
                });
            }]
        }
      })
      .otherwise({
        redirectTo: '/'
      });


  }]);
