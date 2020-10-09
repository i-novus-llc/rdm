package ru.i_novus.ms.rdm.loader.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static java.util.Collections.emptyList;
import static org.springframework.util.StringUtils.isEmpty;

public class RefBookDataUtil {

    private static final ObjectMapper jsonMapper = new ObjectMapper();

    private RefBookDataUtil() {
        // Nothing to do.
    }

    public static List<RefBookDataModel> toRefBookDataModels(Resource file) {

        if (file == null || isEmpty(file.getFilename()))
            return emptyList();

        String extension = getExtension(file.getFilename());
        if (!"json".equals(extension))
            return List.of(new RefBookDataModel(null, null, null, file));

        return toRefBookDataModels(fromFile(file));
    }

    private static String getExtension(String filename) {

        int lastIndex = filename.lastIndexOf('.');
        return (lastIndex >= 0) ? filename.substring(lastIndex + 1) : null;
    }

    private static JsonNode fromFile(Resource file) {
        try {
            return jsonMapper.readTree(file.getFile());

        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot deserialize json from file.", e);

        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot read file.", e);
        }
    }

    private static List<RefBookDataModel> toRefBookDataModels(JsonNode rootNode) {

        if (rootNode == null || rootNode.isEmpty())
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

    @SuppressWarnings("java:S4449")
    private static RefBookDataModel toRefBookDataModel(JsonNode jsonNode) {

        String code = getByKey(jsonNode, "code", JsonNode::asText);

        String filePath = getByKey(jsonNode,"file", JsonNode::asText);
        if (!isEmpty(filePath)) {

            return new RefBookDataModel(
                    code,
                    getByKey(jsonNode, "name", JsonNode::asText),
                    getByKey(jsonNode, "structure", RefBookDataUtil::asJsonString),
                    new ClassPathResource(filePath)
            );
        }

        if (isEmpty(code))
            return null;

        return new RefBookDataModel(
                code,
                getByKey(jsonNode, "name", JsonNode::asText),
                getByKey(jsonNode, "structure", RefBookDataUtil::asJsonString),
                getByKey(jsonNode, "data", RefBookDataUtil::asJsonString)
        );
    }

    private static String asJsonString(JsonNode value) {
        try {
            return jsonMapper.writeValueAsString(value);

        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private static <T> T getByKey(JsonNode node, String key, Function<JsonNode, T> valueExtractor) {

        JsonNode valueJson = node.get(key);
        return (valueJson == null) ? null : valueExtractor.apply(valueJson);
    }
}
