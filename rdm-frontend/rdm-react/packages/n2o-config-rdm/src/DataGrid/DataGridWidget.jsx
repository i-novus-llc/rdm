import React, { useMemo } from 'react'
import { useSelector } from "react-redux";
import { AdvancedTableWidget } from "n2o-framework/lib/components/widgets/AdvancedTable";
import { dataSourceModelByPrefixSelector } from "n2o-framework/lib/ducks/datasource/selectors";
import { ModelPrefix } from "n2o-framework/lib/core/datasource/const";

import { getColumnsFromDatasource, getDataFromDatasource } from "./utils";
import DataGridCell from "./DataGridCell";


function DataGridWidget(props) {
  const {
    table
  } = props
  const { datasource } = props
  const datasourceModel = useSelector(dataSourceModelByPrefixSelector(datasource, ModelPrefix.source))
  const tableConfig = useMemo(() => {
    const columns = getColumnsFromDatasource(datasourceModel)

    return ({
      ...table,
      body: {
        cells: columns.body
      },
      header: {
        cells: columns.header
      }
    })
  }, [datasourceModel, table])

  return (
    <AdvancedTableWidget
      {...props}
      className="rdm-data-grid"
      dataMapper={getDataFromDatasource}
      table={tableConfig}
      components={{
        CellContainer: DataGridCell
      }}
    />
  );
}

DataGridWidget.displayName = '@rdm/DataGridWidget'

export default DataGridWidget
