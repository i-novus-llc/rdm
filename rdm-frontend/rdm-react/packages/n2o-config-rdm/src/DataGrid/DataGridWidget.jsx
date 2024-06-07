import React, { useLayoutEffect } from "react";
import {useDispatch, useSelector} from "react-redux";
import { compose } from 'recompose'

import { AdvancedTableContainer } from "n2o-framework/lib/components/widgets/AdvancedTable";
import { WithTableProps } from "n2o-framework/lib/components/widgets/AdvancedTable/WithTableProps";
import { WidgetHOC } from "n2o-framework/lib/core/widget/WidgetHOC";
import { withSecurityList } from "n2o-framework/lib/core/auth/withSecurity";
import { dataSourceModelByPrefixSelector } from "n2o-framework/lib/ducks/datasource/selectors";
import { ModelPrefix } from "n2o-framework/lib/core/datasource/const";
import { setModel } from 'n2o-framework/lib/ducks/models/store'

import { getColumnsFromDatasource, getDataFromDatasource } from "./utils";
import { rdmUpdateConfigField } from "../store";


function DataGridWidget(props) {
  const { datasource, fetchData } = props

  const dispatch = useDispatch()
  const datasourceModel = useSelector(dataSourceModelByPrefixSelector(datasource, ModelPrefix.source))
  const filterModel = useSelector(dataSourceModelByPrefixSelector(datasource, ModelPrefix.filter))

  useLayoutEffect(() => {
    const columns = getColumnsFromDatasource(datasourceModel)
    const data = getDataFromDatasource(datasourceModel)

    if (columns) {
      const { cells: bodyCells, headers: headerCells } = columns

      dispatch(rdmUpdateConfigField(datasource, 'table.body.cells', bodyCells))
      dispatch(rdmUpdateConfigField(datasource, 'table.header.cells', headerCells))

      dispatch(setModel(ModelPrefix.source, datasource, data))
    }
  }, [datasourceModel]);

  useLayoutEffect(() => {
    if (filterModel) {
      const mappedFilter = Object.entries(filterModel)
        .reduce((out, [key, value]) => {
          out[`filter.${key}`] = value

          return out;
        }, {})

      fetchData(mappedFilter)
    }
  }, [filterModel, fetchData]);


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
)(withSecurityList(WithTableProps(DataGridWidget), 'table.header.cells'))
