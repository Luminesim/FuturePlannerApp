package com.luminesim.futureplanner.input;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.CalendarView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.luminesim.futureplanner.R;

import java.util.GregorianCalendar;

/**
 *
 */
public class AssetSelectionFragment extends AlertDialogFragment {

    private View toStart;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        toStart = getActivity().getLayoutInflater().inflate(R.layout.fragment_asset_type_input, null);
        return new AlertDialog.Builder(getContext())
                .setView(toStart)
                .setPositiveButton(R.string.button_save, (d, which) -> {
                    positiveButtonCallback.accept(toStart);
                })
                .setNegativeButton(R.string.button_cancel, (d, w) -> {
                })
                .create();
    }
}