package ru.i_novus.ms.rdm.impl.service;

import org.junit.Before;
import org.junit.Test;
import ru.i_novus.platform.datastorage.temporal.enums.DiffStatusEnum;
import ru.i_novus.platform.datastorage.temporal.model.Field;
import ru.i_novus.platform.datastorage.temporal.model.value.DiffFieldValue;
import ru.i_novus.platform.datastorage.temporal.model.value.DiffRowValue;
import ru.i_novus.platform.versioned_data_storage.pg_impl.model.BooleanField;
import ru.i_novus.platform.versioned_data_storage.pg_impl.model.IntegerField;
import ru.i_novus.platform.versioned_data_storage.pg_impl.model.StringField;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static ru.i_novus.platform.datastorage.temporal.enums.DiffStatusEnum.*;

public class DiffRowValueCalculatorTest {

    private static final String ID = "id";
    private static final String NAME = "name";
    private static final String CODE = "code";
    private static final String HEX = "hex";

    private static final Integer PK_VALUE = 2;
    private static final String PURPLE = "purple";
    private static final String VIOLET = "violet";

    private static final String A = "Лиловый";
    private static final String B = "Фиолетовый";

    private static final String EE82EE = "EE82EE";

    private static DiffRowValue inserted;
    private static DiffRowValue insertedPurple;
    private static DiffRowValue insertedPurpleA;
    private static DiffRowValue insertedPurpleB;
    private static DiffRowValue insertedVioletA;
    private static DiffRowValue insertedPurpleEE82EE;
    private static DiffRowValue insertedEE82EE;

    private static DiffRowValue updatedPurpleAToVioletA;
    private static DiffRowValue updatedVioletAToVioletB;
    private static DiffRowValue updatedPurpleAToVioletB;
    private static DiffRowValue updatedNullBToPurpleA;
    private static DiffRowValue updatedNullBToVioletA;
    private static DiffRowValue updatedPurpleNullToNullEE82EE;
    private static DiffRowValue updatedPurpleEE82EEToNullEE82EE;
    private static DiffRowValue updatedNullToEE82EE;
    private static DiffRowValue updatedPurpleToViolet;

    private static DiffRowValue deleted;
    private static DiffRowValue deletedVioletA;
    private static DiffRowValue deletedPurpleA;
    private static DiffRowValue deletedPurpleB;
    private static DiffRowValue deletedPurple;
    private static DiffRowValue deletedEE82EE;
    private static DiffRowValue deletedPurpleEE82EE;

