import React, { useLayoutEffect } from "react";
import {useDispatch, useSelector} from "react-redux";
import isEqual from 'lodash/isEqual'
import { AdvancedTableWidget } from "n2o-framework/lib/components/widgets/AdvancedTable";
import { dataSourceModelByPrefixSelector } from "n2o-framework/lib/ducks/datasource/selectors";
import { ModelPrefix } from "n2o-framework/lib/core/datasource/const";

import { getColumnsFromDatasource, getDataFromDatasource } from "./utils";
import { rdmUpdateConfigField } from "../store";


function DataGridWidget(props) {
  const {
    table,
    datasource
  } = props
  const dispatch = useDispatch()
  const datasourceModel = useSelector(dataSourceModelByPrefixSelector(datasource, ModelPrefix.source))

  useLayoutEffect(() => {
    const columns = getColumnsFromDatasource(datasourceModel)
    const { cells: bodyCells, headers: headerCells } = columns
    const isEqualBodyCells = isEqual(table.body.cells, bodyCells)
    const isEqualHeaderCells = isEqual(table.header.cells, headerCells)

    if (isEqualBodyCells && isEqualHeaderCells) { return }

    dispatch(rdmUpdateConfigField(datasource, 'table.body.cells', bodyCells))
    dispatch(rdmUpdateConfigField(datasource, 'table.header.cells', headerCells))
  }, [datasourceModel, table.body, table.header]);

  return (
    <AdvancedTableWidget
      {...props}
      className="rdm-data-grid"
      dataMapper={getDataFromDatasource}
    />
  );
}

DataGridWidget.displayName = '@rdm/DataGridWidget'

export default DataGridWidget
