package com.luminesim.futureplanner.input;

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import android.view.View;
import android.view.WindowManager;
import android.widget.CalendarView;

import com.luminesim.futureplanner.R;

import java.util.GregorianCalendar;

/**
 *
 */
public class CalendarInputFragment extends AlertDialogFragment {

    private View toStart;
    private CalendarView mCalendar;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        toStart = getActivity().getLayoutInflater().inflate(R.layout.fragment_calendar_input, null);
        mCalendar = toStart.findViewById(R.id.datePicker);
        mCalendar.setOnDateChangeListener((v, y, m, d) -> {
            GregorianCalendar c = new GregorianCalendar(y, m, d);
            mCalendar.setDate(c.getTime().getTime());
        });
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