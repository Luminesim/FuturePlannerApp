package com.example.myfirstapp.monad;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.CalendarView;
import android.widget.EditText;

import com.example.myfirstapp.R;
import com.example.myfirstapp.input.AlertDialogFragment;
import com.example.myfirstapp.input.CalendarInputFragment;
import com.example.myfirstapp.input.NumericAmountInputFragment;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import ca.anthrodynamics.indes.lang.ComputableMonad;
import ca.anthrodynamics.indes.lang.Monad;
import ca.anthrodynamics.indes.lang.MonadRepo;
import ca.anthrodynamics.indes.lang.MoneyMonad;
import ca.anthrodynamics.indes.lang.StartingMonad;
import ca.anthrodynamics.indes.lang.ToRateMonad;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * All monads known to the system.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MonadDatabase {

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
    private final Map<String, String> mFormatters = new HashMap<>();

    /**
     * IDs of the views shown for each monad in predictive contexts (e.g. $[amount])
     */
    private final Map<String, String> mPredictiveViews = new HashMap<>();

    /**
     * IDs of the views shown for each monad when inputting data (e.g. view fragment for dialog)
     */
    private final Map<String, Optional<Supplier<AlertDialogFragment>>> mInputViews = new HashMap<>();

    /**
     * Processors of the views shown for each monad when inputting data (e.g. view fragment for dialog)
     */
    private final Map<String, BiFunction<Monad, View, ComputableMonad>> mInputProcessors = new HashMap<>();


    /**
     * Standard monad database with known monad information.
     */
    private static MonadDatabase INSTANCE;

    private MonadRepo mRepo = new MonadRepo();

    /**
     * @param key
     * @param monad
     * @param formatter
     * @param predictiveView
     * @param inputView
     * @param inputProcessor
     * @return
     * @pre key is unique
     */
    public MonadDatabase add(
            @NonNull String key,
            @NonNull Monad monad,
            @NonNull String formatter,
            @NonNull String predictiveView,
            @NonNull Optional<Supplier<AlertDialogFragment>> inputView,
            @NonNull BiFunction<Monad, View, ComputableMonad> inputProcessor) {
        mRepo.add(monad);

        // If key already exists, quit.
        if (mKeys.containsKey(key)) {
            throw new IllegalArgumentException("Duplicate key: " + key);
        }

        mMonads.put(key, monad);
        mKeys.put(monad, key);
        mFormatters.put(key, formatter);
        mPredictiveViews.put(key, predictiveView);
        mInputViews.put(key, inputView);
        mInputProcessors.put(key, inputProcessor);
        return this;
    }

    public List<Monad> getStartingOptions() {
        return mRepo.getStartingOptions();
    }

    public List<Monad> getNextOptions(@NonNull Monad current) {
        return mRepo.nextOptions(current);
    }

    public String getPredictiveText(@NonNull Monad next) {
        if (!mPredictiveViews.containsKey(mKeys.get(next))) {
            throw new IllegalArgumentException("Predictive text not set for " + next);
        }
        return mPredictiveViews.get(mKeys.get(next));
    }

    public ComputableMonad processInput(@NonNull Monad monad, View input) {
        return mInputProcessors.get(mKeys.get(monad)).apply(monad, input);
    }

    /**
     * Formats the monad's result.
     *
     * @param monad
     * @param parameterValues
     * @return
     */
    public String format(@NonNull Monad monad, @NonNull Object[] parameterValues) {
        return format(mKeys.get(monad), parameterValues);
    }

    /**
     * Formats the monad's result.
     *
     * @param parameterValues
     * @return
     */
    public String format(@NonNull String monadId, @NonNull Object[] parameterValues) {
        return String.format(mFormatters.get(monadId), parameterValues);
    }

    /**
     * Formats the monad's result.
     *
     * @return
     */
    public String format(@NonNull MonadData data) throws IOException {
        // Fix parameters.
        Monad template = getMonadById(data.getMonadId());
        Object[] params = data.getParameters(template.getParameterTypes(), false);
        return format(template, params);
    }

    public Optional<Supplier<AlertDialogFragment>> getInputView(Monad monad) {
        return mInputViews.get(mKeys.get(monad));
    }

    /**
     *
     * @param monadJson
     * @return
     *  A computable value from its JSON representation.
     * @throws UnknownMonadException
     */
    public ComputableMonad fromJson(String monadJson) throws UnknownMonadException {
        try {
            Log.i("FuturePlanner/"+MonadDatabase.class, "Data is " + monadJson);
            MonadData data = MonadData.fromJson(monadJson);

            // Fix parameters.
            return getMonadById(data.getMonadId()).withParameters(deserializeParameters(data));
        }
        catch (Throwable t) {
            throw new UnknownMonadException(t);
        }
    }

    /**
     *
     * @param monadId
     * @return
     *  The monad associated with the given ID.
     */
    public Monad getMonadById(@NonNull String monadId) throws UnknownMonadException {
        Monad toReturn = mMonads.get(monadId);
        if (toReturn == null) {
            throw new UnknownMonadException("Unknown monad ID: "+monadId);
        }
        return toReturn;
    }

    /**
     * @return
     *  The formatted text associated with the given monad and its data.
     */
    public String getFormattedStringFromJson(String monadJson) throws UnknownMonadException {
        try {
            Log.i("FuturePlanner/"+MonadDatabase.class, "Data is " + monadJson);
            MonadData data = MonadData.fromJson(monadJson);

            // Fix parameters.
            Object[] params = deserializeParameters(data);
            return format(data.getMonadId(), params);
        }
        catch (Throwable t) {
            throw new UnknownMonadException(t);
        }
    }

    private Object[] deserializeParameters(@NonNull MonadData data) {
        Monad template = getMonadById(data.getMonadId());
        Object[] params = data.getParameters(template.getParameterTypes(), false);
        return params;
    }

    /**
     * @param monad
     * @return The ID associated with the monad.
     * @pre monad is known
     */
    public String getId(@NonNull Monad monad) {
        if (!mKeys.containsKey(monad)) {
            throw new IllegalArgumentException("Unknown monad: " + monad);
        }
        return mKeys.get(monad);
    }

    public static MonadDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (MonadDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = new MonadDatabase();

                    // Create the available monads.
                    INSTANCE.add(
                            "IdMoneyAmount",
                            new MoneyMonad(context.getString(R.string.monad_money_amount)),
                            "$ %s",
                            context.getString(R.string.monad_money_view_text),
                            Optional.of(() -> new NumericAmountInputFragment()),
                            //Optional.of(R.layout.fragment_numeric_amount_input),
                            (template, inputs) ->
                                    template.withParameters(Double.valueOf(((EditText) inputs.findViewById(R.id.numberEditTextMoneyMonad)).getText().toString()))
                    );
                    INSTANCE.add("IdPerYear", new ToRateMonad(12), context.getString(R.string.monad_per_year_view_text), context.getString(R.string.monad_per_year_view_text));
                    INSTANCE.add("IdPerMonth", new ToRateMonad(1), context.getString(R.string.monad_per_month_view_text), context.getString(R.string.monad_per_month_view_text));
                    INSTANCE.add("IdPerDay", new ToRateMonad(1 / 30.0), context.getString(R.string.monad_per_day_view_text), context.getString(R.string.monad_per_day_view_text));
                    INSTANCE.add(
                            "IdStarting",
                            new StartingMonad("Date"),
                            "starting %s",
                            context.getString(R.string.monad_starting_view_text),
                            Optional.of(() -> new CalendarInputFragment()),
                            (template, view) -> {
                                LocalDate ld = LocalDate.ofEpochDay(((CalendarView) view.findViewById(R.id.datePicker)).getDate() / (24 * 60 * 60 * 1000));
                                return template.withParameters(ld);
                            }
                    );


                }
            }
        }
        return INSTANCE;
    }

    private void add(@NonNull String id, @NonNull Monad monad, @NonNull String formatter, @NonNull String predictiveView) {
        this.add(id, monad, formatter, predictiveView, Optional.empty(), (template, view) -> template.withParameters());
    }
}