    @Before
    public void setUp() {
        inserted = new DiffRowValue(
                List.of(
                        new DiffFieldValue<>(new IntegerField(ID), null, PK_VALUE, INSERTED)
                ),
                INSERTED);

        insertedPurple = new DiffRowValue(
                List.of(
                        new DiffFieldValue<>(new IntegerField(ID), null, PK_VALUE, INSERTED),
                        new DiffFieldValue<>(new StringField(CODE), null, PURPLE, INSERTED)
                ),
                INSERTED);

        insertedPurpleA = new DiffRowValue(
                List.of(
                        new DiffFieldValue<>(new IntegerField(ID), null, PK_VALUE, INSERTED),
                        new DiffFieldValue<>(new StringField(CODE), null, PURPLE, INSERTED),
                        new DiffFieldValue<>(new StringField(NAME), null, A, INSERTED)
                ),
                INSERTED);

        insertedPurpleB = new DiffRowValue(
                List.of(
                        new DiffFieldValue<>(new IntegerField(ID), null, PK_VALUE, INSERTED),
                        new DiffFieldValue<>(new StringField(CODE), null, PURPLE, INSERTED),
                        new DiffFieldValue<>(new StringField(NAME), null, B, INSERTED)
                ),
                INSERTED);

        insertedVioletA = new DiffRowValue(
                List.of(
                        new DiffFieldValue<>(new IntegerField(ID), null, PK_VALUE, INSERTED),
                        new DiffFieldValue<>(new StringField(CODE), null, VIOLET, INSERTED),
                        new DiffFieldValue<>(new StringField(NAME), null, A, INSERTED)
                ),
                INSERTED);

        insertedPurpleEE82EE = new DiffRowValue(
                List.of(
                        new DiffFieldValue<>(new IntegerField(ID), null, PK_VALUE, INSERTED),
                        new DiffFieldValue<>(new IntegerField(CODE), null, PURPLE, INSERTED),
                        new DiffFieldValue<>(new StringField(HEX), null, EE82EE, INSERTED)
                ),
                INSERTED);

        insertedEE82EE = new DiffRowValue(
                List.of(
                        new DiffFieldValue<>(new IntegerField(ID), null, PK_VALUE, INSERTED),
                        new DiffFieldValue<>(new StringField(HEX), null, EE82EE, INSERTED)
                ),
                INSERTED);

        updatedPurpleAToVioletA = new DiffRowValue(
                List.of(
                        new DiffFieldValue<>(new IntegerField(ID), null, PK_VALUE, null),
                        new DiffFieldValue<>(new BooleanField(CODE), PURPLE, VIOLET, UPDATED),
                        new DiffFieldValue<>(new StringField(NAME), null, A, null)
                ),
                UPDATED);

        updatedNullBToPurpleA = new DiffRowValue(
                List.of(
                        new DiffFieldValue<>(new IntegerField(ID), null, PK_VALUE, null),
                        new DiffFieldValue<>(new BooleanField(CODE), null, PURPLE, UPDATED),
                        new DiffFieldValue<>(new StringField(NAME), B, A, UPDATED)
                ),
                UPDATED);

        updatedVioletAToVioletB = new DiffRowValue(
                List.of(
                        new DiffFieldValue<>(new IntegerField(ID), null, PK_VALUE, null),
                        new DiffFieldValue<>(new BooleanField(CODE), null, VIOLET, null),
                        new DiffFieldValue<>(new StringField(NAME), A, B, UPDATED)
                ),
                UPDATED);

        updatedPurpleAToVioletB = new DiffRowValue(
                List.of(
                        new DiffFieldValue<>(new IntegerField(ID), null, PK_VALUE, null),
                        new DiffFieldValue<>(new BooleanField(CODE), PURPLE, VIOLET, UPDATED),
                        new DiffFieldValue<>(new StringField(NAME), A, B, UPDATED)
                ),
                UPDATED);

        updatedNullToEE82EE = new DiffRowValue(
                List.of(
                        new DiffFieldValue<>(new IntegerField(ID), null, PK_VALUE, null),
                        new DiffFieldValue<>(new StringField(HEX), null, EE82EE, UPDATED)
                ),
                UPDATED);

        updatedPurpleNullToNullEE82EE = new DiffRowValue(
                List.of(
                        new DiffFieldValue<>(new IntegerField(ID), null, PK_VALUE, null),
                        new DiffFieldValue<>(new StringField(CODE), PURPLE, null, UPDATED),
                        new DiffFieldValue<>(new StringField(HEX), null, EE82EE, UPDATED)
                ),
                UPDATED);

        updatedPurpleEE82EEToNullEE82EE = new DiffRowValue(
                List.of(
                        new DiffFieldValue<>(new IntegerField(ID), null, PK_VALUE, null),
                        new DiffFieldValue<>(new StringField(CODE), PURPLE, null, UPDATED),
                        new DiffFieldValue<>(new StringField(HEX), null, EE82EE, null)
                ),
                UPDATED);

        updatedPurpleToViolet = new DiffRowValue(
                List.of(
                        new DiffFieldValue<>(new IntegerField(ID), null, PK_VALUE, null),
                        new DiffFieldValue<>(new StringField(CODE), PURPLE, VIOLET, UPDATED)
                ),
                UPDATED);

        updatedNullBToVioletA = new DiffRowValue(
                List.of(
                        new DiffFieldValue<>(new IntegerField(ID), null, PK_VALUE, null),
                        new DiffFieldValue<>(new BooleanField(CODE), null, VIOLET, UPDATED),
                        new DiffFieldValue<>(new StringField(NAME), B, A, UPDATED)
                ),
                UPDATED);

        deleted = new DiffRowValue(
                List.of(
                        new DiffFieldValue<>(new IntegerField(ID), PK_VALUE, null, DELETED)
                ),
                DELETED);

        deletedVioletA = new DiffRowValue(
                List.of(
                        new DiffFieldValue<>(new IntegerField(ID), PK_VALUE, null, DELETED),
                        new DiffFieldValue<>(new StringField(CODE), VIOLET, null, DELETED),
                        new DiffFieldValue<>(new StringField(NAME), A, null, DELETED)
                ),
                DELETED);

        deletedPurpleA = new DiffRowValue(
                List.of(
                        new DiffFieldValue<>(new IntegerField(ID), PK_VALUE, null, DELETED),
                        new DiffFieldValue<>(new StringField(CODE), PURPLE, null, DELETED),
                        new DiffFieldValue<>(new StringField(NAME), A, null, DELETED)
                ),
                DELETED);

        deletedPurpleB = new DiffRowValue(
                List.of(
                        new DiffFieldValue<>(new IntegerField(ID), PK_VALUE, null, DELETED),
                        new DiffFieldValue<>(new StringField(CODE), PURPLE, null, DELETED),
                        new DiffFieldValue<>(new StringField(NAME), B, null, DELETED)
                ),
                DELETED);

        deletedPurple = new DiffRowValue(
                List.of(
                        new DiffFieldValue<>(new IntegerField(ID), PK_VALUE, null, DELETED),
                        new DiffFieldValue<>(new StringField(CODE), PURPLE, null, DELETED)
                ),
                DELETED);

        deletedEE82EE = new DiffRowValue(
                List.of(
                        new DiffFieldValue<>(new IntegerField(ID), PK_VALUE, null, DELETED),
                        new DiffFieldValue<>(new StringField(HEX), EE82EE, null, DELETED)
                ),
                DELETED);

        deletedPurpleEE82EE = new DiffRowValue(
                List.of(
                        new DiffFieldValue<>(new IntegerField(ID), PK_VALUE, null, DELETED),
                        new DiffFieldValue<>(new StringField(CODE), PURPLE, null, DELETED),
                        new DiffFieldValue<>(new StringField(HEX), EE82EE, null, DELETED)
                ),
                DELETED);
    }

