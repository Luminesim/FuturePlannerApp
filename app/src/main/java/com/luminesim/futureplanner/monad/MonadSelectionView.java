package com.luminesim.futureplanner.monad;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

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
import java.util.function.Consumer;

import ca.anthrodynamics.indes.lang.ComputableMonad;
import ca.anthrodynamics.indes.lang.Monad;
import ca.anthrodynamics.indes.lang.MonadInformation;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * Allows monads to be navigated.
 */
public class MonadSelectionView extends RecyclerView.Adapter<MonadSelectionView.PredictiveTextHolder> {
    private final Callback mCallback;
    private final Category mCategory;
    private List<String> mCurrentPredictiveViews = new ArrayList<>();
    private List<String> mCurrentHints = new ArrayList<>();
    private List<Monad> mCurrentOptions = new ArrayList<>();
    private MonadInformation mSelectionThusFar = null;
    private List<StackFrame> mStack = new ArrayList<>();
    private int mSelectionPosition = 0;
    private MonadDatabase mData;
    private Consumer<Integer> mOnFutureOptionsIncompatible;
    private Runnable mOnEdit = () -> {
    };

//    public interface OnEditListener {
//        public void onEdit(String monadId, int position, Object[] newParameters);
//    }

    /**
     * @return The output type of the current selection.
     */
    public Optional<Class<?>> getCurrentSelectionOutputType() {
        if (mSelectionThusFar == null) {
            return Optional.empty();
        } else {
            return mSelectionThusFar.getOutType();
        }
    }

//    public boolean doesSelectionProduceType(@NonNull Class<?> type) {
//        if (mSelectionThusFar == null) {
//            return false;
//        }
//        else {
//            return mSelectionThusFar.getProperties().canDuckTypeAs(type);
//        }
//    }

    /**
     * Goes back to starting monads.
     */
    public void restartSelection() {
        mSelectionThusFar = null;
        mStack.clear();
        mSelectionPosition = 0;
        updateMonadList();
    }

    public void editSelection(int index, FragmentManager fragmentManager, Runnable onEdit) {

        // Sanity check:
        if (index < 0) {
            throw new IllegalArgumentException("Index must be non-negative");
        }
        if (index > mStack.size()) {
            throw new IllegalArgumentException("Index "+index+" exceeds stack size " + mStack.size());
        }

        this.mOnEdit = onEdit;
        // Rewind to the selection so it can be replaced.
        if (index == 0) {
            mSelectionThusFar = null;
        } else {
            mSelectionThusFar = mData.getMonadById(mStack.get(0).getMonadId()).getInfo();
            for (int i = 1; i < index; i += 1) {
                mSelectionThusFar = mSelectionThusFar.getMutableCompositionInfo(mData.getMonadById(mStack.get(i).getMonadId()));
            }
        }
        mSelectionPosition = index;

        updateMonadList();
    }

    public void cancelEdit() {
        resumeStackIfNeeded();
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
        void callback(String summaryText, String hint, String monadId, Object[] parameters);
    }

    @AllArgsConstructor
    @Getter
    @Setter
    private class StackFrame {
        private String monadId;
        private Object[] params;
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public MonadSelectionView(
            @NonNull AppCompatActivity c,
            @NonNull Category currentCategory,
            @NonNull Callback callback,
            @NonNull Consumer<Integer> onEditBlocked) {

        this.mCallback = callback;
        this.mData = MonadDatabase.getDatabase(c);
        this.mCategory = currentCategory;
        this.mOnFutureOptionsIncompatible = onEditBlocked;
        updateMonadList();
    }

    /**
     * Removes all monads from the view and adds new ones.
     */
    private void updateMonadList() {
        notifyItemRangeRemoved(0, mCurrentPredictiveViews.size());
        mCurrentOptions.clear();
        mCurrentPredictiveViews.clear();
        mCurrentHints.clear();
        if (mSelectionThusFar == null) {
            mCurrentOptions = mData.getStartingOptions(mCategory);
        } else {
            mCurrentOptions = mData.getNextOptions(mSelectionThusFar, mCategory);
        }
        for (Monad next : mCurrentOptions) {
            mCurrentPredictiveViews.add(mData.getPredictiveText(next));
            mCurrentHints.add(mData.getHint(mData.getId(next)));
        }
        notifyDataSetChanged();
    }

    // Create new views (invoked by the layout manager)
    @Override
    public PredictiveTextHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // create a new view
        View v = LayoutInflater
                .from(parent.getContext())
                .inflate(R.layout.view_selectable_with_hint, parent, false);
        PredictiveTextHolder vh = new PredictiveTextHolder(v);

        vh.itemView.findViewById(R.id.layout).setOnClickListener(v1 -> {
            selectItem(vh.monad, ((AppCompatActivity) parent.getContext()).getSupportFragmentManager());
        });
        return vh;
    }

