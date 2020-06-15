package com.example.myfirstapp.db;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

import com.example.myfirstapp.CategoryContentsListActivity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Detail about facts about an entity. These are required to act
 * on the fact and calculate values or events.
 * E.g. the entity has income from Work.
 * Work pays $100K. It pays this yearly. Work started in December of last year.
 */
@Entity(tableName = "entity_fact_details")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@ForeignKey(entity = com.example.myfirstapp.db.Entity.class, parentColumns = "uid", childColumns = "entity_fact_id")
public class EntityFactDetail {

    /**
     * Our primary key.
     */
    @PrimaryKey(autoGenerate = true)
    private long uid;

    @ColumnInfo(name="entity_fact_uid")
    @NonNull
    private long entityFactUid;

    /**
     * Used to ensure fact details are presented in the right order (for
     * when order matters).
     */
    @ColumnInfo(name = "step_number")
    @NonNull
    private int stepNumber;

    /**
     * Monad data, in JSON format.
     */
    @ColumnInfo(name = "monad_json")
    @NonNull
    private String monadJson;
}
