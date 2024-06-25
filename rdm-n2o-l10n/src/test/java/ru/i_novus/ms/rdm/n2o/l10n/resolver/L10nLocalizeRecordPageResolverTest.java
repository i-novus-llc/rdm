package ru.i_novus.ms.rdm.n2o.l10n.resolver;

import net.n2oapp.framework.api.metadata.SourceComponent;
import net.n2oapp.framework.api.metadata.control.N2oField;
import net.n2oapp.framework.api.metadata.control.plain.N2oCheckbox;
import net.n2oapp.framework.api.metadata.control.plain.N2oOutputText;
import net.n2oapp.framework.api.metadata.global.view.fieldset.N2oFieldsetRow;
import net.n2oapp.platform.i18n.Messages;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.n2o.api.model.DataRecordRequest;
import ru.i_novus.ms.rdm.n2o.l10n.BaseTest;

import java.util.Arrays;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static ru.i_novus.ms.rdm.n2o.api.util.DataRecordUtils.addPrefix;
import static ru.i_novus.ms.rdm.n2o.l10n.constant.L10nRecordConstants.*;
import static ru.i_novus.ms.rdm.n2o.l10n.utils.StructureTestConstants.DEFAULT_STRUCTURE;

@RunWith(MockitoJUnitRunner.class)
public class L10nLocalizeRecordPageResolverTest extends BaseTest {

    private static final String TEST_UNSATISFIED_ACTION = "ab";

    private static final List<String> REGULAR_FIELD_IDS = List.of(
            FIELD_LOCALE_NAME, FIELD_HIDE_UNLOCALIZABLE
    );

    @InjectMocks
    private L10nLocalizeRecordPageResolver resolver;

    @Mock
    private Messages messages;

    @Test
    public void testIsSatisfied() {

        assertTrue(resolver.isSatisfied(DATA_ACTION_LOCALIZE));
        assertFalse(resolver.isSatisfied(TEST_UNSATISFIED_ACTION));
        assertFalse(resolver.isSatisfied(null));
    }

    @Test
    public void testCreateRegularFields() {

        when(messages.getMessage(any(String.class))).thenAnswer(invocation -> invocation.getArguments()[0]);

        final DataRecordRequest request = new DataRecordRequest();

        final List<SourceComponent> fields = resolver.createRegularFields(request);
        assertNotNull(fields);
        assertEquals(1, fields.size());

        final SourceComponent l10nField = fields.get(0);
        assertTrue(l10nField instanceof N2oFieldsetRow);

        final N2oFieldsetRow n2oRow = (N2oFieldsetRow) l10nField;
        assertEquals(REGULAR_FIELD_IDS.size(), n2oRow.getItems().length);

        REGULAR_FIELD_IDS.forEach(id ->
                assertTrue(existsItem(Arrays.asList(n2oRow.getItems()), id))
        );
    }

    private boolean existsItem(List<SourceComponent> items, String id) {

        return items.stream().anyMatch(item -> id.equals(getFieldId(item)));
    }

    private String getFieldId(SourceComponent item) {

        return (item instanceof N2oField) ? ((N2oField) item).getId() : null;
    }

    @Test
    public void testProcessDynamicFields() {

        final Structure structure = createStructure();

        final DataRecordRequest request = new DataRecordRequest();
        request.setStructure(structure);

        final List<SourceComponent> fields = createFields(structure);

        resolver.processDynamicFields(request, fields);
        assertEquals(structure.getAttributes().size() + 3, fields.size());
    }

    @Test
    public void testProcessDynamicFieldsWithEmpty() {

        final DataRecordRequest request = new DataRecordRequest();
        request.setStructure(Structure.EMPTY);

        final List<SourceComponent> fields = emptyList();

        resolver.processDynamicFields(request, fields);
        assertEmpty(fields);
    }

    private Structure createStructure() {

        return new Structure(DEFAULT_STRUCTURE);
    }

    private List<SourceComponent> createFields(Structure structure) {

        final List<SourceComponent> fields = structure.getAttributes().stream().map(this::createField).collect(toList());

        final N2oFieldsetRow notN2oField = new N2oFieldsetRow();
        fields.add(notN2oField);

        final N2oCheckbox notPrefixed = new N2oCheckbox();
        notPrefixed.setId("123");
        fields.add(notPrefixed);

        final N2oCheckbox notAttribute = new N2oCheckbox();
        notAttribute.setId(addPrefix("123"));
        fields.add(notAttribute);

        return fields;
    }

    private SourceComponent createField(Structure.Attribute attribute) {

        final N2oOutputText field = new N2oOutputText();
        field.setId(addPrefix(attribute.getCode()));
        field.setLabel(attribute.getName());

        return field;
    }
}