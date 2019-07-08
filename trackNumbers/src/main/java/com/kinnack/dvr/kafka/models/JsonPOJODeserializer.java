package com.kinnack.dvr.kafka.models;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;

import java.util.Map;

public class JsonPOJODeserializer<T> implements Deserializer<T> {
    public static final String JSON_POJO_CLASS = "JsonPOJOClass";
    public static final String JSON_POJO_TYPE = "JsonPOJOType";
    private ObjectMapper objectMapper = new ObjectMapper();

    private Class<T> tClass;
    private TypeReference<T> tTypeReference;

    /**
     * Default constructor needed by Kafka
     */
    public JsonPOJODeserializer() {
        objectMapper.findAndRegisterModules();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void configure(Map<String, ?> props, boolean isKey) {
        tClass = (Class<T>) props.get(JSON_POJO_CLASS);
        tTypeReference = (TypeReference<T>) props.get(JSON_POJO_TYPE);
    }

    @Override
    public T deserialize(String topic, byte[] bytes) {
        if (tClass == null && tTypeReference == null) {
            throw new IllegalStateException("Please call configure with a map that contains JSON_POJO_CLASS or JSON_POJO_TYPE set to the class of T");
        }
        if (bytes == null)
            return null;

        T data;
        try {
            if (tClass != null) {
                data = objectMapper.readValue(bytes, tClass);
            } else {
                data = objectMapper.readValue(bytes, tTypeReference);
            }

        } catch (Exception e) {
            throw new SerializationException(e);
        }

        return data;
    }

    @Override
    public void close() {

    }
}
