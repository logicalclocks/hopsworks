'use strict';

/**
 * Returns a set/get style internal storage bucket.
 *
 * @return {object} the API to set and retrieve data
 */
module.exports.createStore = function () {
  var bucket = {};

  /**
   * Sets a property on the store, with the given value.
   *
   * @param  {string} property  an identifier for the data
   * @param  {*}      value     the value of the data being stored
   * @return {function} the set function itself to allow chaining
   */
  var set = function (property, value) {
    bucket[property] = value;
    return set;
  };

  /**
   * Returns the store item asked for, otherwise all of the items.
   *
   * @param  {string|undefined} property  the property being requested
   * @return {*} the store item that was matched
   */
  var get = function (property) {
    if (!property) {
      return bucket;
    }

    return bucket[property];
  };

  return {
    set: set,
    get: get
  };
};
