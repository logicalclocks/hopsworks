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
              get: function (key) {
                try {
                  if ($window.Storage) {
                    var retval = $window.localStorage.getItem(key);
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