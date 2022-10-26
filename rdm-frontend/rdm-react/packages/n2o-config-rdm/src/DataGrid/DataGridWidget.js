import React from 'react'
// import omit from 'lodash/omit'
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
  } = props

  const getWidgetProps = ({ id, table, ...rest }) => ({
    id,
    table,
    ...table,
    ...rest,
  })

  return (
    <div className="rdm-data-grid">
      <StandardWidget
        widgetId={id}
        toolbar={toolbar}
        disabled={disabled}
        bottomLeft={paging && (
          <DataGridPagination
            {...paging}
            models={models}
            activePage={page}
            size={size}
            count={count}
          />
        )}
      >
        <DataGridContainer {...getWidgetProps(props)} />
      </StandardWidget>
    </div>
  );
}

// const OmitProps = Component => (props) => {
//   const omited = omit(props, [])
//
//   omited.table = omit(omited.table, ['sorting', 'size'])
//
//   return (
//     <Component {...omited} />
//   )
// }

export default WidgetHOC(WithActiveModel(
  DataGridWidget,
  () => false,
))
