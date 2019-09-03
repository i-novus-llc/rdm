import React from 'react';
import ReactDOM from 'react-dom';
import N2O from 'n2o-framework';
import rdmConfig from 'n2o-config-rdm';

import 'n2o-framework/dist/n2o.css';
import 'n2o-data-grid/css/DataGrid.css';

ReactDOM.render(<N2O {...rdmConfig} />, document.getElementById('root'));
