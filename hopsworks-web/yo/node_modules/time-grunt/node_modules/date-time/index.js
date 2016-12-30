'use strict';
var timeZone = require('time-zone');

module.exports = function (date, opts) {
	date = date || new Date();
	opts = opts || {};

	if (opts.local) {
		// offset the date so it will return the correct value when getting the ISO string
		date = new Date(date.getTime() - (date.getTimezoneOffset() * 60000));
	}

	return date
		.toISOString()
		.replace(/T/, ' ')
		.replace(/\..+/, ' UTC' + (opts.local ? timeZone(date) : ''));
};
