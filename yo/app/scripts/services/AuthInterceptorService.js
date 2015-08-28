'use strict';

angular.module('hopsWorksApp')
        .factory('AuthInterceptorService', ['$q', '$location','growl', function ($q, $location, growl) {

            return {
              response: function (response) {
                //console.log('From server: ', response);

                // Return a promise
                return response || $q.when(response);
              },
              responseError: function (responseRejection) {
                console.log('Error in response: ', responseRejection);

                if (responseRejection.status === 403) {
                  // Access forbidden, authenticating will make no difference

                  console.log('Error in response: ', responseRejection + 'Access forbidden.');

                } else if (responseRejection.status === 401) {
                  // Authorization issue, unauthorized, login required

                  console.log('Error in response ', responseRejection + 'Login required.');

                  var url = $location.url();

                  if (url != '/login' && url != '/register' && url != '/recover') {
                    $location.url('/login');
                    $location.replace();
                  }

                } else {
                  growl.error(responseRejection.data.errorMsg, {title: 'Error', ttl: 5000});
                  console.log('Unhandled error: ', responseRejection);
                }
                return $q.reject(responseRejection);
              }
            };
          }]);
