package com.example.myfirstapp.monad;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;


import com.example.myfirstapp.input.AlertDialogFragment;
import com.example.myfirstapp.R;
import com.example.myfirstapp.input.CalendarInputFragment;
import com.example.myfirstapp.input.NumericAmountInputFragment;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import ca.anthrodynamics.indes.lang.ComputableMonad;
import ca.anthrodynamics.indes.lang.Monad;
import ca.anthrodynamics.indes.lang.MoneyMonad;
import ca.anthrodynamics.indes.lang.StartingMonad;
import ca.anthrodynamics.indes.lang.ToRateMonad;

/**
 * Allows monads to be navigated.
 */
public class MonadRepoAdapter extends RecyclerView.Adapter<MonadRepoAdapter.PredictiveTextHolder> {
    private final Callback mCallback;
    private List<String> mCurrentPredictiveViews = new ArrayList<>();
    private List<Monad> mCurrentOptions = new ArrayList<>();
    private Monad mLastSelection = null;
    private MonadDataset mData = new MonadDataset();

    /**
     * Goes back to starting monads.
     */
    public void restartSelection() {
        mLastSelection = null;
        updateMonadList();
    }

    /**
     * The holder for each monad's predictive view.
     */
    public static class PredictiveTextHolder extends RecyclerView.ViewHolder {
        public TextView text;
        public Monad monad;

        public PredictiveTextHolder(View baseView) {
            super(baseView);
            text = (TextView) baseView;
        }
    }

    /**
     * A callback function.
     */
    @FunctionalInterface
    public interface Callback {
        void callback(String summaryText, String monadId, Object[] parameters);
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public MonadRepoAdapter(AppCompatActivity c, Callback callback) {

        this.mCallback = callback;

        // Create the available monads.
        register(
                "IdMoneyAmount",
                new MoneyMonad(c.getString(R.string.monad_money_amount)),
                "$ %s",
                c.getString(R.string.monad_money_view_text),
                Optional.of(() -> new NumericAmountInputFragment()),
                //Optional.of(R.layout.fragment_numeric_amount_input),
                (template, inputs) ->
                        template.withParameters(Double.valueOf(((EditText) inputs.findViewById(R.id.numberEditTextMoneyMonad)).getText().toString()))
        );
        register("IdPerYear", new ToRateMonad(12), c.getString(R.string.monad_per_year_view_text), c.getString(R.string.monad_per_year_view_text));
        register("IdPerMonth", new ToRateMonad(1), c.getString(R.string.monad_per_month_view_text), c.getString(R.string.monad_per_month_view_text));
        register("IdPerDay", new ToRateMonad(1 / 30.0), c.getString(R.string.monad_per_day_view_text), c.getString(R.string.monad_per_day_view_text));
        register(
                "IdStarting",
                new StartingMonad("Date"),
                "starting %s",
                c.getString(R.string.monad_starting_view_text),
                Optional.of(() -> new CalendarInputFragment()),
//                Optional.of(R.layout.fragment_calendar_input),
                (template, view) -> {
                    LocalDate ld = LocalDate.ofEpochDay(((CalendarView) view.findViewById(R.id.datePicker)).getDate() / (24 * 60 * 60 * 1000));
                    return template.withParameters(ld);
                }
        );

        updateMonadList();
    }

    /**
     * Removes all monads from the view and adds new ones.
     */
    private void updateMonadList() {
        notifyItemRangeRemoved(0, mCurrentPredictiveViews.size());
        mCurrentOptions.clear();
        mCurrentPredictiveViews.clear();
        if (mLastSelection == null) {
            mCurrentOptions = mData.getStartingOptions();
        } else {
            mCurrentOptions = mData.getNextOptions(mLastSelection);
        }
        for (Monad next : mCurrentOptions) {
            mCurrentPredictiveViews.add(mData.getPredictiveText(next));
        }
        notifyDataSetChanged();
    }

    /**
     * Registers a monad for this view.
     *
     * @param monad
     * @param formatter
     * @param predictiveView
     * @param inputView
     */
    private void register(@NonNull String id, @NonNull Monad monad, @NonNull String formatter, @NonNull String predictiveView, @NonNull Optional<Supplier<AlertDialogFragment>> inputView, @NonNull BiFunction<Monad, View, ComputableMonad> inputProcessor) {
        mData.add(id, monad, formatter, predictiveView, inputView, inputProcessor);
    }

    private void register(@NonNull String id, @NonNull Monad monad, @NonNull String formatter, @NonNull String predictiveView) {
        register(id, monad, formatter, predictiveView, Optional.empty(), (template, view) -> template.withParameters());
    }

    // Create new views (invoked by the layout manager)
    @Override
    public PredictiveTextHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // create a new view
        TextView v = (TextView) LayoutInflater
                .from(parent.getContext())
                .inflate(R.layout.monad_selection_layout, parent, false);
        PredictiveTextHolder vh = new PredictiveTextHolder(v);

        vh.itemView.setOnClickListener(v1 -> {
            // Determine the selection.
            // Show an alert to get input, if requested.
            if (mData.getInputView(vh.monad).isPresent()) {
//                // If the fragment is there, build the alert and listen for the "OK"
                FragmentManager fm = ((AppCompatActivity) parent.getContext()).getSupportFragmentManager();
                AlertDialogFragment f = mData.getInputView(vh.monad).get().get();
                f.setPositiveButtonCallback(view -> {
                    mLastSelection = vh.monad;
                    ComputableMonad result = mData.processInput(vh.monad, view);
                    mCallback.callback(mData.format(vh.monad, result.getParameterValues()), mData.getId(vh.monad), result.getParameterValues());
                    updateMonadList();
                });
                f.show(fm, "Input");
            }
            // Otherwise, move to the next bit of text.
            else {

                ComputableMonad result = vh.monad.withParameters();

                // If a result was found, then update to the next monad.
                if (result != null) {
                    mLastSelection = vh.monad;
                    mCallback.callback(mData.format(vh.monad, result.getParameterValues()), mData.getId(vh.monad), result.getParameterValues());
                    updateMonadList();
                }
            }
        });
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(PredictiveTextHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        holder.text.setText(mCurrentPredictiveViews.get(position));
        holder.monad = mCurrentOptions.get(position);

    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mCurrentPredictiveViews.size();
    }
}
