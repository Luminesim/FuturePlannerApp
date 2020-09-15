package com.luminesim.futureplanner.ui.main;

import androidx.arch.core.util.Function;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import lombok.Getter;
import lombok.Setter;

public class PageViewModel extends ViewModel {


    private MutableLiveData<Integer> mIndex = new MutableLiveData<>();
    private LiveData<String> mText = Transformations.map(mIndex, new Function<Integer, String>() {
        @Override
        public String apply(Integer input) {
            return "Hello world from section: " + input;
        }
    });

    @Getter
    @Setter
    private MutableLiveData<Boolean> simulationRunFlag = new MutableLiveData<>(true);

    public void setIndex(int index) {
        mIndex.setValue(index);
    }

    public int getIndex() {
        return  mIndex.getValue();
    }

    public LiveData<String> getText() {
        return mText;
    }
}