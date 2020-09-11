package com.luminesim.futureplanner.input;

import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.luminesim.futureplanner.R;

public class PercentInputFragment extends AlertDialogFragment {

    private View toStart;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        toStart = getActivity().getLayoutInflater().inflate(R.layout.fragment_percent_input, null);
        return new AlertDialog.Builder(getContext())
                .setView(toStart)
                .setPositiveButton(R.string.button_save, (d, which) -> {
                    try {
                        double percent = Double.valueOf(((EditText) toStart.findViewById(R.id.inputNumber)).getText().toString());
                        if (!(0 <= percent && percent <= 100)) {
                            new AlertDialog.Builder(getContext())
                                    .setTitle(R.string.title_error)
                                    .setMessage(R.string.error_percent_out_of_0_100_range)
                                    .setPositiveButton(R.string.button_ok, (x, y) -> {
                                    })
                                    .show();
                        } else {
                            positiveButtonCallback.accept(toStart);
                        }
                    }
                    catch (Throwable t) {
                        Log.e("FuturePlanner", "Problem entering a percent: " + t);
                    }
                })
                .setNegativeButton(R.string.button_cancel, (d, w) -> {
                })
                .create();
    }
}

