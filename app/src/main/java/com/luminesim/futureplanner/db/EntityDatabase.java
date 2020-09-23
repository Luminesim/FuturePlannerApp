package com.luminesim.futureplanner.db;

import android.content.Context;

import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@androidx.room.Database(entities = {EntityFact.class, Entity.class, EntityFactDetail.class, EntityParameter.class}, version = 4)
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
                            .addMigrations(MIGRATION_3_4)
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Convenience method.
     * @param params
     * @return
     */
    private static Object[] params(Object... params) {
        return params;
    }

    public static final Migration MIGRATION_3_4 = new Migration(3,4) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {

            // Nuked 2 monads
            database.execSQL("UPDATE `entity_fact_details` SET `monad_json` = REPLACE(`monad_json`,'IdPercentRateToRate','IdPercentDeduction')");
            database.execSQL("UPDATE `entity_fact_details` SET `monad_json` = REPLACE(`monad_json`,'IdPercentAdditionRateToRate','IdPercentAddition')");

        }
    };
}
