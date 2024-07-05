package ru.i_novus.ms.rdm.loader.client.loader;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import ru.i_novus.ms.rdm.loader.client.loader.model.RefBookDataModel;
import ru.i_novus.ms.rdm.loader.client.loader.model.RefBookDataUpdateTypeEnum;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static java.util.Collections.emptyList;
import static ru.i_novus.ms.rdm.loader.client.loader.model.RefBookDataUpdateTypeEnum.CREATE_ONLY;

public final class RefBookDataUtil {

    private static final char FILE_NAME_EXT_SEPARATOR = '.';

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private RefBookDataUtil() {
        // Nothing to do.
    }

    public static boolean isEmpty(String s) {
        return s == null || s.isEmpty();
    }

    public static List<RefBookDataModel> toRefBookDataModels(Resource file) {

        if (file == null || isEmpty(file.getFilename()))
            return emptyList();

        final String extension = getExtension(file.getFilename());
        if (!"json".equals(extension))
            return List.of(new RefBookDataModel(file));

        return toRefBookDataModels(fileToJsonNode(file));
    }

    private static String getExtension(String filename) {

        if (isEmpty(filename))
            return null;

        int lastIndex = filename.lastIndexOf(FILE_NAME_EXT_SEPARATOR);
        return (lastIndex >= 0) ? filename.substring(lastIndex + 1) : null;
    }

    private static JsonNode fileToJsonNode(Resource file) {
        try {
            return OBJECT_MAPPER.readTree(file.getInputStream());

        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot deserialize json from file.", e);

        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot read file.", e);
        }
    }

    private static List<RefBookDataModel> toRefBookDataModels(JsonNode rootNode) {

        if (isEmptyNode(rootNode))
            return emptyList();

        if (!rootNode.isArray()) {
            RefBookDataModel model = toRefBookDataModel(rootNode);
            return (model != null) ? List.of(model) : emptyList();
        }

        List<RefBookDataModel> result = new ArrayList<>(rootNode.size());
        for (JsonNode jsonNode : rootNode) {
            RefBookDataModel model = toRefBookDataModel(jsonNode);
            if (model != null) {
                result.add(toRefBookDataModel(jsonNode));
            }
        }

        return result;
    }

    //@SuppressWarnings("java:S4449")
    private static RefBookDataModel toRefBookDataModel(JsonNode jsonNode) {

        if (isEmptyNode(jsonNode))
            return null;

        final String code = getByKey(jsonNode, "code", JsonNode::asText);
        final String filePath = getByKey(jsonNode,"file", JsonNode::asText);
        if (isEmpty(code) && isEmpty(filePath))
            return null; // Ошибка записи: код справочника обязателен!

        final RefBookDataModel model = new RefBookDataModel();
        model.setChangeSetId(getByKey(jsonNode, "change_set_id", JsonNode::asText));
        model.setUpdateType(getUpdateType(jsonNode));

        model.setCode(code);
        model.setName(getByKey(jsonNode, "name", JsonNode::asText));
        model.setStructure(getByKey(jsonNode, "structure", RefBookDataUtil::asJsonString));

        if (!isEmpty(filePath)) {
            model.setFile(new ClassPathResource(filePath));
        } else {
            model.setData(getByKey(jsonNode, "data", RefBookDataUtil::asJsonString));
        }

        return model;
    }

    private static RefBookDataUpdateTypeEnum getUpdateType(JsonNode jsonNode) {

        final String updateType = getByKey(jsonNode, "update_type", JsonNode::asText);
        return RefBookDataUpdateTypeEnum.fromValue(updateType, CREATE_ONLY);
    }

    private static boolean isEmptyNode(JsonNode jsonNode) {

        return jsonNode == null || jsonNode.isEmpty();
    }

    private static String asJsonString(JsonNode value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);

        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private static <T> T getByKey(JsonNode node, String key, Function<JsonNode, T> valueExtractor) {

        JsonNode valueJson = node.get(key);
        return (valueJson == null) ? null : valueExtractor.apply(valueJson);
    }
}
