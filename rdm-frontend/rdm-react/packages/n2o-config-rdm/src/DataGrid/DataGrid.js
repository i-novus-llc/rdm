import React from 'react'
import AdvancedTable from 'n2o-framework/lib/components/widgets/AdvancedTable/AdvancedTable'

import DataGridCell from './DataGridCell'

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

export default DataGrid;
