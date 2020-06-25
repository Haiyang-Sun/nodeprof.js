import commonjs from '@rollup/plugin-commonjs';
import resolve from '@rollup/plugin-node-resolve';

export default {
  input: 'deps.js',
  output: {
    file: '../bundle.js',
    format: 'cjs',
    banner() {
      return '// DO NOT INSTRUMENT';
    }
  },
  plugins: [ commonjs(), resolve() ]
};
