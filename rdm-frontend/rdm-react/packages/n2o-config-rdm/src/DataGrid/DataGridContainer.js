import React from 'react'
import AdvancedTable from "n2o-framework/lib/components/widgets/AdvancedTable/AdvancedTable";

import { useColumns } from './hooks/useColumns'
import { useData } from './hooks/useData'
import { useFilters } from './hooks/useFilters'
import DataGridCell from "./DataGridCell";

const components = {
  body: {
    cell: DataGridCell
  }
};

export function DataGridContainer(props) {
    const { setResolve, models, setFilter, fetchData, onSort, sorting, id } = props
    const columns = useColumns({
      id,
      datasourceModel: models.datasource,
      sorting,
      onSort
    })
    const data = useData({
      datasourceModel: models.datasource,
      setResolve
    })
    const { filters, onFilter } = useFilters({
      filterModel: models.filter,
      setFilter,
      fetchData
    })

    return (
        <AdvancedTable
          components={components}
          columns={columns}
          data={data}
          filters={filters}
          onFilter={onFilter}
          resolveModel={models.resolve}
          {...props}
        />
    );
}
