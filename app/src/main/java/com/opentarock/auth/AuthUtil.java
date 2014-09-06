package com.opentarock.auth;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.os.Bundle;

import com.google.api.client.auth.oauth2.PasswordTokenRequest;
import com.google.api.client.auth.oauth2.RefreshTokenRequest;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.http.BasicAuthentication;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;

import java.io.IOException;

public class AuthUtil {

    private static final String SERVICE_URL = "http://192.168.1.2:8080/token";

    public static final String CLIENT_ID = "client_id";
    public static final String CLIENT_SECRET = "client_secret";

    static Bundle refreshToken(AccountManager accountManager, Account account, String authTokenType) throws IOException {
        final String refreshToken = accountManager.getUserData(account, OpenTarockAuthenticator.REFRESH_TOKEN);
        if (refreshToken == null) {
            return null;
        }
        TokenResponse tokenResponse =
                new RefreshTokenRequest(new NetHttpTransport(), new JacksonFactory(),
                        new GenericUrl(SERVICE_URL), refreshToken)
                        .setClientAuthentication(
                                new BasicAuthentication(CLIENT_ID, CLIENT_SECRET)).execute();

        accountManager.setAuthToken(account, authTokenType, tokenResponse.getAccessToken());
        accountManager.setUserData(account, OpenTarockAuthenticator.REFRESH_TOKEN, tokenResponse.getRefreshToken());

        final Bundle result = new Bundle();
        createResponseBundle(account, tokenResponse.getAccessToken(), result);
        return result;
    }

    static Bundle getToken(AccountManager accountManager, Account account, String password) throws IOException {
        TokenResponse tokenResponse =
                new PasswordTokenRequest(new NetHttpTransport(), new JacksonFactory(),
                        new GenericUrl(SERVICE_URL), account.name, password)
                        .setClientAuthentication(
                                new BasicAuthentication(CLIENT_ID, CLIENT_SECRET)).execute();

        accountManager.setAuthToken(account, OpenTarockAuthenticator.TOKEN_TYPE, tokenResponse.getAccessToken());
        accountManager.setUserData(account, OpenTarockAuthenticator.REFRESH_TOKEN, tokenResponse.getRefreshToken());

        final Bundle result = new Bundle();
        createResponseBundle(account, tokenResponse.getAccessToken(), result);
        return result;
    }

    private static void createResponseBundle(Account account, String token, Bundle result) {
        result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
        result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
        result.putString(AccountManager.KEY_AUTHTOKEN, token);
    }
}
