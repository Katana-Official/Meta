package com.wallet.crypto.trustapp;

import android.content.Context;

import androidx.multidex.MultiDexApplication;

import com.wallet.crypto.trustapp.di.DaggerAppComponent;

import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;

import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.HasAndroidInjector;
import io.realm.Realm;

public class App extends MultiDexApplication implements HasAndroidInjector {

	@Inject
	DispatchingAndroidInjector<Object> dispatchingAndroidInjector;
    @NotNull
    public static Context context;

    @Override
	public void onCreate() {
		super.onCreate();
		context = this;
        Realm.init(this);
        DaggerAppComponent
				.builder()
				.application(this)
				.build()
				.inject(this);
	}

	@Override
	public AndroidInjector<Object> androidInjector() {
		return dispatchingAndroidInjector;
	}
}