    // INSERT + INSERT

    @Test
    public void testCalculate_insertAndInsert() {
        DiffRowValueCalculator calculator = new DiffRowValueCalculator(insertedPurpleA, insertedVioletA, Set.of());
        assertCalculateResult(calculator, insertedVioletA);
    }

    @Test
    public void testCalculate_insertAndInsert_sameNewValues() {
        DiffRowValueCalculator calculator = new DiffRowValueCalculator(insertedPurpleA, insertedPurpleA, Set.of());
        assertCalculateResult(calculator, insertedPurpleA);
    }

    @Test
    public void testCalculate_insertAndInsert_fieldsChanged() {
        DiffRowValueCalculator calculator = new DiffRowValueCalculator(insertedPurpleA, insertedPurpleEE82EE, Set.of(NAME, HEX));
        assertCalculateResult(calculator, insertedPurple);
    }

    @Test
    public void testCalculate_insertAndInsert_allFieldsChanged() {
        DiffRowValueCalculator calculator = new DiffRowValueCalculator(insertedPurpleA, insertedEE82EE, Set.of(CODE, NAME, HEX));
        assertCalculateResult(calculator, inserted);
    }

    @Test
    public void testCalculate_insertAndInsert_reverse() {
        DiffRowValueCalculator calculator = new DiffRowValueCalculator(insertedVioletA, insertedPurpleA, Set.of(), true);
        assertCalculateResult(calculator, deletedVioletA);
    }

    // INSERT + UPDATE

    @Test
    public void testCalculate_insertAndUpdate() {
        DiffRowValueCalculator calculator = new DiffRowValueCalculator(insertedPurpleA, updatedPurpleAToVioletA, Set.of());
        assertCalculateResult(calculator, insertedVioletA);
    }

    @Test
    public void testCalculate_insertAndUpdate_sameNewValues() {
        DiffRowValueCalculator calculator = new DiffRowValueCalculator(insertedPurpleA, updatedNullBToPurpleA, Set.of());
        assertCalculateResult(calculator, insertedPurpleA);
    }

