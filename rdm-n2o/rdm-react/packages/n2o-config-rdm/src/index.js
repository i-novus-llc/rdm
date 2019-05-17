import '@babel/polyfill';
import { authProvider } from 'n2o-auth';
import DataGrid from 'n2o-data-grid';

const config = {
  widgets: {
    DataGrid: (props) => <DataGrid
      id='DataGrid'
      fetchOnInit={true}
      minHeight={500}
      rowHeight={50}
      filterable={true}
      {...props}
    />
  },
  security: {
    authProvider,
    externalLoginUrl: '/'
  }
};

export const styles = ['n2o-data-grid/css/DataGrid.css'];

export default config;
