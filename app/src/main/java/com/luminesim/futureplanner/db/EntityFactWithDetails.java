package com.luminesim.futureplanner.db;

import androidx.room.Embedded;
import androidx.room.Relation;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Used to collect details for each entity facts.
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class EntityFactWithDetails {
    @Embedded
    private EntityFact fact;

    @Relation(
            parentColumn = "uid",
            entityColumn = "entity_fact_uid")
    private List<EntityFactDetail> details;
}
