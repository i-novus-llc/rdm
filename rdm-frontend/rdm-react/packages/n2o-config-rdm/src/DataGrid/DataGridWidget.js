import React from 'react'
import StandardWidget from 'n2o-framework/lib/components/widgets/StandardWidget'
import { WidgetHOC } from 'n2o-framework/lib/core/widget/WidgetHOC'
import { WithActiveModel } from 'n2o-framework/lib/components/widgets/Widget/WithActiveModel'

import { DataGridPagination } from './DataGridPagination'
import { DataGridContainer } from './DataGridContainer'

function DataGridWidget(props) {
  const {
    id,
    disabled,
    toolbar,
    paging,
    models,
    page,
    size,
    count,
    fetchData,
    setPage,
    loading,
  } = props
  const { table, ...otherProps } = props

  return (
    <div className="rdm-data-grid">
      <StandardWidget
        loading={loading}
        widgetId={id}
        toolbar={toolbar}
        disabled={disabled}
        bottomLeft={paging && (
          <DataGridPagination
            {...paging}
            setPage={setPage}
            fetchData={fetchData}
            models={models}
            activePage={page}
            size={size}
            count={count}
          />
        )}
      >
        <DataGridContainer {...otherProps} {...table} />
      </StandardWidget>
    </div>
  );
}

export default WidgetHOC(WithActiveModel(
  DataGridWidget,
  () => false,
))
