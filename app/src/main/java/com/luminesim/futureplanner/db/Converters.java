package com.luminesim.futureplanner.db;

import androidx.annotation.NonNull;
import androidx.room.TypeConverter;

import com.luminesim.futureplanner.Category;

public class Converters {

    @TypeConverter
    public Category fromCategoryName(@NonNull String name) {
        return Category.valueOf(name);
    }

    @TypeConverter
    public String toCategoryName(@NonNull Category category) {
        return category.name();
    }
}
