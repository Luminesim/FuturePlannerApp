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

//                            // TODO FIXME TEMP: THIS IS TO HELP POPULATE WITH BASIC DATA.
//                            .addCallback(new RoomDatabase.Callback() {
//                                @Override
//                                public void onOpen(@NonNull SupportSQLiteDatabase db) {
//                                    super.onOpen(db);
//
//                                    if (1 == 1) throw new RuntimeException("Not implemented");
//                                    // If you want to keep data through app restarts,
//                                    // comment out the following block
//                                    databaseWriteExecutor.execute(() -> {
//                                        // Populate the database in the background.
//                                        // If you want to start with more words, just add them.
//                                        EntityDao dao = INSTANCE.entityDao();
//                                        dao.deleteAll();
//
//                                        Entity user = Entity.builder().name("User").build();
//                                        EntityFact incomeWork = EntityFact
//                                                .builder()
//                                                .category(Category.Income.name())
//                                                .entityUid(user.getUid())
//                                                .name("Work")
//                                                .build();
//                                        List<EntityFactDetail> details = new LinkedList<>();
//                                        details.add(EntityFactDetail
//                                                .builder()
//                                                .entityFactId(incomeWork.getUid())
//                                                .stepNumber(0)
//                                                .monadJson((new MonadData("IdMoneyAmount", 100_000.0)).toJson())
//                                                .build());
//                                        details.add(EntityFactDetail
//                                                .builder()
//                                                .entityFactId(incomeWork.getUid())
//                                                .stepNumber(1)
//                                                .monadJson((new MonadData("IdPerYear")).toJson())
//                                                .build());
//                                        EntityWithFacts toAdd = new EntityWithFacts(
//                                                user,
//                                                Arrays.asList(new EntityFactWithDetails(incomeWork, details))
//                                        );
//                                        dao.insert(toAdd);
//                                    });
//                                }
//                            })
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
