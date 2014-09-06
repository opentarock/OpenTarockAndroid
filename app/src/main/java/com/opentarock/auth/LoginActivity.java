package com.opentarock.auth;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.api.client.auth.oauth2.TokenResponseException;
import com.opentarock.MainActivity;
import com.opentarock.R;

import java.io.IOException;

import javax.annotation.Nonnull;

import butterknife.ButterKnife;
import butterknife.InjectView;


/**
 * A login screen that offers login via email and password.
 */
public class LoginActivity extends AccountAuthenticatorActivity {

    private static final String TAG = LoginActivity.class.getSimpleName();

    public static final String ACCOUNT_USER = "account_user";

    private UserLoginTask mAuthTask = null;

    @InjectView(R.id.email_static)
    TextView mEmailStaticView;

    @InjectView(R.id.email)
    EditText mEmailView;

    @InjectView(R.id.password)
    EditText mPasswordView;

    @InjectView(R.id.login_progress)
    ProgressBar mProgressView;

    @InjectView(R.id.email_sign_in_button)
    Button mSignInButton;

    private String mUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        ButterKnife.inject(this);

        setTitle(R.string.title);

        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == EditorInfo.IME_ACTION_DONE) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        TextWatcher inputTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                // To enable the Sign In button both email and password fields must not be empty
                // or just the password field if logging into an existing account.
                mSignInButton.setEnabled((mEmailView.length() > 0 || mEmailView.getVisibility() == View.GONE) && mPasswordView.length() > 0);
            }
        };

        mEmailView.addTextChangedListener(inputTextWatcher);
        mPasswordView.addTextChangedListener(inputTextWatcher);

        Button mEmailSignInButton = (Button) findViewById(R.id.email_sign_in_button);
        mEmailSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        Intent intent = getIntent();
        // If the user account is set in an intent we display it in a TextView and just require
        // just the password from the user.
        if (intent.hasExtra(ACCOUNT_USER)) {
            mUser = intent.getStringExtra(ACCOUNT_USER);
            mEmailStaticView.setText(mUser);
            mEmailStaticView.setVisibility(View.VISIBLE);
        } else {
            mEmailView.setVisibility(View.VISIBLE);
        }
    }

    public void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }

        String email = mEmailView.getText().toString();
        if (mUser != null) {
            email = mUser;
        }
        String password = mPasswordView.getText().toString();

        showProgress(true);
        disableInput(true);
        mAuthTask = new UserLoginTask(email, password);
        mAuthTask.execute();
    }

    /**
     * Show the progress bar.
     */
    private void showProgress(final boolean show) {
        mProgressView.setIndeterminate(show);
    }

    /**
     * Disables the input on all form elements allowing user input.
     * Used during login to prevent user from changing the values while waiting for response;
     */
    private void disableInput(final boolean disabled) {
        mPasswordView.setEnabled(!disabled);
    }

    /**
     * Background task doing the actual login process.
     */
    public class UserLoginTask extends AsyncTask<Void, Void, Integer> {

        private final String mEmail;
        private final String mPassword;

        private final AccountManager mAccountManager;

        UserLoginTask(String email, String password) {
            mEmail = email;
            mPassword = password;
            mAccountManager = AccountManager.get(LoginActivity.this);
        }

        @Override
        protected Integer doInBackground(Void... params) {
            final String accountType = getString(R.string.account_type);

            final Account availableAccounts[] = mAccountManager.getAccountsByType(accountType);
            Account account = findAccountWithEmail(availableAccounts, mEmail);
            if (account == null) {
                Log.d(TAG, "Adding account");
                account = createAccount(mEmail, accountType);
            }

            try {
                Log.d(TAG, "Getting token from server ...");
                final Bundle result = AuthUtil.getToken(mAccountManager, account, mPassword);
                setAccountAuthenticatorResult(result);
            } catch (TokenResponseException e) {
                int errorMessageResource = R.string.error_server_error;
                if (e.getDetails() != null) {
                    String error = e.getDetails().getError();
                    if (error.equals("invalid_grant")) {
                        errorMessageResource = R.string.error_credentials_mismatch;
                    }
                    Log.d(TAG, "Oauth2 error: " + error);
                    String errorDescription = e.getDetails().getErrorDescription();
                    if (errorDescription != null) {
                        Log.d(TAG, errorDescription);
                    }
                    if (e.getDetails().getErrorUri() != null) {
                        Log.d(TAG, e.getDetails().getErrorUri());
                    }
                } else {
                    Log.d(TAG, e.getMessage());
                }
                return errorMessageResource;
            } catch (IOException e) {
                return R.string.error_network_connection;
            }
            return -1;
        }

        /**
         * Create a new account and stores it in {@link android.accounts.AccountManager}.
         * On success account name is stored in {@link android.content.SharedPreferences} as default.
         *
         * @param accountType Type of created account
         * @return Created {@link android.accounts.Account}
         */
        private Account createAccount(String accountName, String accountType) {
            Account account = new Account(accountName, accountType);
            boolean result = mAccountManager.addAccountExplicitly(account, null, null);
            if (!result) {
                throw new RuntimeException(String.format("Unknown error when adding account: %s", accountName));
            } else {
                final SharedPreferences sharedPrefs = getSharedPreferences(
                        getString(R.string.preference_file_accounts),
                        Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPrefs.edit();
                editor.putString(getString(R.string.pref_default_account), mEmail);
                editor.commit();
            }
            return account;
        }

        /**
         * Find an account with email in an array of accounts.
         *
         * @param accounts Array of account
         * @param email    Email address of account
         * @return {@link android.accounts.Account} with specified email or null if no such account was found.
         */
        private Account findAccountWithEmail(@Nonnull Account[] accounts, String email) {
            for (Account account : accounts) {
                if (account.name.equals(email)) {
                    return account;
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(final Integer resId) {
            resetViewState();

            if (resId == -1) {
                // Return to the Main activity after successful login.
                Intent mainIntent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(mainIntent);
                finish();
            } else {
                mPasswordView.requestFocus();
                Toast.makeText(
                        LoginActivity.this,
                        getString(resId),
                        Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected void onCancelled() {
            resetViewState();
        }

        /**
         * Returns the view to the original state only leaving entered email address.
         */
        private void resetViewState() {
            mAuthTask = null;
            showProgress(false);
            disableInput(false);
            mPasswordView.setText("");
        }
    }
}



