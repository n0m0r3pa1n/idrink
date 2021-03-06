package com.nmp90.idrink.di.bars;

import com.nmp90.idrink.di.scopes.FragmentScoped;
import com.nmp90.idrink.ui.activities.MainActivity;

import dagger.Subcomponent;

/**
 * Created by joro on 05.08.16.
 */

@FragmentScoped
@Subcomponent(modules = {BarsModule.class})
public interface BarsComponent {
    void inject(MainActivity barListFragment);
}
