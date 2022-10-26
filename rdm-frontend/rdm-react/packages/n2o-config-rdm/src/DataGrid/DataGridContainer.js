import React from 'react'

import { useColumns } from './hooks/useColumns'
import { useData } from './hooks/useData'
import { useFilters } from './hooks/useFilters'
import DataGrid from './DataGrid'

export function DataGridContainer(props) {
    const columns = useColumns(props)
    const data = useData(props)
    const { filters, onFilter } = useFilters(props)

    return (
        <DataGrid
            columns={columns}
            data={data}
            filters={filters}
            onFilter={onFilter}
            {...props.table}
        />
    );
}
