# grunt-filerev [![Build Status](https://secure.travis-ci.org/yeoman/grunt-filerev.svg?branch=master)](http://travis-ci.org/yeoman/grunt-filerev)

> Static asset revisioning through file content hash


## Getting Started

If you haven't used [grunt][] before, be sure to check out the [Getting Started][] guide, as it explains how to create a [gruntfile][Getting Started] as well as install and use grunt plugins. Once you're familiar with that process, install this plugin with this command:

```sh
$ npm install --save-dev grunt-filerev
```

[grunt]: http://gruntjs.com
[Getting Started]: http://gruntjs.com/getting-started


## Overview

This task will revision your files based on its contents. You should then set the files to expire far into the future for better caching and it will only update when it changes.


### Example

```js
grunt.initConfig({
  filerev: {
    options: {
      algorithm: 'md5',
      length: 8
    },
    images: {
      src: 'img/**/*.{jpg,jpeg,gif,png,webp}'
    }
  },
});
```


### Options

#### options.algorithm

Type: `string`  
Default: `'md5'`

`algorithm` is dependent on the available algorithms supported by the version of OpenSSL on the platform. Examples are `'sha1'`, `'md5'`, `'sha256'`, `'sha512'`, etc. On recent releases, `openssl list-message-digest-algorithms` will display the available digest algorithms.

#### options.length

Type: `number`  
Default: `8`

The number of characters of the file hash to prefix the file name with.

### Destination

It will overwrite the `src` files if you don't specify a `dest`:

```js
filerev: {
  images: {
    src: ['img1.png', 'img2.png'],
    dest: 'tmp'
  }
}
```

#### Summary

The task keeps track of all files created and its sources in a summary that is
exposed through the `grunt.filerev.summary` object. It can be used to replace
references to the revved files or debugging purposes. The key of the object is
the original filename, the value is the new revved path.

For a configuration like this

```js
filerev: {
  images: {
    src: ['img1.png', 'img2.png'],
    dest: 'tmp'
  }
}
```

the content `grunt.filerev.summary` could look like that:

```js
{
  'img1.png': 'tmp/img1.59bcc3ad.png',
  'img2.png': 'tmp/img2.060b1aa6.png'
}
```

#### Source Maps

The task will ensure that any source map for `.css` or `.js` file is revisioned with the same revision as the source file.

For example, `js/main.js` revisioned to `js/main.9d713a59.js` will also have `js/main.js.map` revisioned to the same hash `js/main.9d713a59.js.map`.

## License

[BSD license](http://opensource.org/licenses/bsd-license.php) and copyright Google
