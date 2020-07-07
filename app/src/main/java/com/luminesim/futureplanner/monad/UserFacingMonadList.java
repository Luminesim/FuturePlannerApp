package com.luminesim.futureplanner.monad;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.luminesim.futureplanner.Category;
import com.luminesim.futureplanner.R;
import com.luminesim.futureplanner.db.EntityFactDetail;
import com.luminesim.futureplanner.input.AlertDialogFragment;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import ca.anthrodynamics.indes.lang.ComputableMonad;
import ca.anthrodynamics.indes.lang.Monad;
import ca.anthrodynamics.indes.lang.MonadInformation;
import lombok.NonNull;

/**
 * Allows monads to be navigated.
 */
public class UserFacingMonadList extends RecyclerView.Adapter<UserFacingMonadList.PredictiveTextHolder> {
    private final Callback mCallback;
    private final Category mCategory;
    private List<String> mCurrentPredictiveViews = new ArrayList<>();
    private List<Monad> mCurrentOptions = new ArrayList<>();
    private MonadInformation mSelectionThusFar = null;
    private MonadDatabase mData;

    /**
     * @return
     *  The output type of the current selection.
     */
    public Optional<Class<?>> getCurrentSelectionOutputType() {
        if (mSelectionThusFar == null) {
            return Optional.empty();
        }
        else {
            return mSelectionThusFar.getOutType();
        }
    }

    /**
     * Goes back to starting monads.
     */
    public void restartSelection() {
        mSelectionThusFar = null;
        updateMonadList();
    }

    /**
     * The holder for each monad's predictive view.
     */
    public static class PredictiveTextHolder extends RecyclerView.ViewHolder {
        public View view;
        public Monad monad;

        public PredictiveTextHolder(View baseView) {
            super(baseView);
            view = baseView;
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
    public UserFacingMonadList(AppCompatActivity c, Category currentCategory, Callback callback) {

        this.mCallback = callback;
        this.mData = MonadDatabase.getDatabase(c);
        this.mCategory = currentCategory;
        updateMonadList();
    }

    public Class getOutputType(String monadId) {
        return (Class)mData.getMonadById(monadId).getOutType().get();
    }

    /**
     * Removes all monads from the view and adds new ones.
     */
    private void updateMonadList() {
        notifyItemRangeRemoved(0, mCurrentPredictiveViews.size());
        mCurrentOptions.clear();
        mCurrentPredictiveViews.clear();
        if (mSelectionThusFar == null) {
            mCurrentOptions = mData.getStartingOptions(mCategory);
        } else {
            mCurrentOptions = mData.getNextOptions(mSelectionThusFar, mCategory);
        }
        for (Monad next : mCurrentOptions) {
            mCurrentPredictiveViews.add(mData.getPredictiveText(next));
        }
        notifyDataSetChanged();
    }

    // Create new views (invoked by the layout manager)
    @Override
    public PredictiveTextHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // create a new view
        View v = LayoutInflater
                .from(parent.getContext())
                .inflate(R.layout.monad_selection_layout, parent, false);
        PredictiveTextHolder vh = new PredictiveTextHolder(v);

        vh.itemView.findViewById(R.id.chip).setOnClickListener(v1 -> {
            // Determine the selection.
            // Show an alert to get input, if requested.
            if (mData.getInputView(vh.monad).isPresent()) {
                // If the fragment is there, build the alert and listen for the "OK"
                FragmentManager fm = ((AppCompatActivity) parent.getContext()).getSupportFragmentManager();
                AlertDialogFragment f = mData.getInputView(vh.monad).get().get();

                f.setPositiveButtonCallback(view -> {
                    ComputableMonad result = mData.processInput(vh.monad, view);
                    triggerCallbackAndUpdateMonadList(mData.format(vh.monad, result.getParameterValues()), mData.getId(vh.monad), result.getParameterValues());
                });
                f.show(fm, "Input");
            }
            // Otherwise, move to the next bit of text.
            else {

                ComputableMonad result = vh.monad.withParameters();

                // If a result was found, then update to the next monad.
                if (result != null) {
                    triggerCallbackAndUpdateMonadList(mData.format(vh.monad, result.getParameterValues()), mData.getId(vh.monad), result.getParameterValues());
                }
            }
        });
        return vh;
    }

    /**
     * Allows the callback to be handled by UI events or programmatically.
     * @param summaryText
     * @param monadId
     * @param parameters
     */
    public void triggerCallbackAndUpdateMonadList(@NonNull String summaryText, @NonNull String monadId, @NonNull Object[] parameters) {
        Monad lastSelection = mData.getMonadById(monadId);
        if (mSelectionThusFar == null) {
            mSelectionThusFar = lastSelection.getInfo();
        }
        else {
            mSelectionThusFar = mSelectionThusFar.getMutableCompositionInfo(lastSelection);
        }

        // Convert all parameters to those that the monad needs.
        ObjectMapper fixer = new ObjectMapper();
        fixer.registerModule(new JavaTimeModule());
        fixer.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        Object[] params = new Object[parameters.length];
        try {
            for (int i = 0; i < parameters.length; i += 1) {
                params[i] = fixer.readValue("\""+parameters[i]+"\"", lastSelection.getParameterTypes()[i]);
            }
        }
        catch (Throwable t) {
            throw new RuntimeException(t);
        }
        mCallback.callback(summaryText, monadId, params);
        updateMonadList();
    }

    /**
     * Allows the callback to be handled by UI events or programmatically.
     */
    public void triggerCallbackAndUpdateMonadList(@NonNull EntityFactDetail detail) {

        try {
            MonadData data = MonadData.fromJson(detail.getMonadJson());
            Monad lastSelection = mData.getMonadById(data.getMonadId());
            if (mSelectionThusFar == null) {
                mSelectionThusFar = lastSelection.getInfo();
            }
            else {
                mSelectionThusFar = mSelectionThusFar.getMutableCompositionInfo(lastSelection);
            }
            mCallback.callback(
                    mData.format(data),
                    data.getMonadId(),
                    data.getParameters(lastSelection.getParameterTypes(), false)
            );
            updateMonadList();
        }
        catch (IOException t) {
            throw new RuntimeException(t);
        }
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(PredictiveTextHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        ((Chip)holder.view.findViewById(R.id.chip)).setText(mCurrentPredictiveViews.get(position));
        holder.monad = mCurrentOptions.get(position);
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mCurrentPredictiveViews.size();
    }
}
