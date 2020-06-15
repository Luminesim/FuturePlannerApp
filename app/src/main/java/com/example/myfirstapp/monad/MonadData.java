package com.example.myfirstapp.monad;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Data required to reconstruct a monad and compute or compose its value.
 */
@Getter
@NoArgsConstructor
public class MonadData {
    private String monadId;
    private Object[] parameters;

    public MonadData(@NonNull String monadId, @NonNull Object... parameters) {
        this.monadId = monadId;
        this.parameters = parameters;
    }

    public String toJson() {
        ObjectMapper out = new ObjectMapper();
        try {
            return out.writeValueAsString(this);
        }
        catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static MonadData fromJson(String json) throws IOException {
        ObjectMapper om = new ObjectMapper();
        return om.readValue(json, MonadData.class);
    }
}
