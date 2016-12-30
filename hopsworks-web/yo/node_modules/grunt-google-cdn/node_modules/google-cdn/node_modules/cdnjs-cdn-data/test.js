/*global describe, it, beforeEach */

'use strict';
var assert = require('chai').assert;

describe('cdnjs-cdn-data', function () {
  beforeEach(function () {
    this.data = require('./index');
  });

  it('should include jquery 2.0.3', function () {
    assert.include(this.data.jquery.versions, '2.0.3');
  });

  it('should build jquery 2.0.3 url', function () {
    assert.equal(this.data.jquery.url('2.0.3'), '//cdnjs.cloudflare.com/ajax/libs/jquery/2.0.3/jquery.min.js');
  });
});
