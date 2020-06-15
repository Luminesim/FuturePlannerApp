package com.example.myfirstapp.monad;

import android.view.View;

import androidx.annotation.NonNull;

import com.example.myfirstapp.input.AlertDialogFragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import ca.anthrodynamics.indes.lang.ComputableMonad;
import ca.anthrodynamics.indes.lang.Monad;
import ca.anthrodynamics.indes.lang.MonadRepo;

/**
 * All monads known to the system.
 */
public final class MonadDataset {

    /**
     * Unique keys of all monads.
     */
    private final Map<Monad, String> mKeys = new HashMap<>();

    /**
     * Monads associated with each key.
     */
    private final Map<String, Monad> mMonads = new HashMap<>();

    /**
     * All string formatters for the monad's data.
     */
    private final Map<Monad, String> mFormatters = new HashMap<>();

    /**
     * IDs of the views shown for each monad in predictive contexts (e.g. $[amount])
     *
     */
    private final Map<Monad, String> mPredictiveViews = new HashMap<>();

    /**
     * IDs of the views shown for each monad when inputting data (e.g. view fragment for dialog)
     */
    private final Map<Monad, Optional<Supplier<AlertDialogFragment>>> mInputViews = new HashMap<>();

    /**
     * Processors of the views shown for each monad when inputting data (e.g. view fragment for dialog)
     */
    private final Map<Monad, BiFunction<Monad, View, ComputableMonad>> mInputProcessors = new HashMap<>();

    private MonadRepo repo = new MonadRepo();

    /**
     * @pre key is unique
     * @param key
     * @param monad
     * @param formatter
     * @param predictiveView
     * @param inputView
     * @param inputProcessor
     * @return
     */
    public MonadDataset add(
            @NonNull String key,
            @NonNull Monad monad,
            @NonNull String formatter,
            @NonNull String predictiveView,
            @NonNull Optional<Supplier<AlertDialogFragment>> inputView,
            @NonNull BiFunction<Monad, View, ComputableMonad> inputProcessor) {
        repo.add(monad);

        // If key already exists, quit.
        if (mKeys.containsKey(key)) {
            throw new IllegalArgumentException("Duplicate key: " + key);
        }

        mMonads.put(key, monad);
        mKeys.put(monad, key);
        mFormatters.put(monad, formatter);
        mPredictiveViews.put(monad, predictiveView);
        mInputViews.put(monad, inputView);
        mInputProcessors.put(monad, inputProcessor);
        return this;
    }

    public List<Monad> getStartingOptions() {
        return repo.getStartingOptions();
    }

    public List<Monad> getNextOptions(@NonNull Monad current) {
        return repo.nextOptions(current);
    }

    public String getPredictiveText(@NonNull Monad next) {
        if (!mPredictiveViews.containsKey(next)) {
            throw new IllegalArgumentException("Predictive text not set for " + next);
        }
        return mPredictiveViews.get(next);
    }

    public ComputableMonad processInput(@NonNull Monad monad, View input) {
        return mInputProcessors.get(monad).apply(monad, input);
    }

    /**
     * Formats the monad's result.
     * @param monad
     * @param parameterValues
     * @return
     */
    public String format(@NonNull Monad monad, @NonNull Object[] parameterValues) {
        return String.format(mFormatters.get(monad), parameterValues);
    }

    public Optional<Supplier<AlertDialogFragment>> getInputView(Monad monad) {
        return mInputViews.get(monad);
    }

    /**
     *
     * @param monad
     * @return
     *  The ID associated with the monad.
     * @pre monad is known
     */
    public String getId(@NonNull Monad monad) {
        if (!mKeys.containsKey(monad)) {
            throw new IllegalArgumentException("Unknown monad: " + monad);
        }
        return mKeys.get(monad);
    }
}
