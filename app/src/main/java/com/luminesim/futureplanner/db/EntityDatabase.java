package com.luminesim.futureplanner.db;

import android.content.Context;

import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

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
