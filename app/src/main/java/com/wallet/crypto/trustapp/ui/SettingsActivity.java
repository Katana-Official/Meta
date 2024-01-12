package com.wallet.crypto.trustapp.ui;

import android.app.Fragment;
import android.os.Bundle;
import android.view.MenuItem;

import com.wallet.crypto.trustapp.R;
import com.wallet.crypto.trustapp.router.TransactionsRouter;

import javax.inject.Inject;

import dagger.android.AndroidInjection;
import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.HasAndroidInjector;

public class SettingsActivity extends BaseActivity implements HasAndroidInjector {

    @Inject
    DispatchingAndroidInjector<Object> fragmentInjector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        toolbar();
        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new SettingsFragment())
                .commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                new TransactionsRouter().open(this, true);
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        new TransactionsRouter().open(this, true);
    }

    @Override
    public AndroidInjector<Object> androidInjector() {
        return fragmentInjector;
    }
}
