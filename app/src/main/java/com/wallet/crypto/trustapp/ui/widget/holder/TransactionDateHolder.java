package com.wallet.crypto.trustapp.ui.widget.holder;

import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.wallet.crypto.trustapp.R;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class TransactionDateHolder extends BinderViewHolder<Date> {

//    private static final String DATE_TEMPLATE = "MM/dd/yy H:mm:ss zzz";

    public static final int VIEW_TYPE = 1004;
    private final TextView title;

    public TransactionDateHolder(int resId, ViewGroup parent) {
        super(resId, parent);

        title = findViewById(R.id.title);
    }

    @Override
    public void bind(@Nullable Date data, @NonNull Bundle addition) {
        if (data == null) {
            title.setText(null);
        } else {
            java.text.DateFormat dateFormat = DateFormat.getMediumDateFormat(getContext());
            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
            calendar.setTime(data);
            title.setText(dateFormat.format(calendar.getTime()));
        }
    }
}
