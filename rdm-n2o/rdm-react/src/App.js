import React, { Component } from 'react';
import N2O from 'n2o/lib//N2o';


const config = {
  messages: {
    timeout: {
      error: 0,
      success: 5000,
      warning: 0,
      info: 0,
    }
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
