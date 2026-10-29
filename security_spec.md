# Security Specification — Katalog Yap (Firebase Auth & Firestore)

## 1. Authentication & Identity Model
- **Interactive Google Sign-In**: Uses Jetpack `CredentialManager` with `GetSignInWithGoogleOption` (`R.string.default_web_client_id`) and `Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null))`.
- **Phone + Password Login**: Users can register/set a password for their phone number and sign in directly with phone number + password. When linked with Firebase Auth, user account metadata and password reset events are synced to Firestore `/users/{userId}`.
- **Password Reset via Firebase**: Users can trigger `Firebase.auth.sendPasswordResetEmail(email)` and record password reset metadata in `/users/{userId}`.

## 2. Firestore Collections & Ownership
### `/users/{userId}` (`UserAccount`)
- **Document ID (`userId`)**: Must equal `request.auth.uid`.
- **Read (`get`, `list`)**: Allowed only when `isSignedIn() && request.auth.uid == userId`.
- **Create (`create`)**:
  - Requires `isSignedIn() && isValidId(userId) && request.auth.uid == userId`.
  - Requires `incoming().userId == request.auth.uid`.
  - Required keys: `['userId', 'phone', 'email', 'displayName', 'provider', 'passwordHash', 'passwordResetRequestedAt', 'createdAt', 'updatedAt']`.
  - Field validations:
    - `userId`: string, 1..128 chars
    - `phone`: string, 0..32 chars
    - `email`: string, 0..254 chars
    - `displayName`: string, 0..120 chars
    - `provider`: string in `['phone_password', 'google']`
    - `passwordHash`: string, 0..256 chars
    - `passwordResetRequestedAt`: int >= 0
    - `createdAt`: timestamp (`request.time`)
    - `updatedAt`: timestamp (`request.time`)
- **Update (`update`)**:
  - Requires `isSignedIn() && request.auth.uid == userId`.
  - `userId` and `createdAt` are immutable (`incoming().userId == existing().userId && incoming().createdAt == existing().createdAt`).
  - Only mutable fields (`phone`, `email`, `displayName`, `provider`, `passwordHash`, `passwordResetRequestedAt`, `updatedAt`) may change, and the resulting document must satisfy `isValidUserAccount(incoming())`.
- **Delete (`delete`)**:
  - Allowed only for the owner (`isSignedIn() && request.auth.uid == userId`).
