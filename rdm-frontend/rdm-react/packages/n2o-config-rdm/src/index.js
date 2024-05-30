import authProvider from "n2o-framework/lib/core/auth/authProvider";
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

export default config;
