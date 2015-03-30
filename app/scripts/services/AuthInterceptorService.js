'use strict';

angular.module('hopsWorksApp')
  .factory('AuthInterceptorService', ['$q', '$location', function ($q, $location) {
    return {
      response: function (response) {
        console.log('Response from server', response);
        // Return a promise
        return response || $q.when(response);
      },
      responseError: function (responseRejection) {
        console.log('Error in response ', responseRejection);
        if (responseRejection.status === 403) {
          // Access forbidden, authenticating will make no difference
          console.log('Error in response ', responseRejection + 'Access forbidden, authenticating will make no difference');
        } else if (responseRejection.status === 401) {
          // Authorization issue, unauthorized, login required
          console.log('Error in response ', responseRejection + 'Authorization issue, unauthorized, login required');
          $location.url('/login');
          $location.replace();
        }
        return $q.reject(responseRejection);
      }
    };
  }]);
