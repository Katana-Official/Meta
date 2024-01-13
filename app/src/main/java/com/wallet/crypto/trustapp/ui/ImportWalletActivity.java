package com.wallet.crypto.trustapp.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.viewpager.widget.ViewPager;

import android.widget.ProgressBar;

import com.google.android.material.tabs.TabLayout;
import com.wallet.crypto.trustapp.C;
import com.wallet.crypto.trustapp.R;
import com.wallet.crypto.trustapp.entity.ErrorEnvelope;
import com.wallet.crypto.trustapp.entity.Wallet;
import com.wallet.crypto.trustapp.ui.widget.adapter.TabPagerAdapter;
import com.wallet.crypto.trustapp.viewmodel.ImportWalletViewModel;
import com.wallet.crypto.trustapp.viewmodel.ImportWalletViewModelFactory;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

public class ImportWalletActivity extends BaseActivity {

    private static final int KEYSTORE_FORM_INDEX = 0;
    private static final int PRIVATE_KEY_FORM_INDEX = 1;

    private final List<Pair<String, Fragment>> pages = new ArrayList<>();

    @Inject
    ImportWalletViewModelFactory importWalletViewModelFactory;
    ImportWalletViewModel importWalletViewModel;
    private Dialog dialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_import_wallet);
        toolbar();

        pages.add(KEYSTORE_FORM_INDEX, new Pair<>(getString(R.string.tab_keystore), ImportKeystoreFragment.create()));
        pages.add(PRIVATE_KEY_FORM_INDEX, new Pair<>(getString(R.string.tab_private_key), ImportPrivateKeyFragment.create()));
        ViewPager viewPager = findViewById(R.id.viewPager);
        viewPager.setAdapter(new TabPagerAdapter(getSupportFragmentManager(), pages));
        TabLayout tabLayout = findViewById(R.id.tabLayout);
        tabLayout.setupWithViewPager(viewPager);

        importWalletViewModel = ViewModelProviders.of(this, importWalletViewModelFactory)
                .get(ImportWalletViewModel.class);
        importWalletViewModel.progress().observe(this, this::onProgress);
        importWalletViewModel.error().observe(this, this::onError);
        importWalletViewModel.wallet().observe(this, this::onWallet);
    }

    @Override
    protected void onResume() {
        super.onResume();

        ((ImportKeystoreFragment) pages.get(KEYSTORE_FORM_INDEX).second)
                .setOnImportKeystoreListener(importWalletViewModel);
        ((ImportPrivateKeyFragment) pages.get(PRIVATE_KEY_FORM_INDEX).second)
                .setOnImportPrivateKeyListener(importWalletViewModel);
    }

    @Override
    protected void onPause() {
        super.onPause();

        hideDialog();
    }

    private void onWallet(Wallet wallet) {
        Intent result = new Intent();
        result.putExtra(C.Key.WALLET, wallet);
        setResult(RESULT_OK, result);
        finish();
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);
        super.onBackPressed();
    }

    private void onError(ErrorEnvelope errorEnvelope) {
        hideDialog();
        dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.title_dialog_error)
                .setMessage(R.string.error_import)
                .setPositiveButton(R.string.ok, null)
                .create();
        dialog.show();
    }

    private void onProgress(boolean shouldShowProgress) {
        hideDialog();
        if (shouldShowProgress) {
            dialog = new AlertDialog.Builder(this)
                    .setTitle(R.string.title_dialog_handling)
                    .setView(new ProgressBar(this))
                    .setCancelable(false)
                    .create();
            try{
                dialog.show();
            }catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    private void hideDialog() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }
}
