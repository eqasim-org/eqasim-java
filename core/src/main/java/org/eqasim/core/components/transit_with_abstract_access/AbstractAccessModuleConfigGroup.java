package org.eqasim.core.components.transit_with_abstract_access;

import jakarta.validation.constraints.NotNull;
import org.matsim.core.config.ReflectiveConfigGroup;

public class AbstractAccessModuleConfigGroup extends ReflectiveConfigGroup {

    public static final String ABSTRACT_ACCESS_GROUP_NAME = "abstractAccess";

    private static final String ACCESS_ITEMS_FILE_PATH = "accessItemsFilePath";


    private static final String MODE_NAME = "modeName";

    @NotNull
    private String accessItemsFilePath;

    @NotNull
    private String modeName;

    public AbstractAccessModuleConfigGroup() {
        super(ABSTRACT_ACCESS_GROUP_NAME);
    }

    @StringSetter(ACCESS_ITEMS_FILE_PATH)
    public void setAccessItemsFilePath(String accessItemsFilePath) {
        this.accessItemsFilePath = accessItemsFilePath;
    }

    @StringGetter(ACCESS_ITEMS_FILE_PATH)
    public String getAccessItemsFilePath() {
        return this.accessItemsFilePath;
    }

    @StringSetter(MODE_NAME)
    public void setModeName(String modeName) {
        this.modeName = modeName;
    }

    @StringGetter(MODE_NAME)
    public String getModeName() {
        return this.modeName;
    }
}
