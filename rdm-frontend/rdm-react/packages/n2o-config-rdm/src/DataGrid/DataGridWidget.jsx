import React, { useMemo } from 'react'
import { useSelector } from "react-redux";
import { AdvancedTableWidget } from "n2o-framework/lib/components/widgets/AdvancedTable";
import { dataSourceModelByPrefixSelector } from "n2o-framework/lib/ducks/datasource/selectors";
import { ModelPrefix } from "n2o-framework/lib/core/datasource/const";

import { getColumnsFromDatasource, getDataFromDatasource } from "./utils";
import DataGridCell from "./DataGridCell";

import get from 'lodash/get'

function DataGridWidget(props) {
  console.log("DataGridWidget props")
  console.log(props)

  const {
    table
  } = props
  const { datasource } = props
  const datasourceModel = useSelector(dataSourceModelByPrefixSelector(datasource, ModelPrefix.source))
  console.log("datasourceModel")
  console.log(datasourceModel)
  console.log(get(datasourceModel, [0], {}))

  const tableConfig = useMemo(() => {
//     console.log("datasourceModel")
//     console.log(datasourceModel)
//     console.log(get(datasourceModel, [0], {}))
//     console.log(get(datasourceModel, [0, 'columnsConfig'], {}))
    const columns = getColumnsFromDatasource(datasourceModel)
    console.log("columns")
    console.log(columns)
//     console.log(columns.cells)
//     console.log(columns.headers)

    return ({
      ...table,
      body: {
        cells: columns.cells
      },
      header: {
        cells: columns.headers
      }
    })
  }, [datasourceModel, table])

  console.log("tableConfig")
  console.log(tableConfig)

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
