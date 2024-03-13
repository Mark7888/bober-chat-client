# google-services.json

1. Go to the Firebase console.
2. Select a project.
3. In the center of your project overview page, click the Android icon to launch the setup workflow.
4. Follow the setup workflow to add a Firebase configuration file (google-services.json) to your app.
5. encode the google-services.json file to base64
   `base64 google-services.json`
6. Add the base64 encoded string to the repository secrets as `GOOGLE_SERVICES_JSON`

# Firebase SDK

On a machine with a browser, install the Firebase CLI.
`curl -sL firebase.tools | bash`

Start the signin process by running the following command:
`firebase login:ci`

1. Visit the URL provided, then log in using a Google account.
2. Print a new refresh token. The current CLI session will not be affected.
3. Store the output token in a secure but accessible way in your CI system.
4. Add this token to the repository secrets as `FIREBASE_TOKEN`

Also add the following to the repository secrets:

- `FIREBASE_PROJECT_ID` - the Firebase project ID
- `FIREBASE_APP_ID` - the Firebase app ID

firebase apps:android:sha:create `APP_ID` `SHA_HASH` --project `PROJECT_ID`

# JarSigner

1. Create a keystore
   `keytool -genkey -v -keystore my-release-key.keystore -alias bober_alias -keyalg RSA -validity 10000`
2. Enter your details as prompted. Remember the password you set.
3. Encrypt your keystore file with `base64 my-release-key.keystore` and add the output to the repository secrets as `JARSIGNER_KEYSTORE`
4. Add the following to the repository secrets:
   - `KEYSTORE_PASSWORD` - the password you set for the keystore
   - `KEY_PASSWORD` - the password you set for the key (probably the same as the keystore password)
