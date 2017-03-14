'use strict';
/**
 * Allows storage and fetching of items from HTML-5 local storage. 
 * Used, for example, to store and restore 'New Job' state when you navigate away from and back to Jobs.
 */
angular.module('hopsWorksApp')
        .factory('StorageService', ['$window', function ($window) {
            return {
              clear: function () {
                try {
                  if ($window.Storage) {
                    $window.localStorage.clear();
                    console.log("LocalStorage: cleaning local storage");
                    return true;
                  }
                } catch (error) {
                  console.error(error, error.message);
                }
                return false;
              },
              store: function (key, value) {
                try {
                  if ($window.Storage) {
                    if (value === null) {
                      $window.localStorage.setItem(key, value);
//                      console.log("LocalStorage: storing key: " + key);
                    } else {
                      $window.localStorage.setItem(key, $window.JSON.stringify(value));
//                      console.log("LocalStorage: storing key/value: " + key + " -- " + value);
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
              contains: function (key) {
                try {
                  if ($window.Storage) {
                    var retval = $window.localStorage.getItem(key);
                    if (retval) {
                      return true;
                    } else {
                      return false;
                    }
                  } else {
                    return false;
                  }
                } catch (error) {
                  console.error(error, error.message);
                }
                return false;
              },
              recover: function (key) {
                try {
                  if ($window.Storage) {
                    var retval = $window.localStorage.getItem(key);
//                    console.log("LocalStorage: retrieved for key: " + key + " --- " + retval);
                    $window.localStorage.removeItem(key);
                    if (retval) {
                      return $window.JSON.parse(retval);
                    } else {
                      return false;
                    }
                  } else {
                    return false;
                  }
                } catch (error) {
                  console.error(error, error.message);
                }
                return false;
              },
              remove: function (key) {
                try {
                  if ($window.Storage) {
                    $window.localStorage.removeItem(key);
//                    console.log("LocalStorage: removing key from local storage: " + key);
                    return true;
                  } else {
                    return false;
                  }
                } catch (error) {
                  console.error(error, error.message);
                }
                return false;
              }
            };
          }]);