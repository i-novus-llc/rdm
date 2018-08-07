package ru.inovus.ms.rdm.file.export;

import org.springframework.data.domain.Page;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;
import ru.inovus.ms.rdm.file.Row;
import ru.inovus.ms.rdm.model.SearchDataCriteria;
import ru.inovus.ms.rdm.service.api.VersionService;
import ru.inovus.ms.rdm.util.ConverterUtil;

import java.util.Iterator;
import java.util.List;

/**
 * Created by znurgaliev on 23.07.2018.
 */
public class VersionDataIterator implements Iterator<Row> {

    private final static int MAX_BUFFER_SIZE = 1000;
    private int currentPage = 0;
    private Iterator<Integer> versionIdIterator;
    private Integer currentVersionId;
    private VersionService versionService;
    private Iterator<RowValue> buffer;
    private boolean hasNext = true;

    public VersionDataIterator(VersionService versionService, List<Integer> versionIdList) {
        this.versionService = versionService;
        this.versionIdIterator = versionIdList.iterator();
        this.currentVersionId = this.versionIdIterator.next();
    }

    @Override
    public boolean hasNext() {
        return hasNext && (buffer != null && buffer.hasNext() || nextPage());
    }

    @Override
    public Row next() {
        if (!hasNext()) return null;
        RowValue rowValue = buffer.next();
        return ConverterUtil.toRow(rowValue);
    }

    private boolean nextPage() {
        SearchDataCriteria criteria = new SearchDataCriteria();
        criteria.setPageSize(MAX_BUFFER_SIZE);
        criteria.setPageNumber(++currentPage);
        Page<RowValue> page = versionService.search(currentVersionId, criteria);
        if (page != null && page.getContent() != null && !page.getContent().isEmpty()){
            buffer = page.getContent().iterator();
            return true;
        } else if (versionIdIterator.hasNext()){
            currentVersionId = versionIdIterator.next();
            currentPage = 0;
            return nextPage();
        } else {
            hasNext = false;
            return false;
        }
    }
}
