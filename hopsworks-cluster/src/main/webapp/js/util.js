function getPort() {
    var port = Number(location.port);
  if (port === 'undefined' || port === 0) {
    port = 80;
    if (location.protocol === 'https:') {
      port = 443;
    }
  }

  if (port === 3333 || port === 9000) {
    port = 8080;
  }
  return port;
};

function getPathname() {
  return "/hopsworks-cluster";
}

function getApiPath() {
  return getPathname() + "/api";
}

function getApiLocationBase() {
  return location.protocol + "//" + location.hostname +":" + getPort() + getApiPath();
};





