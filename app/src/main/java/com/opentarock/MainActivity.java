package com.opentarock;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.opentarock.auth.LoginActivity;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class MainActivity extends Activity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private AccountManager mAccountManager;

    @InjectView(R.id.get_token)
    Button mGetTokenButton;

    @InjectView(R.id.invalidate_token)
    Button mInvalidateTokenButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.inject(this);

        mAccountManager = AccountManager.get(this);

        final SharedPreferences sharedPrefs = getSharedPreferences(
                getString(R.string.preference_file_accounts),
                Context.MODE_PRIVATE);
        final String accountName = sharedPrefs.getString(getString(R.string.pref_default_account), "");

        if (accountName.isEmpty()) {
            Log.d(TAG, "No default account");
            Intent accountLoginIntent = new Intent(getApplicationContext(), LoginActivity.class);
            startActivity(accountLoginIntent);
            finish();
        }

        final String accountType = getString(R.string.account_type);
        mGetTokenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Account account = new Account(accountName, accountType);
                final AccountManagerFuture<Bundle> future = mAccountManager.getAuthToken(account, "", null, MainActivity.this, null, null);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "Getting auth token");
                        try {
                            Bundle result = future.getResult();
                            final String authToken = result.getString(AccountManager.KEY_AUTHTOKEN);
                            Log.d(TAG, "Token is: " + authToken);
                        } catch (Exception e) {
                            e.printStackTrace();
                            Log.d(TAG, "There was a problem getting the token");
                        }
                    }
                }).start();
            }
        });

        mInvalidateTokenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Account account = new Account(accountName, accountType);
                final AccountManagerFuture<Bundle> future = mAccountManager.getAuthToken(account, "", null, MainActivity.this, null, null);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Bundle result = future.getResult();
                            final String authToken = result.getString(AccountManager.KEY_AUTHTOKEN);
                            Log.d(TAG, "Token is: " + authToken);
                            final String accountType = getString(R.string.account_type);
                            mAccountManager.invalidateAuthToken(accountType, authToken);
                        } catch (Exception e) {
                            e.printStackTrace();
                            Log.d(TAG, "There was a problem getting the token");
                        }
                    }
                }).start();
            }
        });
    }
}
