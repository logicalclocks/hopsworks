var regex = {
  block: {
    '//': /(([ \t]*)\/\/\s*bower:*(\S*))(\n|\r|.)*?(\/\/\s*endbower)/gi
  }
};

module.exports = {
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
  
  js: {
    block: regex.block['//'],
    detect: {
      js: /['"]([^'"]+\.js)['"],?/gi,
      css: /['"]([^'"]+\.js)['"],?/gi
    },
    replace: {
      js: '"{{filePath}}",',
      css: '"{{filePath}}",'
    }
  },

  jade: {
    block: /(([ \t]*)\/\/-?\s*bower:*(\S*))(\n|\r|.)*?(\/\/-?\s*endbower)/gi,
    detect: {
      js: /script\(.*src=['"]([^'"]+)/gi,
      css: /link\(.*href=['"]([^'"]+)/gi
    },
    replace: {
      js: 'script(src=\'{{filePath}}\')',
      css: 'link(rel=\'stylesheet\', href=\'{{filePath}}\')'
    }
  },

  slim: {
    block: /(([ \t]*)\/!?\s*bower:(\S*))(\n|\r|.)*?(\/!?\s*endbower)/gi,
    detect: {
      js: /script.*src=['"]([^'"]+)/gi,
      css: /link.*href=['"]([^'"]+)/gi
    },
    replace: {
      js: 'script src=\'{{filePath}}\'',
      css: 'link rel=\'stylesheet\' href=\'{{filePath}}\''
    }
  },

  less: {
    block: regex.block['//'],
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
    block: regex.block['//'],
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
    block: regex.block['//'],
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
    block: regex.block['//'],
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
  },

  haml: {
    block: /(([ \t]*)-#\s*bower:*(\S*))(\n|\r|.)*?(-#\s*endbower)/gi,
    detect: {
      js: /\%script\{.*src:['"]([^'"]+)/gi,
      css: /\%link\{.*href:['"]([^'"]+)/gi
    },
    replace: {
      js: '%script{src:\'{{filePath}}\'}',
      css: '%link{rel:\'stylesheet\', href:\'{{filePath}}\'}'
    }
  }
};


module.exports['default'] = module.exports.html;
module.exports.htm = module.exports.html;
module.exports.yml = module.exports.yaml;
