import React from 'react';
import { connect } from 'react-redux';
import { compose, withHandlers, withState, lifecycle } from 'recompose';
import widgetContainer from 'n2o-framework/lib/components/widgets/WidgetContainer';
import DataGrid from "./DataGrid";

import { setModel } from 'n2o-framework/lib/ducks/models/store';
import { makeGetFilterModelSelector } from 'n2o-framework/lib/ducks/models/selectors';
import { PREFIXES } from 'n2o-framework/lib/ducks/models/constants';
import columnHOC from 'n2o-framework/lib/components/widgets/Table/withColumn';
import TableCell from 'n2o-framework/lib/components/widgets/Table/TableCell';
import factoryResolver from 'n2o-framework/lib/core/factory/factoryResolver';
import map from 'lodash/map';
import get from 'lodash/get';
import isEqual from 'lodash/isEqual';
import omit from 'lodash/omit';
import isObject from 'lodash/isObject';
import isEmpty from 'lodash/isEmpty';
import isNil from 'lodash/isNil'

const ReduxCell = columnHOC(TableCell);

function DataGridContainer({
    columns,
    rows,
    onSetFilter,
    filters,
    onResolve,
    table,
}) {
    return (
        <DataGrid
            columns={columns}
            data={rows}
            onFilter={onSetFilter}
            filters={filters}
            onResolve={onResolve}
            {...table}
        />
    );
}

const mapStateToProps = (state, props) => {
  return {
    filtersFromRedux: makeGetFilterModelSelector(props.widgetId)(state, props),
  };
};

export default compose(
    connect(mapStateToProps),
    widgetContainer({
        mapProps: props => ({
            onSetFilter: (model) => {
                props.dispatch(setModel(PREFIXES.filter, props.widgetId, model))
            },
            models: props.models,
            actions: props.actions,
            widgetId: props.widgetId,
            minHeight: props.minHeight,
            filterable: props.filterable,
            rowHeight: props.rowHeight,
            datasource: props.datasource || [],
            toolbar: props.toolbar,
            onSort: props.onSort,
            onFetch: props.onFetch,
            onResolve: props.onResolve,
            ...props
        })
    }),
    withState('columns', 'setColumns'),
    withState('rows', 'setRows', []),
    withState('filters', 'setFilters', {}),
    withHandlers({
        prepareColumns: ({ models, widgetId, modelId, sorting, onSort }) => () => {
            const datasource = get(models, `datasource.${modelId}`, []);
            const columns = get(datasource, '[0].columns', null);

            if (!columns) {
                return null;
            }

            return map(columns, (item) => {
                let newItem = Object.assign({}, item);
                newItem = {
                  ...newItem,
                    src: 'TextTableHeader',
                    id: newItem.key,
                    dataIndex: newItem.key,
                    width: newItem.width,
                    filterControl: newItem.filterControl,
                    filterable: newItem.filterable,
                    resizable: newItem.resizable,
                    sortable: newItem.sortable,
                };

                const resolvedProps = factoryResolver(omit(newItem, ['filterControl']));

                return {
                    ...newItem,
                    title: (
                        <ReduxCell
                            {...resolvedProps}
                            label={item.name}
                            columnId={newItem.key}
                            widgetId={widgetId}
                            as='div'
                            sorting={sorting && sorting[newItem.key]}
                            onSort={onSort}
                        />
                    ),
                    render: value => ({
                        children: isObject(value) ? value['value'] : value,
                        props: {
                            valueKey: newItem.key
                        }
                    })
                };
            });
        },
        getData: ({ models, widgetId }) => () => {
          const datasource = get(models, `datasource.${widgetId}`, []);
          return datasource && datasource.length > 1 ? map(datasource.slice(1), item => item.row) : [];
        },
        onSetFilter: ({ onSetFilter, filters, filtersFromRedux, setFilters, onFetch }) => filter => {
            const filterModel = {
                ...filtersFromRedux,
                [`filter.${filter.id}`]: filter.value,
            };

            setFilters({
                ...filters,
                [filter.id]: filter.value
            });
            onSetFilter(filterModel);
            onFetch({
                ...filterModel,
                page: 1
            });
        }
    }),
    lifecycle({
        componentDidUpdate(prevProps) {
            const {
                setColumns,
                prepareColumns,
                models,
                setRows,
                getData,
                onResolve,
            } = this.props;
            if (!isEqual(prevProps.models, models)) {
                setColumns(prepareColumns());
            }

            if (!isEqual(prevProps.models, models)) {
                setRows(getData());
            }

            if ((isEmpty(prevProps.models) || isNil(prevProps.models)) && !isEmpty(getData())) {
              onResolve(getData()[0]);
            }
        }
    })
)(DataGridContainer);
