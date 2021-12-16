import React from 'react';
import {compose, withHandlers} from 'recompose';
import dependency from 'n2o-framework/lib/core/dependency';
import DataGridPagination from "./DataGridPagination";
import StandardWidget from 'n2o-framework/lib/components/widgets/StandardWidget';
import DataGridContainer from "./DataGridContainer";
import {connect} from "react-redux";

function DataGridWidget({
    id: widgetId,
    disabled,
    actions,
    getWidgetProps,
    toolbar,
    paging,
    models,
  }) {
  return (
    <div className="rdm-data-grid">
      <StandardWidget
        toolbar={toolbar}
        disabled={disabled}
        widgetId={widgetId}
        actions={actions}
        bottomLeft={paging && <DataGridPagination widgetId={widgetId}/>}
      >
        <DataGridContainer {...getWidgetProps()} models={models}/>
      </StandardWidget>
    </div>
  );
}

const mapStateToProps = (state) => {
  return {
    models: state.models
  }
}

export default compose(
  dependency,
  withHandlers({
    getWidgetProps: ({id: widgetId, table, ...rest}) => () => ({
      widgetId,
      table,
      ...table,
      ...rest
    })
  }),
  connect(mapStateToProps),
)(DataGridWidget);
