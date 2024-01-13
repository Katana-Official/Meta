package com.wallet.crypto.trustapp.entity;

public class ServiceException extends Exception {
	public transient final ErrorEnvelope error;

	public ServiceException(String message) {
		super(message);

		error = new ErrorEnvelope(message);
	}
}
