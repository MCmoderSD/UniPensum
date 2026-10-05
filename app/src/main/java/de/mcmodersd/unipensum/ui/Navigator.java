package de.mcmodersd.unipensum.ui;

import androidx.fragment.app.Fragment;

public interface Navigator {
    void push(Fragment fragment);
    void pop();

    static Navigator of(Fragment fragment) {
        return (Navigator) fragment.requireActivity();
    }
}