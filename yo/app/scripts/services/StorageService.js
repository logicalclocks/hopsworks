'use strict';
/**
 * Allows storage and fetching of items.
 */
angular.module('hopsWorksApp')
  .factory('StorageService', ['$window', function($window) {
      return {
        clear: function() {
          try {
            if ($window.Storage) {
              $window.localStorage.clear();
              return true;
            }
          } catch (error) {
            console.error(error, error.message);
          }
          return false;
        },
        store: function(key, value) {
          try {
            if ($window.Storage) {
              if (value === null) {
                $window.localStorage.setItem(key, value);
              } else {
                $window.localStorage.setItem(key, $window.JSON.stringify(value));
              }
              return true;
            } else {
              return false;
            }
          } catch (error) {
            console.error(error, error.message);
          }
          return false;
        },
        recover: function(key) {
          try {
            if ($window.Storage) {
              var retval = $window.localStorage.getItem(key);
              $window.localStorage.removeItem(key);
              if(retval){
                return $window.JSON.parse(retval);
              }else{
                return false;
              }
            } else {
              return false;
            }
          } catch (error) {
            console.error(error, error.message);
          }
          return false;
        }
      }
    }])