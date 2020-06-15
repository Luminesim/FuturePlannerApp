package com.example.myfirstapp.db;

import androidx.room.Embedded;
import androidx.room.Relation;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
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

    public List<EntityFactWithDetails> getFacts() {
        if (facts == null) {
            return new ArrayList<>();
        }
        else {
            return facts;
        }
    }
}
