package com.opentarock.auth;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class OpenTarockAuthenticatorService extends Service {
    @Override
    public IBinder onBind(Intent intent) {
        OpenTarockAuthenticator authenticator = new OpenTarockAuthenticator(this);
        return authenticator.getIBinder();
    }
}
