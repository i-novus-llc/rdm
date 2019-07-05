package ru.inovus.ms.rdm.repositiory;

class RefBookVersionConstants {

    /*
     * Ссылающийся справочник должен иметь:
     *   1) структуру,
     *   2) первичный ключ,
     *   3) ссылку на указанный справочник.
     *
     */
    static final String FIND_REFERRER_VERSIONS = "select distinct bv.* \n" +
            "  from n2o_rdm_management.ref_book_version bv \n" +
            " cross join lateral \n" +
            "       jsonb_to_recordset(bv.\"structure\" -> 'attributes') \n" +
            "           as akey(\"code\" varchar, \"isPrimary\" bool) \n" +
            " cross join lateral \n" +
            "       jsonb_to_recordset(bv.\"structure\" -> 'attributes') \n" +
            "           as aref(\"type\" varchar, \"referenceCode\" varchar) \n" +
            " where bv.\"structure\" is not null \n" +
            "   and (bv.\"structure\" -> 'attributes') is not null \n" +
            "   and akey.\"isPrimary\" = true \n" +
            "   and aref.\"type\" = 'REFERENCE' \n" +
            "   and aref.\"referenceCode\" = :refBookCode \n";

    static final String WHERE_REF_BOOK_STATUS = "   and ( \n" +
            "       (:refBookStatus = 'ALL') or \n" +
            "       exists( \n" +
            "       select 1 \n" +
            "         from n2o_rdm_management.ref_book b \n" +
            "        where b.id = bv.ref_book_id \n" +
            "          and ((:refBookStatus = 'USED' and not b.archived) or \n" +
            "               (:refBookStatus = 'ARCHIVED' and b.archived)) )\n" +
            "       ) \n";

    static final String WHERE_REF_BOOK_SOURCE = "   and ( \n" +
            "       (:refBookSource = 'ALL') or \n" +
            "       (:refBookSource = 'ACTUAL' and bv.status = 'PUBLISHED' and \n" +
            "        bv.from_date <= timezone('utc', now()) and \n" +
            "        (bv.to_date > timezone('utc', now()) or bv.to_date is null)) or \n" +
            "       (:refBookSource = 'DRAFT' and bv.status = 'DRAFT') or \n" +
            // with subquery:
            "       (:refBookSource != 'LAST_PUBLISHED' or \n" +
            "        (:refBookSource = 'LAST_PUBLISHED' and bv.status = 'PUBLISHED')) and \n" +
            "       bv.id = ( \n" +
            "       select lv.id \n" +
            "         from n2o_rdm_management.ref_book_version lv \n" +
            "        where lv.ref_book_id = bv.ref_book_id \n" +
            "          and ( (:refBookSource = 'LAST_VERSION') or \n" +
            "                (:refBookSource = 'LAST_PUBLISHED' and lv.status = bv.status) ) \n" +
            "        order by lv.from_date desc \n" +
            "        limit 1 )\n" +
            "       ) \n";

    private RefBookVersionConstants() {
    }
}
