import React, { Component } from 'react';
import PropTypes from 'prop-types';
import isEmpty from 'lodash/isEmpty';
import isEqual from 'lodash/isEqual';
import { connect } from 'react-redux';
import { createStructuredSelector } from 'reselect';
import Pagination from 'n2o-framework/lib/components/snippets/Pagination/Pagination';
import {
  makeWidgetCountSelector,
  makeWidgetSizeSelector,
  makeWidgetPageSelector,
} from 'n2o-framework/lib/ducks/widgets/selectors';
import { makeGetModelByPrefixSelector, makeGetFilterModelSelector } from 'n2o-framework/lib/ducks/models/selectors';
import { dataRequestWidget } from 'n2o-framework/lib/ducks/widgets/store';
import { PREFIXES } from 'n2o-framework/lib/ducks/models/constants';

/**
 * Компонент табличной пагинации. По `widgetId` автоматически определяет все свойства для `Paging`
 * @reactProps {string} widgetId - уникальный идентификатор виджета
 * @reactProps {number} count
 * @reactProps {number} size
 * @reactProps {number} activePage
 * @reactProps {function} onChangePage
 */
class DataGridPagination extends Component {
  componentDidUpdate(prevProps) {
    const { datasource, onChangePage, activePage, count, size } = this.props;
    if (
      datasource &&
      !isEqual(prevProps.datasource, datasource) &&
      (isEmpty(datasource) && count > 0 && activePage > 1)
    ) {
      onChangePage(Math.ceil(count / size));
    }
  }

  render() {
    const {
      count,
      size,
      activePage,
      onChangePage,
      prev,
      next,
      first,
      last,
      lazy,
      showCountRecords,
      hideSinglePage,
      maxButtons,
      withoutBody,
      prevText,
      nextText,
      filters,
    } = this.props;

    return (
      <Pagination
        onSelect={page =>  onChangePage(page, { ...filters })}
        activePage={activePage}
        count={count > 0 ? count - 1 : count}
        size={size}
        maxButtons={maxButtons}
        stepIncrement={10}
        prev={prev}
        prevText={prevText}
        next={next}
        nextText={nextText}
        first={first}
        last={last}
        lazy={lazy}
        showCountRecords={showCountRecords}
        hideSinglePage={hideSinglePage}
        withoutBody={withoutBody}
      />
    );
  }
}

DataGridPagination.propTypes = {
  widgetId: PropTypes.string,
  count: PropTypes.number,
  size: PropTypes.number,
  activePage: PropTypes.number,
  onChangePage: PropTypes.func,
  datasource: PropTypes.array,
  maxButtons: PropTypes.number,
};

DataGridPagination.defaultProps = {
  datasource: [],
  maxButtons: 4,
};

const mapStateToProps = createStructuredSelector({
  count: (state, props) =>
    makeWidgetCountSelector(props.widgetId)(state, props),
  size: (state, props) => makeWidgetSizeSelector(props.widgetId)(state, props),
  activePage: (state, props) =>
    makeWidgetPageSelector(props.widgetId)(state, props),
  datasource: (state, props) =>
    makeGetModelByPrefixSelector(PREFIXES.datasource, props.widgetId)(
      state,
      props
    ),
  filters: (state, props) =>
    makeGetFilterModelSelector(props.widgetId)(state, props),
});

function mapDispatchToProps(dispatch, ownProps) {
  return {
    onChangePage: (page, filters) => {
      dispatch(
        dataRequestWidget(ownProps.widgetId, ownProps.modelId, {
          page,
          ...filters
        })
      );
    },
  };
}

DataGridPagination = connect(
  mapStateToProps,
  mapDispatchToProps
)(DataGridPagination);
export default DataGridPagination;
