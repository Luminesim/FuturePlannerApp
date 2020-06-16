package com.example.myfirstapp.db;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverter;
import androidx.room.TypeConverters;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.myfirstapp.Category;
import com.example.myfirstapp.monad.MonadData;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@androidx.room.Database(entities = {EntityFact.class, Entity.class, EntityFactDetail.class}, version = 1, exportSchema = false)
@TypeConverters(Converters.class)
public abstract class EntityDatabase extends RoomDatabase {
    public abstract EntityDao entityDao();

    private static volatile EntityDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;
    static final ExecutorService databaseWriteExecutor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    public static EntityDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (EntityDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            EntityDatabase.class,
                            "entity_database")
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