    @Test
    public void testCalculate_insertAndUpdate_fieldsChanged() {
        DiffRowValueCalculator calculator = new DiffRowValueCalculator(insertedPurpleA, updatedPurpleNullToNullEE82EE, Set.of(NAME));
        DiffRowValue expectedResult = new DiffRowValue(
                List.of(
                        new DiffFieldValue<>(new IntegerField(ID), null, PK_VALUE, INSERTED),
                        new DiffFieldValue<>(new StringField(CODE), null, null, INSERTED)
                ),
                INSERTED);
        assertCalculateResult(calculator, expectedResult);
    }

    @Test
    public void testCalculate_insertAndUpdate_allFieldsChanged() {
        DiffRowValueCalculator calculator = new DiffRowValueCalculator(insertedPurpleA, updatedNullToEE82EE, Set.of(CODE, NAME, HEX));
        assertCalculateResult(calculator, inserted);
    }

    @Test
    public void testCalculate_insertAndUpdate_reverse() {
        DiffRowValueCalculator calculator = new DiffRowValueCalculator(insertedPurpleA, updatedVioletAToVioletB, Set.of(), true);
        assertCalculateResult(calculator, updatedPurpleAToVioletA);
    }

    // INSERT + DELETE

    @Test
    public void testCalculate_insertAndDelete_annihilated() {
        DiffRowValueCalculator calculator = new DiffRowValueCalculator(insertedPurpleA, deletedVioletA, Set.of());
        assertAnnihilated(calculator);
    }

    @Test
    public void testCalculate_insertAndDelete_reverse() {
        DiffRowValueCalculator calculator = new DiffRowValueCalculator(insertedPurpleA, deletedVioletA, Set.of(), true);
        assertCalculateResult(calculator, updatedPurpleAToVioletA);
    }

    // UPDATE + INSERT

    @Test
    public void testCalculate_updateAndInsert() {
        DiffRowValueCalculator calculator = new DiffRowValueCalculator(updatedVioletAToVioletB, insertedPurpleA, Set.of());
        DiffRowValue expectedResult = new DiffRowValue(
                List.of(
                        new DiffFieldValue<>(new IntegerField(ID), null, PK_VALUE, null),
                        new DiffFieldValue<>(new StringField(CODE), VIOLET, PURPLE, UPDATED),
                        new DiffFieldValue<>(new StringField(NAME), null, A, null)
                ),
                UPDATED);
        assertCalculateResult(calculator, expectedResult);
    }

    @Test
    public void testCalculate_updateAndInsert_sameNewValues() {
        DiffRowValueCalculator calculator = new DiffRowValueCalculator(updatedNullBToPurpleA, insertedPurpleA, Set.of());
        assertCalculateResult(calculator, updatedNullBToPurpleA);
    }

    @Test
    public void testCalculate_updateAndInsert_sameOldValues_annihilated() {
        DiffRowValueCalculator calculator = new DiffRowValueCalculator(updatedPurpleAToVioletA, insertedPurpleA, Set.of());
        assertAnnihilated(calculator);
    }

    @Test
    public void testCalculate_updateAndInsert_fieldsChanged() {
        DiffRowValueCalculator calculator = new DiffRowValueCalculator(updatedPurpleNullToNullEE82EE, insertedVioletA, Set.of(NAME, HEX));
        DiffRowValue expectedResult = new DiffRowValue(
                List.of(
                        new DiffFieldValue<>(new IntegerField(ID), null, PK_VALUE, null),
                        new DiffFieldValue<>(new StringField(CODE), PURPLE, VIOLET, UPDATED)
                ),
                UPDATED);
        assertCalculateResult(calculator, expectedResult);
    }

    @Test
    public void testCalculate_updateAndInsert_allFieldsChanged_annihilated() {
        DiffRowValueCalculator calculator = new DiffRowValueCalculator(updatedPurpleAToVioletA, insertedEE82EE, Set.of(NAME, CODE, HEX));
        assertAnnihilated(calculator);
    }

    @Test
    public void testCalculate_updateAndInsert_reverse() {
        DiffRowValueCalculator calculator = new DiffRowValueCalculator(updatedPurpleAToVioletA, insertedPurpleA, Set.of(), true);
        assertCalculateResult(calculator, deletedVioletA);
    }

