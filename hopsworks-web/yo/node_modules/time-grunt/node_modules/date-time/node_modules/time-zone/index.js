'use strict';
module.exports = function (date) {
	var offset = (date || new Date()).getTimezoneOffset();
	var absOffset = Math.abs(offset);
	var hours = Math.floor(absOffset / 60);
	var minutes = absOffset % 60;
	var minutesOut = minutes > 0 ? ':' + ('0' + minutes).slice(-2) : '';

	return (offset < 0 ? '+' : '-') + hours + minutesOut;
};
