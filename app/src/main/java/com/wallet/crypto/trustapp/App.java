package com.wallet.crypto.trustapp;

import android.app.Application;
import android.content.Context;

import com.wallet.crypto.trustapp.di.DaggerAppComponent;

import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;

import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.HasAndroidInjector;
import io.realm.Realm;
import net_62v.external.IMundoProcessCallback;
import net_62v.external.MetaCore;

public class App extends Application implements HasAndroidInjector, IMundoProcessCallback {

	@Override
	protected void attachBaseContext(Context base) {
		super.attachBaseContext(base);
		MetaCore.setProcessLifecycleCallback(this);
	}

	@Override
	public void onCreate(String s, String s1, String s2) {
		IMundoProcessCallback.super.onCreate(s, s1, s2);
		// Mundo internal application
	}

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
