# wiredep [![npm](https://badge.fury.io/js/wiredep.svg)](http://badge.fury.io/js/wiredep) [![Build Status](https://travis-ci.org/taptapship/wiredep.svg?branch=master)](https://travis-ci.org/taptapship/wiredep)
> Wire [Bower](http://bower.io) dependencies to your source code.

## Getting Started
Install the module with [npm](https://npmjs.org):

```bash
$ npm install --save wiredep
```

Install your dependencies (if you haven't already):

```bash
$ bower install --save jquery
```

Insert placeholders in your code where your dependencies will be injected:

```html
<html>
<head>
  <!-- bower:css -->
  <!-- endbower -->
</head>
<body>
  <!-- bower:js -->
  <!-- endbower -->
</body>
</html>
```

Let `wiredep` work its magic:

```bash
$ node
> require('wiredep')({ src: 'index.html' });

index.html modified.
{ packages:
   { jquery:
      { main: [Object],
        type: [Object],
        name: 'jquery',
        dependencies: {} } },
  js: [ 'bower_components/jquery/dist/jquery.js' ] }
```


```html
<html>
<head>
  <!-- bower:css -->
  <!-- endbower -->
</head>
<body>
  <!-- bower:js -->
  <script src="bower_components/jquery/dist/jquery.js"></script>
  <!-- endbower -->
</body>
</html>
```

## How it Works
Installing a Bower package with `--save` will add the package as a `dependency` in your project's `bower.json` file. This library reads that file, then reads the `bower.json` files for each of those dependencies. Based on these connections, it determines the order your scripts must be included before injecting them between placeholders in your source code.

## What can go wrong?

  - A Bower package may not properly list its `dependencies` in its bower.json file.

  - A Bower package may not specify a `main` property in its bower.json file.

In both of these cases, it is most helpful to send a PR to the offending repository with a solution. This isn't just a fix for wiredep, but for other tools which conform to the Bower specification. Most often it's just an author's oversight, so they will welcome the contribution and clarity.

If that solution doesn't work, you can get around these problems by [overriding properties](#bower-overrides).

## Build Chain Integration

### [gulp.js](http://gulpjs.com/)

wiredep works with [streams](https://github.com/substack/stream-handbook) and integrates with gulp.js out of the box:

```js
var wiredep = require('wiredep').stream;

gulp.task('bower', function () {
  gulp.src('./src/footer.html')
    .pipe(wiredep({
      optional: 'configuration',
      goes: 'here'
    }))
    .pipe(gulp.dest('./dest'));
});
```

### [Grunt](http://gruntjs.com)

See [`grunt-wiredep`](https://github.com/stephenplusplus/grunt-wiredep).


## Programmatic Access
You can run `wiredep` without manipulating any files.

```js
require('wiredep')();
```

...returns...
```js
{
  js: [
    'paths/to/your/js/files.js',
    'in/their/order/of/dependency.js'
  ],
  css: [
    'paths/to/your/css/files.css'
  ],
  // etc.
}
```


## Command Line
** wiredep-cli has been split into its own module. In a future release it will not be included in this package anymore **

Install [wiredep-cli](https://github.com/taptapship/wiredep-cli) to use the CLI.

```sh
$ npm install -g wiredep-cli
```

## Configuration

```js
require('wiredep')({
  directory: 'the directory of your Bower packages.', // default: '.bowerrc'.directory || bower_components
  bowerJson: 'your bower.json file contents.',        // default: require('./bower.json')
  src: ['filepaths', 'and/even/globs/*.html', 'to take', 'control of.'],

  // ----- Advanced Configuration -----
  // All of the below settings are for advanced configuration, to
  // give your project support for additional file types and more
  // control.
  //
  // Out of the box, wiredep will handle HTML files just fine for
  // JavaScript and CSS injection.

  cwd: 'path/to/where/we/are/pretending/to/be',

  dependencies: true,    // default: true
  devDependencies: true, // default: false
  includeSelf: true,     // default: false

  exclude: [ /jquery/, 'bower_components/modernizr/modernizr.js' ],

  ignorePath: /string or regexp to ignore from the injected filepath/,

  overrides: {
    // see `Bower Overrides` section below.
    //
    // This inline object offers another way to define your overrides if
    // modifying your project's `bower.json` isn't an option.
  },

  onError: function(err) {
    // If not overridden, an error will throw.

    // err = Error object.
    // err.code can be:
    //   - "PKG_NOT_INSTALLED" (a Bower package was not found)
    //   - "BOWER_COMPONENTS_MISSING" (cannot find the `bower_components` directory)
  },

  onFileUpdated: function(filePath) {
    // filePath = 'name-of-file-that-was-updated'
  },

  onPathInjected: function(fileObject) {
    // fileObject.block = 'type-of-wiredep-block' ('js', 'css', etc)
    // fileObject.file = 'name-of-file-that-was-updated'
    // fileObject.path = 'path-to-file-that-was-injected'
  },

  onMainNotFound: function(pkg) {
    // pkg = 'name-of-bower-package-without-main'
  },

  fileTypes: {
    fileExtension: {
      block: /match the beginning-to-end of a bower block in this type of file/,
      detect: {
        typeOfBowerFile: /match the way this type of file is included/
      },
      replace: {
        typeOfBowerFile: '<format for this {{filePath}} to be injected>',
        anotherTypeOfBowerFile: function (filePath) {
          return '<script class="random-' + Math.random() + '" src="' + filePath + '"></script>';
        }
      }
    },

    // defaults:
    html: {
      block: /(([ \t]*)<!--\s*bower:*(\S*)\s*-->)(\n|\r|.)*?(<!--\s*endbower\s*-->)/gi,
      detect: {
        js: /<script.*src=['"]([^'"]+)/gi,
        css: /<link.*href=['"]([^'"]+)/gi
      },
      replace: {
        js: '<script src="{{filePath}}"></script>',
        css: '<link rel="stylesheet" href="{{filePath}}" />'
      }
    },

    jade: {
      block: /(([ \t]*)\/\/\s*bower:*(\S*))(\n|\r|.)*?(\/\/\s*endbower)/gi,
      detect: {
        js: /script\(.*src=['"]([^'"]+)/gi,
        css: /link\(.*href=['"]([^'"]+)/gi
      },
      replace: {
        js: 'script(src=\'{{filePath}}\')',
        css: 'link(rel=\'stylesheet\', href=\'{{filePath}}\')'
      }
    },

    less: {
      block: /(([ \t]*)\/\/\s*bower:*(\S*))(\n|\r|.)*?(\/\/\s*endbower)/gi,
      detect: {
        css: /@import\s['"](.+css)['"]/gi,
        less: /@import\s['"](.+less)['"]/gi
      },
      replace: {
        css: '@import "{{filePath}}";',
        less: '@import "{{filePath}}";'
      }
    },

    sass: {
      block: /(([ \t]*)\/\/\s*bower:*(\S*))(\n|\r|.)*?(\/\/\s*endbower)/gi,
      detect: {
        css: /@import\s(.+css)/gi,
        sass: /@import\s(.+sass)/gi,
        scss: /@import\s(.+scss)/gi
      },
      replace: {
        css: '@import {{filePath}}',
        sass: '@import {{filePath}}',
        scss: '@import {{filePath}}'
      }
    },

    scss: {
      block: /(([ \t]*)\/\/\s*bower:*(\S*))(\n|\r|.)*?(\/\/\s*endbower)/gi,
      detect: {
        css: /@import\s['"](.+css)['"]/gi,
        sass: /@import\s['"](.+sass)['"]/gi,
        scss: /@import\s['"](.+scss)['"]/gi
      },
      replace: {
        css: '@import "{{filePath}}";',
        sass: '@import "{{filePath}}";',
        scss: '@import "{{filePath}}";'
      }
    },

    styl: {
      block: /(([ \t]*)\/\/\s*bower:*(\S*))(\n|\r|.)*?(\/\/\s*endbower)/gi,
      detect: {
        css: /@import\s['"](.+css)['"]/gi,
        styl: /@import\s['"](.+styl)['"]/gi
      },
      replace: {
        css: '@import "{{filePath}}"',
        styl: '@import "{{filePath}}"'
      }
    },

    yaml: {
      block: /(([ \t]*)#\s*bower:*(\S*))(\n|\r|.)*?(#\s*endbower)/gi,
      detect: {
        js: /-\s(.+js)/gi,
        css: /-\s(.+css)/gi
      },
      replace: {
        js: '- {{filePath}}',
        css: '- {{filePath}}'
      }
    }
```


## Bower Overrides
To override a property, or lack of, in one of your dependency's `bower.json` file, you may specify an `overrides` object in your own `bower.json`.

As an example, this is what your `bower.json` may look like if you wanted to override `package-without-main`'s `main` file (the path is relative to your dependency's folder):

```js
{
  ...
  "dependencies": {
    "package-without-main": "1.0.0"
  },
  "overrides": {
    "package-without-main": {
      "main": "dist/package-without-main.js"
    }
  }
}
```

If the project has multiple files, such as a javascript and a css file, `main` can be an array, as such:

```js
{
  ...
  "dependencies": {
    "package-without-main": "1.0.0"
  },
  "overrides": {
    "package-without-main": {
      "main": ["dist/package-without-main.css", "dist/package-without-main.js"]
    }
  }
}
```

## Contributing
In lieu of a formal styleguide, take care to maintain the existing coding style. Add unit tests for any new or changed functionality. Lint and test your code using `npm test`.


## License
Copyright (c) 2014 Stephen Sawchuk. Licensed under the MIT license.
