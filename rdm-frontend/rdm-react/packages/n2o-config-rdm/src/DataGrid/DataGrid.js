import React from 'react';
import PropTypes from 'prop-types';
import AdvancedTable from 'n2o-framework/lib/components/widgets/AdvancedTable/AdvancedTable';
import DataGridCell from "./DataGridCell";

const components = {
    body: {
        cell: DataGridCell
    }
};

function DataGrid(props) {
    return (
        <AdvancedTable
          components={components}
          {...props}
        />
    );
}

DataGrid.propTypes = {
  hasSelect: PropTypes.bool
};

DataGrid.defaultProps = {
  hasSelect: true
};

export default DataGrid;
