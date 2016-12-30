# google-cdn-data [![Build Status](https://travis-ci.org/shahata/google-cdn-data.png)](https://travis-ci.org/shahata/google-cdn-data)

> [Google CDN](https://developers.google.com/speed/libraries/devguide) data for [google-cdn](https://github.com/passy/google-cdn).

This module makes it easy to replace references to your bower resources in your
app with links to the libraries on the Google CDN.

## Getting started

Install: `npm install --save-dev google-cdn google-cdn-data`

### Example

*bower.json*:

```json
{
  "name": "my-awesome-app",
  "dependencies": {
    "jquery": "~2.0.0"
  }
}
```

```javascript
var googlecdn = require('google-cdn');

var bowerConfig = loadJSON('bower.json');
var markup = '<script src="bower_components/jquery/jquery.js"></script>';
googlecdn(markup, bowerConfig, {cdn: require('google-cdn-data')}, function (err, result) {
  if (err) {
    throw err;
  }

  assert.equal(result,
    '<script src="//ajax.googleapis.com/ajax/libs/jquery/2.0.2/jquery.min.js"></script>');
});
```

## License

MIT
