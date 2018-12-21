import React, { Component } from 'react';
import N2O from 'n2o';
import DataGrid from 'n2o-data-grid';
import 'n2o-data-grid/css/DataGrid.css';
import { authProvider } from 'n2o-auth';



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

class App extends Component {
  render() {
    return (
        <N2O {...config}  />
    );
  }
}

export default App;
