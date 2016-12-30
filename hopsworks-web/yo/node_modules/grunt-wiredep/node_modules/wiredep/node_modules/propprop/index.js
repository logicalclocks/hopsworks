module.exports = function (propertyName) {
  return function (item) {
    if (propertyName in item) {
      return item[propertyName];
    }
  };
};
