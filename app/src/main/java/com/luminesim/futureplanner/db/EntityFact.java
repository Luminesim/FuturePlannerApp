package com.luminesim.futureplanner.db;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

import com.luminesim.futureplanner.Category;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * A fact about an entity.
 * E.g. the entity has an income source named "Work"
 */
@Entity(tableName = "entity_facts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@ForeignKey(entity = com.luminesim.futureplanner.db.Entity.class, parentColumns = "uid", childColumns = "entity_uid")
public class EntityFact {

    /**
     * Our UID.
     */
    @PrimaryKey(autoGenerate = true)
    private long uid;

    @ColumnInfo(name="entity_uid")
    @NonNull
    private Long entityUid;

//    @ColumnInfo(name="parent_entity_detail_uid")
//    private Long parentDetailUid;

    /**
     * The use of this knowledge, e.g. for income calculation
     * @see Category
     */
    @ColumnInfo(name = "category")
    @NonNull
    private Category category;

    /**
     * The (possibly non-unique) name of the knowledge, e.g. "Travel"
     */
    @ColumnInfo(name = "name")
    @NonNull
    private String name;
}
