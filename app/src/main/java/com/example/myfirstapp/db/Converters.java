package com.example.myfirstapp.db;

import androidx.annotation.NonNull;
import androidx.room.TypeConverter;

import com.example.myfirstapp.Category;

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
