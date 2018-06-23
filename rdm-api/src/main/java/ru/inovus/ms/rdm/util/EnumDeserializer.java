package ru.inovus.ms.rdm.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import ru.inovus.ms.rdm.service.Identifiable;

import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.Arrays;
import java.util.function.Predicate;

@Provider
public abstract class EnumDeserializer<E extends Enum<E> & Identifiable> extends StdDeserializer<E> {

    private static final long serialVersionUID = -4838851382671458574L;

    protected EnumDeserializer(Class<E> vc) {
        super(vc);
    }

    protected JsonNode getIdentifiableAttribute(JsonNode jsonNode) {
        return jsonNode.get("id");
    }

    protected Predicate<Identifiable> getPredicate(JsonNode jsonNode) {
        return v -> v.getId() == jsonNode.asInt();
    }

    @SuppressWarnings("unchecked")
    public E deserialize(JsonParser jp, DeserializationContext dc) throws IOException {

        final JsonNode jsonNode = jp.readValueAsTree();

        JsonNode id = getIdentifiableAttribute(jsonNode);
        if (id.isNull())
            return null;

        return Arrays.stream((E[]) this.handledType().getEnumConstants()).filter(getPredicate(id))
                .findAny().orElseThrow(() ->
                        dc.mappingException("Cannot deserialize " + this.handledType() + " from id " + id.asInt()));
    }
}
