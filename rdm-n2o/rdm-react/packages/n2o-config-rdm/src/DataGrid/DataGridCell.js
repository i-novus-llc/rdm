import React from 'react';
import { pure } from 'recompose';
import { get, isEmpty, isObject } from 'lodash';
/**
 * Компонент обертка Cell
 * @param children - вставляемый компонент
 * @param hasSpan - флаг возможности colSpan/rowSpan в этой колонке
 * @param record - модель строки
 * @param valueKey
 * @returns {*}
 * @constructor
 */
function DataGridCell({ children, hasSpan, record, valueKey }) {
    const { span } = record;
    let colSpan = 1;
    let rowSpan = 1;

    if (hasSpan && span) {
        if (span.colSpan === 0 || span.rowSpan === 0) {
            return null;
        }
        colSpan = span.colSpan;
        rowSpan = span.rowSpan;
    }

    let style = get(record, 'cellOptions.styles', {});

    if (isEmpty(style) && isObject(record[valueKey])) {
        style = get(record, `${valueKey}.cellOptions.styles`, {});
    }

    return (
        <td colSpan={colSpan} rowSpan={rowSpan} style={style}>
            <div className="n2o-advanced-table-cell-expand d-flex flex-column align-items-start">
                {children}
                {isObject(record[valueKey]) && record[valueKey].oldValue && (
                    <span style={{ textDecoration: 'line-through' }}>{record[valueKey].oldValue}</span>
                )}
            </div>
        </td>
    );
}

export default pure(DataGridCell);
