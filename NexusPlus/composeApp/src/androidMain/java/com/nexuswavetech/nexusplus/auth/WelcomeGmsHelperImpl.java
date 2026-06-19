package com.nexuswavetech.nexusplus.auth;

import android.content.Context;
import android.content.Intent;
import androidx.activity.result.ActivityResultLauncher;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

public final class WelcomeGmsHelperImpl implements WelcomeGmsHelper.Impl {

    @Override
    public void handleSignInResult(Intent data, WelcomeViewModel viewModel) {
        try {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            GoogleSignInAccount account = task.getResult(ApiException.class);
            String token = account.getIdToken();
            if (token != null) {
                viewModel.onGoogleSignInTokenReceived(token);
            } else {
                viewModel.onGoogleSignInError("No ID token — check Firebase SHA-1 fingerprint");
            }
        } catch (ApiException e) {
            viewModel.onGoogleSignInError("Sign-in failed (code " + e.getStatusCode() + ")");
        } catch (Exception e) {
            viewModel.onGoogleSignInError("Sign-in error: " + e.getMessage());
        }
    }

    @Override
    public void launchSignIn(
            Context context,
            String webClientId,
            WelcomeViewModel viewModel,
            ActivityResultLauncher<Intent> launcher
    ) {
        try {
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(webClientId)
                    .requestEmail()
                    .build();
            GoogleSignInClient client = GoogleSignIn.getClient(context, gso);
            client.signOut().addOnCompleteListener(t -> launcher.launch(client.getSignInIntent()));
        } catch (Exception e) {
            viewModel.onGoogleSignInError(
                    "Google Sign-In not configured — add a valid google-services.json with OAuth web client ID"
            );
        }
    }
}
