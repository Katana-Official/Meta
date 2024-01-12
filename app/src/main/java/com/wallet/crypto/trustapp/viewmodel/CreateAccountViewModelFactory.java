package com.wallet.crypto.trustapp.viewmodel;


import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

public class CreateAccountViewModelFactory implements ViewModelProvider.Factory {

	public CreateAccountViewModelFactory() { }

	@NonNull
	@Override
	public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
		return (T) new CreateAccountViewModel();
	}
}
