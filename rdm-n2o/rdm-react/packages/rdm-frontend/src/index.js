import React from 'react';
import ReactDOM from 'react-dom';
import { each } from 'lodash';
import { styles } from 'n2o-config-rdm';
import App from './App';

import 'n2o/dist/n2o.css';
import './index.css';

each( styles, function( module ){
  require( module );
});

ReactDOM.render(<App />, document.getElementById('root'));