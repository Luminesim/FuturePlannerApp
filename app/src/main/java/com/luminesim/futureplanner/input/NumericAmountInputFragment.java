package com.luminesim.futureplanner.input;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.luminesim.futureplanner.R;

import java.util.function.BiFunction;

import ca.anthrodynamics.indes.lang.ComputableMonad;
import ca.anthrodynamics.indes.lang.Monad;

public class NumericAmountInputFragment extends AlertDialogFragment {

    private View toStart;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        toStart = getActivity().getLayoutInflater().inflate(R.layout.fragment_numeric_amount_input, null);
        return new AlertDialog.Builder(getContext())
                .setView(toStart)
                .setPositiveButton(R.string.button_save, (d, which) -> {
                    positiveButtonCallback.accept(toStart);
                })
                .setNegativeButton(R.string.button_cancel, (d, w) -> {
                })
                .create();
    }

    public static ComputableMonad makeComputable(Monad template, View inputs) {
        return template.withParameters(Double.valueOf(((EditText) inputs.findViewById(R.id.inputNumber)).getText().toString()));
    }
}

