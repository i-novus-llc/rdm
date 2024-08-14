import React, { useLayoutEffect } from "react";
import {useDispatch, useSelector} from "react-redux";
import { compose } from 'recompose'

import { AdvancedTableContainer } from "n2o-framework/lib/components/widgets/AdvancedTable";
import { WithTableProps } from "n2o-framework/lib/components/widgets/AdvancedTable/WithTableProps";
import { WidgetHOC } from "n2o-framework/lib/core/widget/WidgetHOC";
import { dataSourceModelByPrefixSelector } from "n2o-framework/lib/ducks/datasource/selectors";
import { ModelPrefix } from "n2o-framework/lib/core/datasource/const";
import { setModel } from 'n2o-framework/lib/ducks/models/store'

import { getColumnsFromDatasource, getDataFromDatasource } from "./utils";
import { rdmUpdateConfigField } from "../store";


function DataGridWidget(props) {
  const { datasource, id } = props

  const dispatch = useDispatch()
  const datasourceModel = useSelector(dataSourceModelByPrefixSelector(datasource, ModelPrefix.source))

  useLayoutEffect(() => {
    const columns = getColumnsFromDatasource(datasourceModel)
    const data = getDataFromDatasource(datasourceModel)

    if (columns) {
      const { cells: bodyCells, headers: headerCells } = columns

      dispatch(rdmUpdateConfigField(id, 'table.body.cells', bodyCells))
      dispatch(rdmUpdateConfigField(id, 'table.header.cells', headerCells))

      dispatch(setModel(ModelPrefix.source, datasource, data))
    }
  }, [datasourceModel, id]);

  return (
    <AdvancedTableContainer
      {...props}
      className="rdm-data-grid"
    />
  );
}

DataGridWidget.displayName = '@rdm/DataGridWidget'

export default compose(
  WidgetHOC,
)(WithTableProps(DataGridWidget), 'table.header.cells')
