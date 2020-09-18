package com.luminesim.futureplanner.monad;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.CalendarView;
import android.widget.EditText;

import com.luminesim.futureplanner.Category;
import com.luminesim.futureplanner.R;
import com.luminesim.futureplanner.input.AlertDialogFragment;
import com.luminesim.futureplanner.input.CalendarInputFragment;
import com.luminesim.futureplanner.input.NumericAmountInputFragment;
import com.luminesim.futureplanner.input.PercentInputFragment;
import com.luminesim.futureplanner.monad.types.CurrencyMonad;
import com.luminesim.futureplanner.monad.types.OnDateMonad;
import com.luminesim.futureplanner.monad.types.PercentAdditionMonad;
import com.luminesim.futureplanner.monad.types.PercentDeductionMonad;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import ca.anthrodynamics.indes.lang.ComputableMonad;
import ca.anthrodynamics.indes.lang.EndingMonad;
import ca.anthrodynamics.indes.lang.Monad;
import ca.anthrodynamics.indes.lang.MonadInformation;
import ca.anthrodynamics.indes.lang.MonadRepo;
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

    private final Map<String, String> mHints = new HashMap<>();

    /**
     * Monads associated with each key.
     */
    private final Map<String, Monad> mMonads = new HashMap<>();

    /**
     * All string formatters for the monad's data.
     */
    private final Map<String, Function<Object[], String>> mFormatters = new HashMap<>();

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
     * The categories for which a monad may be used.
     */
    private final Map<String, List<Category>> mCategories = new HashMap<>();



    /**
     * @param key
     * @param monad
     * @param formatString
     * @param predictiveView
     * @param inputView
     * @param inputProcessor
     * @return
     * @pre key is unique
     */
    public MonadDatabase add(
            @NonNull String key,
            @NonNull Monad monad,
            @NonNull List<Category> validCategories,
            @NonNull String formatString,
            @NonNull String predictiveView,
            @NonNull String hint,
            @NonNull Optional<Supplier<AlertDialogFragment>> inputView,
            @NonNull BiFunction<Monad, View, ComputableMonad> inputProcessor) {
        return add(key, monad, validCategories, params -> String.format(formatString, params), predictiveView, hint, inputView, inputProcessor);
    }
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
            @NonNull List<Category> validCategories,
            @NonNull Function<Object[], String> formatter,
            @NonNull String predictiveView,
            @NonNull String hint,
            @NonNull Optional<Supplier<AlertDialogFragment>> inputView,
            @NonNull BiFunction<Monad, View, ComputableMonad> inputProcessor) {
        mRepo.add(monad);

        // If key already exists, quit.
        if (mKeys.containsKey(key)) {
            throw new IllegalArgumentException("Duplicate key: " + key);
        }

        // If the valid category list is empty, quit.
        if (validCategories.isEmpty()) {
            throw new IllegalArgumentException("Category list must have at least one item: " + key);
        }

        mMonads.put(key, monad);
        mKeys.put(monad, key);
        mCategories.put(key, Collections.unmodifiableList(new ArrayList<>(validCategories)));
        mFormatters.put(key, formatter);
        mPredictiveViews.put(key, predictiveView);
        mHints.put(key, hint);
        mInputViews.put(key, inputView);
        mInputProcessors.put(key, inputProcessor);
        return this;
    }

    public List<Monad> getStartingOptions(@NonNull Category currentCategory) {

        // Get all valid options that work in this category.
        return mRepo.getStartingOptions().stream().filter(next -> mCategories.get(getId(next)).contains(currentCategory)).collect(Collectors.toList());
    }

    public List<Monad> getNextOptions(@NonNull MonadInformation current, @NonNull Category currentCategory) {
        // Get all valid options that work in this category.
        return mRepo.nextOptions(current).stream().filter(next -> mCategories.get(getId(next)).contains(currentCategory)).collect(Collectors.toList());
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
        return mFormatters.get(monadId).apply(parameterValues);
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
     * @param monadJson
     * @return A computable value from its JSON representation.
     * @throws UnknownMonadException
     */
    public ComputableMonad fromJson(String monadJson) throws UnknownMonadException {
        try {
            Log.i("FuturePlanner/" + MonadDatabase.class, "Data is " + monadJson);
            MonadData data = MonadData.fromJson(monadJson);

            // Fix parameters.
            return getMonadById(data.getMonadId()).withParameters(deserializeParameters(data));
        } catch (Throwable t) {
            throw new UnknownMonadException(t);
        }
    }

    /**
     * @param monadId
     * @return The monad associated with the given ID.
     */
    public Monad getMonadById(@NonNull String monadId) throws UnknownMonadException {
        Monad toReturn = mMonads.get(monadId);
        if (toReturn == null) {
            throw new UnknownMonadException("Unknown monad ID: " + monadId);
        }
        return toReturn;
    }

    /**
     * @return The formatted text associated with the given monad and its data.
     */
    public String getFormattedStringFromJson(String monadJson) throws UnknownMonadException {
        try {
            Log.i("FuturePlanner/" + MonadDatabase.class, "Data is " + monadJson);
            MonadData data = MonadData.fromJson(monadJson);

            // Fix parameters.
            Object[] params = deserializeParameters(data);
            return format(data.getMonadId(), params);
        } catch (Throwable t) {
            throw new UnknownMonadException(t);
        }
    }

    public Object[] deserializeParameters(@NonNull MonadData data) {
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
                            new CurrencyMonad(Currency.getInstance("CAD")),
                            Arrays.asList(Category.values()),
                            "$%s",
                            context.getString(R.string.monad_money_view_text),
                            context.getString(R.string.hint_money_amount),
                            Optional.of(() -> new NumericAmountInputFragment()),
                            //Optional.of(R.layout.fragment_numeric_amount_input),
                            (template, inputs) ->
                                    template.withParameters(Double.valueOf(((EditText) inputs.findViewById(R.id.inputNumber)).getText().toString()))
                    );
                    INSTANCE.add(
                            "IdPercentDeduction",
                            new PercentDeductionMonad("Percent"),
                            Arrays.asList(Category.values()),
                            "minus %s%%",
                            context.getString(R.string.moand_percent_deduction),
                            context.getString(R.string.hint_percent_deduction),
                            Optional.of(() -> new PercentInputFragment()),
                            (template, inputs) -> {
                                String input = ((EditText) inputs.findViewById(R.id.inputNumber)).getText().toString();
                                return template.withParameters(Double.valueOf(((EditText) inputs.findViewById(R.id.inputNumber)).getText().toString()));
                            }
                    );
//                    INSTANCE.add(
//                            // TODO FIXME: Got the key wrong, needs to be updated in DB...
//                            "IdPercentRateToRate",
//                            new PercentDeductionMonadRateToRate("Percent"),
//                            Arrays.asList(Category.values()),
//                            "minus %s%%",
//                            context.getString(R.string.moand_percent_deduction),
//                            context.getString(R.string.hint_percent_deduction),
//                            Optional.of(() -> new PercentInputFragment()),
//                            (template, inputs) -> {
//                                String input = ((EditText) inputs.findViewById(R.id.inputNumber)).getText().toString();
//                                return template.withParameters(Double.valueOf(((EditText) inputs.findViewById(R.id.inputNumber)).getText().toString()));
//                            }
//                    );
                    INSTANCE.add(
                            "IdPercentAddition",
                            new PercentAdditionMonad("Percent"),
                            Arrays.asList(Category.values()),
                            "plus %s%%",
                            context.getString(R.string.moand_percent_addition),
                            context.getString(R.string.hint_percent_addition),
                            Optional.of(() -> new PercentInputFragment()),
                            (template, inputs) -> {
                                String input = ((EditText) inputs.findViewById(R.id.inputNumber)).getText().toString();
                                return template.withParameters(Double.valueOf(((EditText) inputs.findViewById(R.id.inputNumber)).getText().toString()));
                            }
                    );
//                    INSTANCE.add(
//                            "IdPercentAdditionRateToRate",
//                            new PercentAdditionMonadRateToRate("Percent"),
//                            Arrays.asList(Category.values()),
//                            "plus %s%%",
//                            context.getString(R.string.moand_percent_addition),
//                            context.getString(R.string.hint_percent_addition),
//                            Optional.of(() -> new PercentInputFragment()),
//                            (template, inputs) -> {
//                                String input = ((EditText) inputs.findViewById(R.id.inputNumber)).getText().toString();
//                                return template.withParameters(Double.valueOf(((EditText) inputs.findViewById(R.id.inputNumber)).getText().toString()));
//                            }
//                    );
                    INSTANCE.add("IdPerYear",
                            new ToRateMonad(1 / 365.0), Arrays.asList(Category.values()),
                            context.getString(R.string.monad_per_year_view_text),
                            context.getString(R.string.monad_per_year_view_text),
                            context.getString(R.string.hint_per_year));
                    INSTANCE.add("IdPerMonth",
                            new ToRateMonad(1 / (365.0 / 12.0)),
                            Arrays.asList(Category.values()),
                            context.getString(R.string.monad_per_month_view_text),
                            context.getString(R.string.monad_per_month_view_text),
                            context.getString(R.string.hint_per_month));
                    INSTANCE.add("IdPerWeek",
                            new ToRateMonad(1 / (365.0 / 52.0)),
                            Arrays.asList(Category.values()),
                            context.getString(R.string.monad_per_week_view_text),
                            context.getString(R.string.monad_per_week_view_text),
                            context.getString(R.string.hint_per_week));
                    INSTANCE.add("IdPerFortnight",
                            new ToRateMonad(1 / (365.0 / 26.0)),
                            Arrays.asList(Category.values()),
                            context.getString(R.string.monad_per_fortnight_view_text),
                            context.getString(R.string.monad_per_fortnight_view_text),
                            context.getString(R.string.hint_per_fortnight));
                    INSTANCE.add("IdPerDay",
                            new ToRateMonad(1.0),
                            Arrays.asList(Category.values()),
                            context.getString(R.string.monad_per_day_view_text),
                            context.getString(R.string.monad_per_day_view_text),
                            context.getString(R.string.hint_per_day));
                    INSTANCE.add(
                            "IdStarting",
                            new StartingMonad("Date"),
                            Arrays.asList(Category.values()),
                            params -> {
                                LocalDate date = (LocalDate)params[0];
                                DateTimeFormatter df = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);
                                String formattedDate = date.format(df);
                                return String.format("starting %s", formattedDate);
                            },
                            context.getString(R.string.monad_starting_view_text),
                            context.getString(R.string.hint_starting),
                            Optional.of(() -> new CalendarInputFragment()),
                            (template, view) -> {
                                LocalDate ld = LocalDate.ofEpochDay(((CalendarView) view.findViewById(R.id.datePicker)).getDate() / (24 * 60 * 60 * 1000));
                                return template.withParameters(ld);
                            }
                    );
                    INSTANCE.add(
                            "IdEnding",
                            new EndingMonad("Date"),
                            Arrays.asList(Category.values()),
                            params -> {
                                LocalDate date = (LocalDate)params[0];
                                DateTimeFormatter df = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);
                                String formattedDate = date.format(df);
                                return String.format("ending %s", formattedDate);
                            },
                            context.getString(R.string.monad_ending_view_text),
                            context.getString(R.string.hint_ending),
                            Optional.of(() -> new CalendarInputFragment()),
                            (template, view) -> {
                                LocalDate ld = LocalDate.ofEpochDay(((CalendarView) view.findViewById(R.id.datePicker)).getDate() / (24 * 60 * 60 * 1000));
                                return template.withParameters(ld);
                            }
                    );
                    INSTANCE.add(
                            "IdOnDate",
                            new OnDateMonad("Date"),
                            Arrays.asList(Category.values()),
                            params -> {
                                LocalDateTime raw = (LocalDateTime)params[0];
                                LocalDate date = raw.toLocalDate();
                                DateTimeFormatter df = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);
                                String formattedDate = date.format(df);
                                return String.format("once on %s", formattedDate);
                            },
                            context.getString(R.string.monad_one_off_view_text),
                            context.getString(R.string.hint_on_date),
                            Optional.of(() -> new CalendarInputFragment()),
                            (template, view) -> {
                                LocalDate ld = LocalDate.ofEpochDay(((CalendarView) view.findViewById(R.id.datePicker)).getDate() / (24 * 60 * 60 * 1000));
                                return template.withParameters(ld.atTime(0, 0));
                            }
                    );
                }
            }
        }
        return INSTANCE;
    }

    private void add(@NonNull String id, @NonNull Monad monad, @NonNull List<Category> validCategories, @NonNull String formatter, @NonNull String predictiveView, @NonNull String hint) {
        this.add(id, monad, validCategories, formatter, predictiveView, hint, Optional.empty(), (template, view) -> template.withParameters());
    }

    public String getHint(@NonNull String monadId) {
        if (!mHints.containsKey(monadId)) {
            throw new IllegalArgumentException("Unknown monad or no hint exists: " + monadId);
        }
        return mHints.get(monadId);
    }

    /**
     * Converts JSON to a computable monad.
     *
     * @param monadJson
     * @return
     */
    public ComputableMonad makeComputable(@NonNull String monadJson) throws IOException {
        MonadData data = MonadData.fromJson(monadJson);
        Object[] params = deserializeParameters(data);
        if (params.length == 0) {
            return getMonadById(data.getMonadId()).withParameters();
        } else if (params.length == 1) {
            return getMonadById(data.getMonadId()).withParameters(params[0]);
        } else {
            return getMonadById(data.getMonadId()).withParameters(params);
        }
    }
}
