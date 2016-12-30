# google-cdn [![Build Status](https://secure.travis-ci.org/passy/google-cdn.png?branch=master)](http://travis-ci.org/passy/google-cdn) [![Dependency Status](https://gemnasium.com/passy/google-cdn.png)](https://gemnasium.com/passy/google-cdn) [![Code Climate](https://codeclimate.com/github/passy/google-cdn.png)](https://codeclimate.com/github/passy/google-cdn)

> Grunt task for replacing refs to resources on the [Google CDN](https://developers.google.com/speed/libraries/devguide).

This module makes it easy to replace references to your bower resources in your
app with links to the libraries on the Google CDN (and some other CDNs).

## Getting started

Install: `npm install --save google-cdn`

Install CDN data module: `npm install --save google-cdn-data` (see list of more [data modules](#cdn-data-modules) below)

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

## API

### googlecdn(content, bowerJson[, options], callback)

Replaces references to libraries supported by the Google CDN in `content`.
The library versions are inferred from the `bowerJson`.

`options` is an optional object with these keys:

  - `componentsPath`: defaults to `bower_components`, the path you specify in
    your script tags to the components directory.
  - `cdn`: defaults to `require('google-cdn-data')`. CDN you want to use. Object of the following format:

  ```javascript
  {
    jquery: {
      versions: ['2.0.3', '2.0.2', '2.0.1', '2.0.0'],
      url: function (version) {
        return '//my.own.cdn/libs/jquery/' + version + '/jquery.min.js';
      }
    }
  }
  ```

## Used by

- [grunt-google-cdn](https://github.com/btford/grunt-google-cdn)
- [gulp-google-cdn](https://github.com/sindresorhus/gulp-google-cdn)

## CDN data modules

- [google-cdn-data](https://github.com/shahata/google-cdn-data)
- [cdnjs-cdn-data](https://github.com/shahata/cdnjs-cdn-data)
- [jsdelivr-cdn-data](https://github.com/shahata/jsdelivr-cdn-data)

## Debugging

You can turn on debugging by setting the `DEBUG` environment variable to
`google-cdn`. E.g.

`env DEBUG='google-cdn' grunt cdnify`

## License

BSD
