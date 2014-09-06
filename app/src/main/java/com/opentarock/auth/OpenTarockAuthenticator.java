package com.opentarock.auth;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.api.client.auth.oauth2.TokenResponseException;

import java.io.IOException;

import javax.annotation.Nonnull;

public class OpenTarockAuthenticator extends AbstractAccountAuthenticator {

    private static final String TAG = OpenTarockAuthenticator.class.getSimpleName();

    public static final String REFRESH_TOKEN = "refresh_token";

    public static final String TOKEN_TYPE = "token";

    private final Context mContext;

    private AccountManager mAccountManager;

    public OpenTarockAuthenticator(Context context) {
        super(context);
        mContext = context;
        mAccountManager = AccountManager.get(context);
    }

    @Override
    public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Bundle addAccount(AccountAuthenticatorResponse response, String accountType, String authTokenType, String[] requiredFeatures, Bundle options) throws NetworkErrorException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account, Bundle options) throws NetworkErrorException {
        throw new UnsupportedOperationException();
    }

    @Override
    @Nonnull
    public Bundle getAccountRemovalAllowed(AccountAuthenticatorResponse response, Account account) throws NetworkErrorException {
        return super.getAccountRemovalAllowed(response, account);
    }

    @Override
    public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) throws NetworkErrorException {
        Log.d(TAG, "Getting new auth token ...");

        try {
            Log.d(TAG, "Refreshing token ...");
            Bundle result = AuthUtil.refreshToken(mAccountManager, account, authTokenType);
            if (result != null) {
                return result;
            }
        } catch (TokenResponseException e) {
            if (e.getDetails() != null) {
                Log.d(TAG, "Error: " + e.getDetails().getError());
                if (e.getDetails().getErrorDescription() != null) {
                    Log.d(TAG, e.getDetails().getErrorDescription());
                }
                if (e.getDetails().getErrorUri() != null) {
                    Log.d(TAG, e.getDetails().getErrorUri());
                }
            } else {
                Log.d(TAG, e.getMessage());
            }
        } catch (IOException e) {
            throw new NetworkErrorException(e);
        }

        final Intent loginIntent = new Intent(mContext, LoginActivity.class);
        loginIntent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
        loginIntent.putExtra(LoginActivity.ACCOUNT_USER, account.name);
        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, loginIntent);
        return bundle;
    }

    @Override
    public String getAuthTokenLabel(String authTokenType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) throws NetworkErrorException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account, String[] features) throws NetworkErrorException {
        throw new UnsupportedOperationException();
    }
}
