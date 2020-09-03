package ru.i_novus.ms.rdm.l10n.api.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import static org.springframework.util.StringUtils.isEmpty;

@ApiModel("Сведения о локали для версии")
public class L10nVersionLocale {

    @ApiModelProperty("Идентификатор версии")
    private Integer versionId;

    @ApiModelProperty("Код локали")
    private String localeCode;

    @ApiModelProperty("Наименование локали")
    private String localeName;

    @ApiModelProperty("Наименование локали в самой локали")
    private String localeSelfName;

    public L10nVersionLocale() {
        // Nothing to do.
    }

    public Integer getVersionId() {
        return versionId;
    }

    public void setVersionId(Integer versionId) {
        this.versionId = versionId;
    }

    public String getLocaleCode() {
        return localeCode;
    }

    public void setLocaleCode(String localeCode) {
        this.localeCode = localeCode;
    }

    public String getLocaleName() {
        return localeName;
    }

    public void setLocaleName(String localeName) {
        this.localeName = localeName;
    }

    public String getLocaleSelfName() {
        return localeSelfName;
    }

    public void setLocaleSelfName(String localeSelfName) {
        this.localeSelfName = localeSelfName;
    }

    public String getLocaleFullName() {
        return isEmpty(localeSelfName) ? localeName : String.format("%s (%s)", localeName, localeSelfName);
    }
}
