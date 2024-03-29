package ru.i_novus.ms.rdm.n2o.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.compare.CompareCriteria;
import ru.i_novus.ms.rdm.api.model.diff.StructureDiff;
import ru.i_novus.ms.rdm.api.rest.VersionRestService;
import ru.i_novus.ms.rdm.api.service.CompareService;
import ru.i_novus.ms.rdm.n2o.model.AttributeDiff;
import ru.i_novus.platform.datastorage.temporal.enums.DiffStatusEnum;

import java.util.*;
import java.util.stream.Collectors;

@Controller
public class CompareStructureController {

    private final CompareService compareService;

    private final VersionRestService versionService;

    @Autowired
    public CompareStructureController(CompareService compareService,
                                      VersionRestService versionService) {

        this.compareService = compareService;
        this.versionService = versionService;
    }

    public Page<AttributeDiff> getCommonDiff(CompareCriteria criteria) {

        Structure newStructure = versionService.getStructure(criteria.getNewVersionId());
        StructureDiff structureDiff = compareService.compareStructures(criteria.getOldVersionId(), criteria.getNewVersionId());

        List<AttributeDiff> deleted = structureDiff.getDeleted().stream().
                map(attrDiff -> createDiff(attrDiff.getOldAttribute(), null, DiffStatusEnum.DELETED))
                .collect(Collectors.toList());
        Map<String, AttributeDiff> insAndUpd = createDiffMap(structureDiff, DiffStatusEnum.INSERTED, DiffStatusEnum.UPDATED);

        List<AttributeDiff> resultDiffs = new ArrayList<>(newStructure.getAttributes().stream()
                .map(attribute -> insAndUpd.containsKey(attribute.getCode()) ?
                        insAndUpd.get(attribute.getCode()) :
                        createDiff(null, attribute, null))
                .collect(Collectors.toList()));
        resultDiffs.addAll(deleted);
        resultDiffs.removeIf(attrDiff ->
                criteria.getDiffStatus() != null && !Objects.equals(criteria.getDiffStatus(), attrDiff.getDiffStatus()));

        return getPage(resultDiffs, criteria);
    }

    public Page<AttributeDiff> getOldWithDiff(CompareCriteria criteria) {
        Structure oldStructure = versionService.getStructure(criteria.getOldVersionId());
        StructureDiff structureDiff = compareService.compareStructures(criteria.getOldVersionId(), criteria.getNewVersionId());

        Map<String, AttributeDiff> delAndUpd = createDiffMap(structureDiff, DiffStatusEnum.DELETED, DiffStatusEnum.UPDATED);

        List<AttributeDiff> resultDiffs = new ArrayList<>(oldStructure.getAttributes().stream()
                .map(attribute -> delAndUpd.containsKey(attribute.getCode()) ?
                        delAndUpd.get(attribute.getCode()) :
                        createDiff(attribute, null, null))
                .filter(attrDiff ->
                        criteria.getDiffStatus() == null ||
                        Objects.equals(criteria.getDiffStatus(), attrDiff.getDiffStatus()))
                .collect(Collectors.toList()));

        return getPage(resultDiffs, criteria);
    }

    public Page<AttributeDiff> getNewWithDiff(CompareCriteria criteria) {
        Structure newStructure = versionService.getStructure(criteria.getNewVersionId());
        StructureDiff structureDiff = compareService.compareStructures(criteria.getOldVersionId(), criteria.getNewVersionId());

        Map<String, AttributeDiff> insAndUpd = createDiffMap(structureDiff, DiffStatusEnum.INSERTED, DiffStatusEnum.UPDATED);

        List<AttributeDiff> resultDiffs = new ArrayList<>(newStructure.getAttributes().stream()
                .map(attribute -> insAndUpd.containsKey(attribute.getCode()) ?
                        insAndUpd.get(attribute.getCode()) :
                        createDiff(null, attribute, null))
                .filter(attrDiff ->
                        criteria.getDiffStatus() == null ||
                                Objects.equals(criteria.getDiffStatus(), attrDiff.getDiffStatus()))
                .collect(Collectors.toList()));

        return getPage(resultDiffs, criteria);
    }

    private AttributeDiff createDiff(Structure.Attribute oldAttr, Structure.Attribute newAttr, DiffStatusEnum diffStatus) {

        oldAttr = oldAttr != null ? oldAttr : new Structure.Attribute();
        newAttr = newAttr != null ? newAttr : new Structure.Attribute();

        return new AttributeDiff(oldAttr, newAttr, diffStatus);
    }

    private Map<String, AttributeDiff> createDiffMap(StructureDiff structureDiff, DiffStatusEnum... statuses) {
        Map<String, AttributeDiff> diffMap = new LinkedHashMap<>();
        for (DiffStatusEnum status : statuses) {
            diffMap.putAll(createDiffList(structureDiff, status).stream()
                    .collect(Collectors.toMap(AttributeDiff::getCode, attrDiff -> attrDiff)));
        }
        return diffMap;
    }

    private List<AttributeDiff> createDiffList(StructureDiff structureDiff, DiffStatusEnum diffStatus) {
        List<StructureDiff.AttributeDiff> currentDiffs = Collections.emptyList();
        if (DiffStatusEnum.INSERTED.equals(diffStatus) && structureDiff.getInserted() != null) {
            currentDiffs = structureDiff.getInserted();
        } else if (DiffStatusEnum.UPDATED.equals(diffStatus) && structureDiff.getUpdated() != null) {
            currentDiffs = structureDiff.getUpdated();
        } else if (DiffStatusEnum.DELETED.equals(diffStatus) && structureDiff.getDeleted() != null) {
            currentDiffs = structureDiff.getDeleted();
        }
        return currentDiffs.stream()
                .map(attrDiff -> createDiff(attrDiff.getOldAttribute(), attrDiff.getNewAttribute(), diffStatus))
                .collect(Collectors.toList());
    }

    private Page<AttributeDiff> getPage(List<AttributeDiff> content, Pageable pageable) {
        Integer totalCount = content.size();
        content = content.stream()
                .skip(pageable.getOffset())
                .limit(pageable.getPageSize())
                .collect(Collectors.toList());

        return new PageImpl<>(content, pageable, totalCount);    }




}
