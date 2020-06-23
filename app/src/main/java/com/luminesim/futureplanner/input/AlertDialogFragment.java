package com.luminesim.futureplanner.input;

import android.view.View;

import androidx.fragment.app.DialogFragment;

import java.util.function.Consumer;

import lombok.Setter;

public abstract class AlertDialogFragment  extends DialogFragment {

    @Setter
    protected Consumer<View> positiveButtonCallback;
}
