import authProvider from 'n2o-framework/lib/core/auth/authProvider';

import DataGrid from './DataGrid';
import { widgetsReducer } from './store'

const config = {
  widgets: {
    DataGrid
  },
  customReducers: {
    widgets: widgetsReducer,
  },
  security: {
    authProvider,
    externalLoginUrl: '/'
  }
};

export const styles = ['../css/DataGrid.css'];

export default config;
