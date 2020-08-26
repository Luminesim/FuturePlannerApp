package com.luminesim.futureplanner.db;

import androidx.room.Embedded;
import androidx.room.Relation;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Used to collect parameters for each entity.
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class EntityWithParameters {
    @Embedded
    private Entity entity;

    @Relation(
            parentColumn = "uid",
        entityColumn = "entity_uid")
    private List<EntityParameter> parameters;
}