    private void selectItem(Monad monad, FragmentManager fragmentManager) {
        // Determine the selection.
        // Show an alert to get input, if requested.
        if (mData.getInputView(monad).isPresent()) {
            // If the fragment is there, build the alert and listen for the "OK"
            AlertDialogFragment f = mData.getInputView(monad).get().get();

            f.setPositiveButtonCallback(view -> {
                try {
                    ComputableMonad result = mData.processInput(monad, view);
                    triggerCallbackAndUpdateMonadList(mData.format(monad, result.getParameterValues()), mData.getId(monad), result.getParameterValues());
                }
                catch (Throwable t) {
                    Log.e("FuturePlanner Monad Selection", t.getLocalizedMessage());
                }
            });
            f.show(fragmentManager, "Input");
        }
        // Otherwise, move to the next bit of text.
        else {

            ComputableMonad result = monad.withParameters();

            // If a result was found, then update to the next monad.
            if (result != null) {
                triggerCallbackAndUpdateMonadList(mData.format(monad, result.getParameterValues()), mData.getId(monad), result.getParameterValues());
            }
        }
    }

    /**
     * Allows the callback to be handled by UI events or programmatically.
     *
     * @param summaryText
     * @param monadId
     * @param parameters
     */
    public void triggerCallbackAndUpdateMonadList(@NonNull String summaryText, @NonNull String monadId, @NonNull Object[] parameters) {
        Monad lastSelection = mData.getMonadById(monadId);
        if (mSelectionThusFar == null) {
            mSelectionThusFar = lastSelection.getInfo();
        } else {
            mSelectionThusFar = mSelectionThusFar.getMutableCompositionInfo(lastSelection);
        }

        // Convert all parameters to those that the monad needs.
        ObjectMapper fixer = new ObjectMapper();
        fixer.registerModule(new JavaTimeModule());
        fixer.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        Object[] params = new Object[parameters.length];
        try {
            for (int i = 0; i < parameters.length; i += 1) {
                params[i] = fixer.readValue("\"" + parameters[i] + "\"", lastSelection.getParameterTypes()[i]);
            }
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }

        advanceOrUpdateStack(new StackFrame(monadId, params));
        mCallback.callback(summaryText, mData.getHint(monadId), monadId, params);
        updateMonadList();
        resumeStackIfNeeded();
    }

    private void advanceOrUpdateStack(StackFrame frame) {
        if (mSelectionPosition == mStack.size()) {
            mStack.add(frame);
            mOnEdit = () -> {
            };
        } else {
            mOnEdit.run();
            mOnEdit = () -> {
            };
            mStack.get(mSelectionPosition).setMonadId(frame.getMonadId());
            mStack.get(mSelectionPosition).setParams(frame.getParams());
        }
        mSelectionPosition += 1;
    }

    /**
     * Allows the callback to be handled by UI events or programmatically.
     */
    public void triggerCallbackAndUpdateMonadList(@NonNull StackFrame data) throws IllegalStateException {

        try {
            Monad lastSelection = mData.getMonadById(data.getMonadId());
            if (mSelectionThusFar == null) {
                mSelectionThusFar = lastSelection.getInfo();
            } else {
                mSelectionThusFar = mSelectionThusFar.getMutableCompositionInfo(lastSelection);
            }

            advanceOrUpdateStack(new StackFrame(data.getMonadId(), data.getParams()));
            mCallback.callback(
                    mData.format(data.getMonadId(), data.getParams()),
                    mData.getHint(data.getMonadId()),
                    data.getMonadId(),
                    data.getParams()
            );
            updateMonadList();
            resumeStackIfNeeded();
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
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
            } else {
                mSelectionThusFar = mSelectionThusFar.getMutableCompositionInfo(lastSelection);
            }

            advanceOrUpdateStack(new StackFrame(data.getMonadId(), data.getParameters(lastSelection.getParameterTypes(), false)));
            mCallback.callback(
                    mData.format(data),
                    mData.getHint(data.getMonadId()),
                    data.getMonadId(),
                    data.getParameters(lastSelection.getParameterTypes(), false)
            );

            updateMonadList();
            resumeStackIfNeeded();
        } catch (IOException t) {
            throw new RuntimeException(t);
        }
    }

    private void resumeStackIfNeeded() {

        // If we've edited something, ensure that we can rebuild up to the end of the statement.
        while (mSelectionPosition < mStack.size()) {
            // If this is the first item, we may not have set a selection. If so, do it.
            if (mSelectionThusFar == null && mSelectionPosition == 0) {
//                mSelectionThusFar = mData.getMonadById(mStack.get(mSelectionPosition).getMonadId()).getInfo();
                triggerCallbackAndUpdateMonadList(mStack.get(0));
            }
            // If the next option is safe to use then use it.
            else if (mData.getNextOptions(mSelectionThusFar, mCategory).contains(mData.getMonadById(mStack.get(mSelectionPosition).getMonadId()))) {
                triggerCallbackAndUpdateMonadList(mStack.get(mSelectionPosition));
            }
            // Otherwise, nuke everything from hereafter.
            else {
                mOnFutureOptionsIncompatible.accept(mSelectionPosition);
                mStack = mStack.subList(0, mSelectionPosition);
            }
        }
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(PredictiveTextHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        ((TextView) holder.view.findViewById(R.id.chip)).setText(mCurrentPredictiveViews.get(position));
        ((TextView) holder.view.findViewById(R.id.hint)).setText(mCurrentHints.get(position));
        holder.monad = mCurrentOptions.get(position);
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mCurrentPredictiveViews.size();
    }
}
