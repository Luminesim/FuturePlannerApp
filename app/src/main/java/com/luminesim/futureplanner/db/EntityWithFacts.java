package com.luminesim.futureplanner.db;

import androidx.room.Embedded;
import androidx.room.Relation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class EntityWithFacts {
    @Embedded
    private Entity entity;

    @Relation(
            entity = EntityFact.class,
            parentColumn = "uid",
            entityColumn = "entity_uid"
    )
    private List<EntityFactWithDetails> facts;

    @Relation(
            entity = EntityParameter.class,
            parentColumn = "uid",
            entityColumn = "entity_uid"
    )
    private List<EntityParameter> parameters;

    public List<EntityFactWithDetails> getFacts() {
        if (facts == null) {
            return new ArrayList<>();
        }
        else {
            return facts;
        }
    }

    public List<EntityParameter> getParameters() {
        if (parameters == null) {
            return new ArrayList<>();
        }
        else {
            return parameters;
        }
    }

    /**
     *
     * @param name
     * @return
     *  The parameter with the given name, or an empty optional if none.
     */
    public Optional<String> getParameter(@NonNull String name) {
        return getParameters().stream().filter(ep -> ep.getName().equals(name)).map(ep -> ep.getValue()).findFirst();
    }
}
