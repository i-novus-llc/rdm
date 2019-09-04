import React from 'react';
import { connect } from 'react-redux';
import { compose, withHandlers, withState, lifecycle } from 'recompose';
import widgetContainer from 'n2o-framework/lib/components/widgets/WidgetContainer';
import DataGrid from "./DataGrid";

import { setModel } from 'n2o-framework/lib/actions/models';
import { makeGetFilterModelSelector } from 'n2o-framework/lib/selectors/models';
import { PREFIXES } from 'n2o-framework/lib/constants/models';
import columnHOC from 'n2o-framework/lib/components/widgets/Table/withColumn';
import TableCell from 'n2o-framework/lib/components/widgets/Table/TableCell';
import factoryResolver from 'n2o-framework/lib/utils/factoryResolver';

import { map, get, isEqual, omit, isObject, isEmpty } from 'lodash';

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
        prepareColumns: ({ datasource, widgetId, sorting, onSort }) => () => {
            const columns = get(datasource, '[0].columns', null);

            if (!columns) {
                return null;
            }

            return map(columns, (item, index) => {
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
        getData: ({ datasource }) => () => datasource && datasource.length > 1 ? map(datasource.slice(1), item => item.row) : [],
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
                datasource,
                setRows,
                getData
            } = this.props;
            if (!isEqual(prevProps.datasource, datasource)) {
                setColumns(prepareColumns());
            }

            if (!isEqual(prevProps.datasource, datasource)) {
                setRows(getData());
            }
        }
    })
)(DataGridContainer);
