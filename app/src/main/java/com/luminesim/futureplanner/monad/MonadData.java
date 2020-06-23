package com.luminesim.futureplanner.monad;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * Data required to reconstruct a monad and compute or compose its value.
 */
@Getter
@NoArgsConstructor
public class MonadData {

    @Getter
    private String monadId;

    private String[] parameters;

    private static ObjectMapper serializer;

    private static ObjectMapper getSerializer() {
        // Set up the serializer, if needed.
        if (serializer == null) {
            serializer = new ObjectMapper();
            serializer.registerModule(new JavaTimeModule());
            serializer.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            serializer.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        }
        return serializer;
    }

    public MonadData(@NonNull String monadId, @NonNull Object... parameters) {

        this.monadId = monadId;
        this.parameters = new String[parameters.length];
        try {
            for (int i = 0; i < parameters.length; i += 1) {
                this.parameters[i] = getSerializer().writeValueAsString(parameters[i]);
            }
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public String toJson() {
        try {
            return getSerializer().writeValueAsString(this);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    /**
     * Gets JSON parameter strings cast to the given types.
     *
     * @param types
     * @return
     * @pre types.length == parameters.length
     */
    public Object[] getParameters(@NonNull Class<?>[] types, boolean addQuotes) {
        // Sanity check.
        if (types.length != parameters.length) {
            throw new IllegalArgumentException(String.format("Have %s parameters but was only given %s types.", parameters.length, types.length));
        }
        String delim = "\"";
        if (!addQuotes) {
            delim = "";
        }
        Object[] params = new Object[parameters.length];
        try {
            for (int i = 0; i < parameters.length; i += 1) {
                Object value = getSerializer().readValue(delim + parameters[i] + delim, types[i]);
                if (value == null) {
                    throw new RuntimeException("Null value returned for parameter " + parameters[i] + " and type " + types[i].getName());
                }
                params[i] = value;
            }
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
        return params;
    }

    /**
     * Gets the JSON parameter strings stored in this instance.
     * @apiNote This is required to persist the object.
     * @return
     */
    public String[] getParameters() {
        return parameters;
    }

    public static MonadData fromJson(String json) throws IOException {
        return getSerializer().readValue(json, MonadData.class);
    }
}
