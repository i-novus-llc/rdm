import React from 'react';
import { compose, withHandlers } from 'recompose';
import dependency from 'n2o-framework/lib/core/dependency';
import StandardWidget from 'n2o-framework/lib/components/widgets/StandardWidget';
import factoryResolver from 'n2o-framework/lib/utils/factoryResolver';
import DataGridContainer from "./DataGridContainer";

function DataGridWidget({
    widgetId,
    disabled,
    actions,
    prepareFilter,
    getWidgetProps,
    toolbar
}) {
    return (
        <div className="rdm-data-grid">
            <StandardWidget
                toolbar={toolbar}
                disabled={disabled}
                widgetId={widgetId}
                actions={actions}
                // filter={prepareFilter()}
            >
                <DataGridContainer {...getWidgetProps()} />
            </StandardWidget>
        </div>
    );
}

export default compose(
    dependency,
    withHandlers({
        prepareFilters: ({ filter }) => () => factoryResolver(filter, 'Input'),
        getWidgetProps: ({ id: widgetId, ...rest }) => () => ({
            widgetId,
            ...rest
        })
    })
)(DataGridWidget);
