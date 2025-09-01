import React, { useLayoutEffect } from "react";
import { useDispatch, useSelector } from "react-redux";
import isEmpty from "lodash/isEmpty";
import { EMPTY_ARRAY } from "n2o-framework/lib/utils/emptyTypes";

import { AdvancedTableWidget } from "n2o-framework/lib/components/widgets/AdvancedTable";
import { dataSourceModelByPrefixSelector } from "n2o-framework/lib/ducks/datasource/selectors";
import { ModelPrefix } from "n2o-framework/lib/core/datasource/const";
import { setModel } from 'n2o-framework/lib/ducks/models/store'
import { updateTableParams, changeTableParam } from "n2o-framework/lib/ducks/table/store";

import { getColumnsFromDatasource, getDataFromDatasource } from "./utils";

function DataGridWidget(props) {
  const { datasource, id, table } = props

  const dispatch = useDispatch()
  const datasourceModel = useSelector(dataSourceModelByPrefixSelector(datasource, ModelPrefix.source))

  useLayoutEffect(() => {
    const columns = getColumnsFromDatasource(datasourceModel)

    /** колонки приходят не в данных widget, а с данными таблицы в list */
    if (columns) {
      const { headers: headerCells, cells: bodyCells } = columns

      dispatch(updateTableParams(id, { isInit: true }))

      dispatch(changeTableParam(id, 'header', { cells: headerCells }))
      dispatch(changeTableParam(id, 'body', { cells: bodyCells }))

      const computedDsModel = getDataFromDatasource(datasourceModel)

      dispatch(setModel(ModelPrefix.source, datasource, computedDsModel))
    }
  }, [datasourceModel, id])

  /** Guard table может придти без body и header */
  const defaultTable = isEmpty(table.body) || isEmpty(table.header) ?
    { ...table, header: { cells: EMPTY_ARRAY }, body: { cells: EMPTY_ARRAY } } : table

  return <AdvancedTableWidget {...props} table={defaultTable} className="rdm-data-grid" />
}

DataGridWidget.displayName = '@rdm/DataGridWidget'

export default DataGridWidget
