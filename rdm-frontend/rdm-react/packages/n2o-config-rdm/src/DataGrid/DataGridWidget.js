import React from 'react';
import {compose, withHandlers} from 'recompose';
import dependency from 'n2o-framework/lib/core/dependency';
import DataGridPagination from "./DataGridPagination";
import StandardWidget from 'n2o-framework/lib/components/widgets/StandardWidget';
import DataGridContainer from "./DataGridContainer";
import {connect} from "react-redux";

function DataGridWidget({
    id,
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
        widgetId={id}
        modelId={id}
        actions={actions}
        bottomLeft={paging && <DataGridPagination widgetId={id}/>}
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
    getWidgetProps: ({id, table, ...rest}) => () => ({
      widgetId: id,
      modelId: id,
      table,
      ...table,
      ...rest
    })
  }),
  connect(mapStateToProps),
)(DataGridWidget);
