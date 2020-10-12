package com.luminesim.futureplanner.models;

import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LifecycleOwner;

import com.luminesim.futureplanner.Category;
import com.luminesim.futureplanner.R;
import com.luminesim.futureplanner.db.EntityFact;
import com.luminesim.futureplanner.db.EntityFactDetail;
import com.luminesim.futureplanner.db.EntityRepository;
import com.luminesim.futureplanner.monad.MonadData;
import com.luminesim.futureplanner.monad.MonadDatabase;
import com.luminesim.futureplanner.ui.main.FactEntryActivity;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

@Getter
@AllArgsConstructor
public class ModelPackage {

    @NonNull
    private final String id;

    /**
     * The model type that any valid statement sequence or template must produce.
     */
    @NonNull
    private final Class outputType;
    @NonNull
    private final ModelTemplate defaultTemplate;
    @NonNull
    private final Map<String, ModelTemplate> templates;

    /**
     * Adds all options relevant to this package to the database.
     */
    @NonNull
    private final Consumer<MonadDatabase> optionProvider;


    public void addOptionsToDatabase(@NonNull MonadDatabase db) {
        optionProvider.accept(db);
    }

    public void addDefaultTemplateToEntity(long entityUid, EntityRepository repo, Consumer<Long> onFactReady) {
        addTemplateToEntity(defaultTemplate, entityUid, repo, onFactReady);
    }

    public void addTemplateToEntity(String templateId, long entityUid, EntityRepository repo, Consumer<Long> onFactReady) {
        addTemplateToEntity(templates.get(templateId), entityUid, repo, onFactReady);
    }

    private void addTemplateToEntity(ModelTemplate template, long entityUid, EntityRepository repo, Consumer<Long> onFactReady) {
        // Create the model fact and its setup details.
        template.getSetupInstructions().forEach(setupInstructions -> {

            EntityFact modelFact = EntityFact
                    .builder()
                    .category(Category.ModelDefinition)
                    .name(template.getName())
                    .entityUid(entityUid)
                    .build();

            // Collect all details.
            List<EntityFactDetail> details = new LinkedList<>();
            for (int i = 0; i < setupInstructions.getSteps().size(); i += 1) {
                ModelTemplate.SetupStep step = setupInstructions.getSteps().get(i);
                EntityFactDetail detail = EntityFactDetail
                        .builder()
                        .stepNumber(i)
                        .monadJson(new MonadData(step.getMonadId(), step.getMonadParameters()).toJson())
                        .build();
                details.add(detail);
            }

            // Submit the update.
            repo.updateFact(entityUid, modelFact, details, onFactReady);
//                edb.updateFact(entityUid, submodel, details, factUid -> {
//
//                    // Launch edit page.
//                    Intent launcher = new Intent(c, FactEntryActivity.class);
//                    launcher.putExtra(FactEntryActivity.EXTRA_MODEL_CLASS, outputType.getName());
//                    launcher.putExtra(FactEntryActivity.EXTRA_DATA_CATEGORY, Category.ModelDefinition);
//                    launcher.putExtra(c.getString(R.string.extra_entity_uid), entityUid);
//                    launcher.putExtra(c.getString(R.string.extra_fact_uid), factUid);
//                });
        });
    }
}
