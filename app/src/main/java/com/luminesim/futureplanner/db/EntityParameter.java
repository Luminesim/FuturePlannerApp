package com.luminesim.futureplanner.db;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@androidx.room.Entity(tableName = "entity_parameters")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@ForeignKey(entity = com.luminesim.futureplanner.db.Entity.class, parentColumns = "uid", childColumns = "entity_uid")
public class EntityParameter {

    public EntityParameter(@NonNull String name, @NonNull String value) {
        this.name = name;
        this.value = value;
    }

    /**
     * Our UID.
     */
    @PrimaryKey(autoGenerate = true)
    private long uid;

    /**
     * The UID of the entity using this parameter.
     */
    @ColumnInfo(name="entity_uid")
    @NonNull
    private long entityUid;

    /**
     * The name of the parameter.
     */
    @NonNull
    private String name;

    /**
     * The value of the parameter.
     */
    @NonNull
    private String value;
}