    // UPDATE + UPDATE

    @Test
    public void testCalculate_updateAndUpdate() {
        DiffRowValueCalculator calculator = new DiffRowValueCalculator(updatedPurpleAToVioletA, updatedVioletAToVioletB, Set.of());
        assertCalculateResult(calculator, updatedPurpleAToVioletB);
    }

    @Test
    public void testCalculate_updateAndUpdate_sameNewValues_annihilated() {
        DiffRowValueCalculator calculator = new DiffRowValueCalculator(updatedPurpleAToVioletA, updatedNullBToPurpleA, Set.of());
        assertAnnihilated(calculator);
    }

    @Test
    public void testCalculate_updateAndUpdate_fieldsChanged() {
        DiffRowValueCalculator calculator = new DiffRowValueCalculator(updatedPurpleNullToNullEE82EE, updatedNullToEE82EE, Set.of(CODE, NAME));
        assertCalculateResult(calculator, updatedNullToEE82EE);
    }

    @Test
    public void testCalculate_updateAndUpdate_fieldsChanged_annihilated() {
        DiffRowValueCalculator calculator = new DiffRowValueCalculator(updatedPurpleEE82EEToNullEE82EE, updatedNullToEE82EE, Set.of(CODE, NAME));
        assertAnnihilated(calculator);
    }

    @Test
    public void testCalculate_updateAndUpdate_allFieldsChanged_annihilated() {
        DiffRowValueCalculator calculator = new DiffRowValueCalculator(updatedPurpleAToVioletA, updatedNullToEE82EE, Set.of(CODE, NAME, HEX));
        assertAnnihilated(calculator);
    }

    @Test
    public void testCalculate_updateAndUpdate_reverse() {
        DiffRowValueCalculator calculator = new DiffRowValueCalculator(updatedVioletAToVioletB, updatedPurpleAToVioletA, Set.of(), true);
        DiffRowValue expectedResult = new DiffRowValue(
                List.of(
                        new DiffFieldValue<>(new IntegerField(ID), null, PK_VALUE, null),
                        new DiffFieldValue<>(new StringField(CODE), VIOLET, PURPLE, UPDATED),
                        new DiffFieldValue<>(new StringField(NAME), B, A, UPDATED)
                ),
                UPDATED);
        assertCalculateResult(calculator, expectedResult);
    }

    // UPDATE + DELETE

    @Test
    public void testCalculate_updateAndDelete() {
        DiffRowValueCalculator calculator = new DiffRowValueCalculator(updatedPurpleAToVioletB, deletedVioletA, Set.of());
        assertCalculateResult(calculator, deletedPurpleA);
    }

    @Test
    public void testCalculate_updateAndDelete_sameNewValues() {
        DiffRowValueCalculator calculator = new DiffRowValueCalculator(updatedPurpleAToVioletA, deletedVioletA, Set.of());
        assertCalculateResult(calculator, deletedPurpleA);
    }

    @Test
    public void testCalculate_updateAndDelete_fieldsChanged() {
        DiffRowValueCalculator calculator = new DiffRowValueCalculator(updatedPurpleNullToNullEE82EE, deletedPurpleB, Set.of(NAME, HEX));
        assertCalculateResult(calculator, deletedPurple);
    }

    @Test
    public void testCalculate_updateAndDelete_allFieldsChanged() {
        DiffRowValueCalculator calculator = new DiffRowValueCalculator(updatedPurpleToViolet, deletedEE82EE, Set.of(NAME, CODE, HEX));
        assertCalculateResult(calculator, deleted);
    }

    @Test
    public void testCalculate_updateAndDelete_reverse() {
        DiffRowValueCalculator calculator = new DiffRowValueCalculator(updatedVioletAToVioletB, deletedVioletA, Set.of(), true);
        DiffRowValue expectedResult = new DiffRowValue(
                List.of(
                        new DiffFieldValue<>(new IntegerField(ID), null, PK_VALUE, null),
                        new DiffFieldValue<>(new StringField(CODE), null, VIOLET, null),
                        new DiffFieldValue<>(new StringField(NAME), B, A, UPDATED)
                ),
                UPDATED);
        assertCalculateResult(calculator, expectedResult);
    }

