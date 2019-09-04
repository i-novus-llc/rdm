import React from 'react';
import AdvancedTable from 'n2o-framework/lib/components/widgets/AdvancedTable/AdvancedTable';
import DataGridCell from "./DataGridCell";

const components = {
    body: {
        cell: DataGridCell
    }
};

function DataGrid({
    columns,
    rows,
    onFilter,
    filters,
    onResolve,
}) {
    return (
        <AdvancedTable
            columns={columns}
            data={rows}
            filters={filters}
            onFilter={onFilter}
            components={components}
            hasSelect={true}
            onResolve={onResolve}
        />
    );
}

export default DataGrid;
