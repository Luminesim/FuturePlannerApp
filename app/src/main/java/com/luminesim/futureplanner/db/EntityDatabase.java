package com.luminesim.futureplanner.db;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.PrimaryKey;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.luminesim.futureplanner.simulation.CanadianIndividualIncomeSimulation;
import com.luminesim.futureplanner.simulation.SimpleIndividualIncomeSimulation;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@androidx.room.Database(entities = {EntityFact.class, Entity.class, EntityFactDetail.class, EntityParameter.class}, version = 3)
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
                            .addMigrations(MIGRATION_1_2)
                            .addMigrations(MIGRATION_2_3)
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


    /**
     * Migrates first to second version.
     */
    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {

            // Entities now have types.
            database.execSQL("ALTER TABLE `entities` ADD COLUMN `type` TEXT DEFAULT '"+CanadianIndividualIncomeSimulation.ENTITY_TYPE+"' NOT NULL");
            database.execSQL("UPDATE `entities` SET `type` = ?", params(CanadianIndividualIncomeSimulation.ENTITY_TYPE));

            // Entities now have parameters.
            database.execSQL(
                    "CREATE TABLE `entity_parameters` ("
                            + "`uid` INTEGER, "
                            + "`entity_uid` LONG, "
                            + "`name` TEXT, "
                            + "`value` TEXT, "
                            + "PRIMARY KEY (`uid`))"
            );

            // Existing entities have default parameters.
            database.execSQL(
                    "INSERT INTO `entity_parameters` (uid, entity_uid, name, value) VALUES "
                            + "(1, (SELECT `uid` FROM `entities` LIMIT 1), ?, 'Saskatchewan'), "
                            + "(2, (SELECT `uid` FROM `entities` LIMIT 1), ?, '10000.00')",
                    new Object[] {CanadianIndividualIncomeSimulation.PARAMETER_PROVINCE, CanadianIndividualIncomeSimulation.PARAMETER_INITIAL_FUNDS});
        }
    };


    /**
     * Migrates versions.
     */
    public static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {

            // Removing support for the more complex simulation for now.
            database.execSQL("UPDATE `entities` SET `type` = ?", params(SimpleIndividualIncomeSimulation.ENTITY_TYPE));

            // Remove obsolete parameters.
            database.execSQL("DELETE FROM `entity_parameters` WHERE `name` = ?", params(CanadianIndividualIncomeSimulation.PARAMETER_PROVINCE));
        }
    };
}