    // DELETE + INSERT

    @Test
    public void testCalculate_deleteAndInsert() {
        DiffRowValueCalculator calculator = new DiffRowValueCalculator(deletedVioletA, insertedPurpleA, Set.of());
        DiffRowValue expectedResult = new DiffRowValue(
                List.of(
                        new DiffFieldValue<>(new IntegerField(ID), null, PK_VALUE, null),
                        new DiffFieldValue<>(new StringField(CODE), VIOLET, PURPLE, UPDATED),
                        new DiffFieldValue<>(new StringField(NAME), null, A, null)
                ),
                UPDATED);
        assertCalculateResult(calculator, expectedResult);
    }

    @Test
    public void testCalculate_deleteAndInsert_sameNewValues_annihilated() {
        DiffRowValueCalculator calculator = new DiffRowValueCalculator(deletedVioletA, insertedVioletA, Set.of());
        assertAnnihilated(calculator);
    }

    @Test
    public void testCalculate_deleteAndInsert_fieldsChanged() {
        DiffRowValueCalculator calculator = new DiffRowValueCalculator(deletedPurpleA, insertedVioletA, Set.of());
        assertCalculateResult(calculator, updatedPurpleAToVioletA);
    }

    @Test
    public void testCalculate_deleteAndInsert_allFieldsChanged_annihilated() {
        DiffRowValueCalculator calculator = new DiffRowValueCalculator(deletedVioletA, insertedEE82EE, Set.of(CODE, NAME, HEX));
        assertAnnihilated(calculator);
    }

    @Test
    public void testCalculate_deleteAndInsert_reverse_annihilated() {
        DiffRowValueCalculator calculator = new DiffRowValueCalculator(deletedVioletA, insertedPurpleA, Set.of(), true);
        assertAnnihilated(calculator);
    }

    // DELETE + UPDATE

    @Test
    public void testCalculate_deleteAndUpdate() {
        DiffRowValueCalculator calculator = new DiffRowValueCalculator(deletedVioletA, updatedVioletAToVioletB, Set.of());

        DiffRowValue expectedResult = new DiffRowValue(
                List.of(
                        new DiffFieldValue<>(new IntegerField(ID), null, PK_VALUE, null),
                        new DiffFieldValue<>(new StringField(CODE), null, VIOLET, null),
                        new DiffFieldValue<>(new StringField(NAME), A, B, UPDATED)
                ),
                UPDATED);

        assertCalculateResult(calculator, expectedResult);
    }

    @Test
    public void testCalculate_deleteAndUpdate_sameNewValues_annihilated() {
        DiffRowValueCalculator calculator = new DiffRowValueCalculator(deletedVioletA, updatedPurpleAToVioletA, Set.of());
        assertAnnihilated(calculator);
    }

    @Test
    public void testCalculate_deleteAndUpdate_fieldsChanged() {
        DiffRowValueCalculator calculator = new DiffRowValueCalculator(deletedPurpleEE82EE, updatedNullBToVioletA, Set.of(HEX, NAME));
        assertCalculateResult(calculator, updatedPurpleToViolet);
    }

    @Test
    public void testCalculate_deleteAndUpdate_allFieldsChanged_annihilated() {
        DiffRowValueCalculator calculator = new DiffRowValueCalculator(deletedVioletA, updatedNullToEE82EE, Set.of(CODE, NAME, HEX));
        assertAnnihilated(calculator);
    }

    @Test
    public void testCalculate_deleteAndUpdate_reverse() {
        DiffRowValueCalculator calculator = new DiffRowValueCalculator(deletedVioletA, updatedPurpleAToVioletB, Set.of(), true);
        assertCalculateResult(calculator, insertedPurpleA);
    }


    // DELETE + DELETE

    @Test
    public void testCalculate_deleteAndDelete() {
        DiffRowValueCalculator calculator = new DiffRowValueCalculator(deletedVioletA, deletedPurpleB, Set.of());
        assertCalculateResult(calculator, deletedVioletA);
    }

