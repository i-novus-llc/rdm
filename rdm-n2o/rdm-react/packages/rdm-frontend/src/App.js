import React, { Component } from 'react';
import N2O from 'n2o';
import config from 'n2o-config-rdm';

class App extends Component {
  render() {
    return (
        <N2O {...config}  />
    );
  }
}

export default App;
