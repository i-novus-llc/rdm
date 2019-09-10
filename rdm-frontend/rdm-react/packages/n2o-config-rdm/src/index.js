import React from 'react';
import '@babel/polyfill';
import { authProvider } from 'n2o-auth';
import DataGrid from './DataGrid/index';

const config = {
  widgets: {
    DataGrid
  },
  security: {
    authProvider,
    externalLoginUrl: '/'
  }
};

export const styles = ['../css/DataGrid.css'];

export default config;