    @Test
    public void testCalculate_deleteAndDelete_sameNewValues() {
        DiffRowValueCalculator calculator = new DiffRowValueCalculator(deletedVioletA, deletedVioletA, Set.of());
        assertCalculateResult(calculator, deletedVioletA);
    }

    @Test
    public void testCalculate_deleteAndDelete_fieldsChanged() {
        DiffRowValueCalculator calculator = new DiffRowValueCalculator(deletedPurpleEE82EE, deletedPurpleB, Set.of(NAME, HEX));
        assertCalculateResult(calculator, deletedPurple);
    }

    @Test
    public void testCalculate_deleteAndDelete_allFieldsChanged() {
        DiffRowValueCalculator calculator = new DiffRowValueCalculator(deletedVioletA, deletedEE82EE, Set.of(CODE, NAME, HEX));
        assertCalculateResult(calculator, deleted);
    }

    @Test
    public void testCalculate_deleteAndDelete_reverse() {
        DiffRowValueCalculator calculator = new DiffRowValueCalculator(deletedPurpleB, deletedVioletA, Set.of(), true);
        assertCalculateResult(calculator, insertedVioletA);
    }

    // SINGLE

    @Test
    public void testCalculate_singleRandomDiff() {
        DiffRowValue diffRowValue = getRandomDiffRowValue(insertedPurpleA, updatedPurpleAToVioletA, deletedVioletA);
        DiffRowValueCalculator calculator = new DiffRowValueCalculator(diffRowValue, null, Set.of());
        assertCalculateResult(calculator, diffRowValue);
    }

    @Test
    public void testCalculate_singleInsertOrDelete_allFieldsChanged() {
        DiffRowValue diffRowValue = getRandomDiffRowValue(insertedPurpleA, deletedEE82EE);
        DiffStatusEnum status = diffRowValue.getStatus();

        DiffRowValueCalculator calculator = new DiffRowValueCalculator(diffRowValue, null, Set.of(CODE, NAME, HEX));
        DiffRowValue expectedResult = (status == INSERTED ? inserted : deleted);
        assertCalculateResult(calculator, expectedResult);
    }

    @Test
    public void testCalculate_singleInsertOrDelete_fieldsChanged() {
        DiffRowValue diffRowValue = getRandomDiffRowValue(insertedPurpleA, deletedPurpleB);
        DiffStatusEnum status = diffRowValue.getStatus();
        DiffRowValueCalculator calculator = new DiffRowValueCalculator(diffRowValue, null, Set.of(NAME));
        DiffRowValue expectedResult = (status == INSERTED ? insertedPurple : deletedPurple);
        assertCalculateResult(calculator, expectedResult);
    }

    @Test
    public void testCalculate_singleUpdate_fieldsChanged() {
        DiffRowValueCalculator calculator = new DiffRowValueCalculator(updatedPurpleAToVioletA, null, Set.of(NAME));
        assertCalculateResult(calculator, updatedPurpleToViolet);
    }

    @Test
    public void testCalculate_singleUpdate_allFieldsChanged_annihilated() {
        DiffRowValueCalculator calculator = new DiffRowValueCalculator(updatedPurpleAToVioletA, null, Set.of(CODE, NAME));
        assertAnnihilated(calculator);
    }

    // SINGLE REVERSE

    @Test
    public void testCalculate_singleInsertDiff_reverse() {
        DiffRowValueCalculator calculator = new DiffRowValueCalculator(insertedPurpleA, null, Set.of(), true);
        assertCalculateResult(calculator, deletedPurpleA);
    }

    @Test
    public void testCalculate_singleUpdateDiff_reverse() {
        DiffRowValueCalculator calculator = new DiffRowValueCalculator(updatedPurpleAToVioletA, null, Set.of(), true);
        DiffRowValue expectedResult = new DiffRowValue(
                List.of(
                        new DiffFieldValue<>(new IntegerField(ID), null, PK_VALUE, null),
                        new DiffFieldValue<>(new StringField(CODE), VIOLET, PURPLE, UPDATED),
                        new DiffFieldValue<>(new StringField(NAME), null, A, null)
                ),
                UPDATED);
        assertCalculateResult(calculator, expectedResult);
    }

