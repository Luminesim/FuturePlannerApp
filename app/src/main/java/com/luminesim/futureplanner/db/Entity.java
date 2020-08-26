package com.luminesim.futureplanner.db;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.PrimaryKey;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * An entity -- person, asset, staff, etc.
 */
@androidx.room.Entity(tableName = "entities")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Entity {
    @PrimaryKey(autoGenerate = true)
    private long uid;

    @ColumnInfo(name = "name")
    @NonNull
    private String name;

    @NonNull
    private String type;
}