    @Test
    public void testCalculate_singleDeleteDiff_reverse() {
        DiffRowValueCalculator calculator = new DiffRowValueCalculator(deletedVioletA, null, Set.of(), true);
        assertCalculateResult(calculator, insertedVioletA);
    }

    @Test
    public void testCalculate_singleInsert_allFieldsChanged_reverse() {
        DiffRowValueCalculator calculator = new DiffRowValueCalculator(insertedPurpleA, null, Set.of(CODE, NAME, HEX), true);
        assertCalculateResult(calculator, deleted);
    }

    @Test
    public void testCalculate_singleDelete_allFieldsChanged_reverse() {
        DiffRowValueCalculator calculator = new DiffRowValueCalculator(deletedPurpleEE82EE, null, Set.of(CODE, NAME, HEX), true);
        assertCalculateResult(calculator, inserted);
    }

    @Test
    public void testCalculate_singleInsert_fieldsChanged_reverse() {
        DiffRowValueCalculator calculator = new DiffRowValueCalculator(insertedPurpleA, null, Set.of(NAME), true);
        assertCalculateResult(calculator, deletedPurple);
    }

    @Test
    public void testCalculate_singleDelete_fieldsChanged_reverse() {
        DiffRowValueCalculator calculator = new DiffRowValueCalculator(deletedPurpleB, null, Set.of(NAME), true);
        assertCalculateResult(calculator, insertedPurple);
    }

    @Test
    public void testCalculate_singleUpdate_allFieldsChanged_reverse_annihilated() {
        DiffRowValueCalculator calculator = new DiffRowValueCalculator(updatedPurpleAToVioletA, null, Set.of(CODE, NAME), true);
        assertAnnihilated(calculator);
    }

    private void assertDiffRowValue(DiffRowValue expected, DiffRowValue actual) {
        Set<String> commonFields = getCommonFields(expected, actual);
        assertEquals(expected.getStatus(), actual.getStatus());
        assertEquals(expected.getValues().size(), actual.getValues().size());
        expected.getValues().forEach(expectedFieldValue -> {
            String fieldName = expectedFieldValue.getField().getName();
            if (commonFields.contains(fieldName)) {
                assertFieldValue(fieldName, expectedFieldValue, actual.getDiffFieldValue(fieldName), false);
                assertFieldValue(fieldName, expectedFieldValue, actual.getDiffFieldValue(fieldName), true);
                assertEquals(expectedFieldValue.getStatus(), actual.getDiffFieldValue(fieldName).getStatus());
            }
        });
    }

    private Set<String> getCommonFields(DiffRowValue expected, DiffRowValue actual) {
        Set<String> expectedFields = expected.getValues().stream().map(DiffFieldValue::getField).map(Field::getName).collect(Collectors.toSet());
        Set<String> actualFields = expected.getValues().stream().map(DiffFieldValue::getField).map(Field::getName).collect(Collectors.toSet());
        expectedFields.retainAll(actualFields);
        return expectedFields;
    }

    private void assertCalculateResult(DiffRowValueCalculator calculator, DiffRowValue expectedResult) {
        assertFalse(calculator.isAnnihilated());
        DiffRowValue actualResult = calculator.calculate();
        assertDiffRowValue(expectedResult, actualResult);
    }

    private void assertAnnihilated(DiffRowValueCalculator calculator) {
        assertTrue(calculator.isAnnihilated());
        assertNull(calculator.calculate());
    }

    private void assertFieldValue(String fieldName, DiffFieldValue expected, DiffFieldValue actual, boolean isNew) {
        Object expectedNewValue = isNew ? expected.getNewValue() : expected.getOldValue();
        Object actualNewValue = isNew ? actual.getNewValue() : actual.getOldValue();

        assertTrue("Field '" + fieldName + "'(" + (isNew ? "new" : "old") + " value). " +
                        "Expected: " + expectedNewValue + ". Actual: " + actualNewValue,
                same(expectedNewValue, actualNewValue));
    }

    private boolean same(Object a, Object b) {
        return (a == null && b == null) || (a != null && a.equals(b));
    }

    private DiffRowValue getRandomDiffRowValue(DiffRowValue... rows) {
        Random rand = new Random();
        return Arrays.asList(rows).get(rand.nextInt(rows.length));
    }
}
