package com.nineja.chat.firebase

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import com.google.firebase.firestore.FirebaseFirestore
import com.nineja.chat.model.User
import com.nineja.chat.utils.PreferencesManager
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAuthManager @Inject constructor(
    private val context: Context,
    private val preferencesManager: PreferencesManager,
    private val firestore: FirebaseFirestore
) {
    
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private lateinit var googleSignInClient: GoogleSignInClient
    private var verificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    
    interface AuthStateListener {
        fun onSignedIn(user: User)
        fun onSignedOut()
        fun onAuthError(error: String)
    }
    
    interface PhoneAuthListener {
        fun onCodeSent(verificationId: String)
        fun onVerificationCompleted(credential: PhoneAuthCredential)
        fun onVerificationFailed(error: String)
        fun onCodeAutoRetrievalTimeOut()
    }
    
    private var authStateListener: AuthStateListener? = null
    private var phoneAuthListener: PhoneAuthListener? = null
    
    init {
        setupGoogleSignIn()
        setupAuthStateListener()
    }
    
    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("YOUR_WEB_CLIENT_ID") // Replace with your web client ID
            .requestEmail()
            .requestProfile()
            .build()
        
        googleSignInClient = GoogleSignIn.getClient(context, gso)
    }
    
    private fun setupAuthStateListener() {
        auth.addAuthStateListener { firebaseAuth ->
            val firebaseUser = firebaseAuth.currentUser
            if (firebaseUser != null) {
                // User is signed in
                fetchUserProfile(firebaseUser.uid)
            } else {
                // User is signed out
                authStateListener?.onSignedOut()
                preferencesManager.clearUserSession()
            }
        }
    }
    
    // Email/Password Authentication
    suspend fun signUpWithEmail(email: String, password: String, displayName: String): Result<User> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: throw Exception("User creation failed")
            
            // Update display name
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .build()
            firebaseUser.updateProfile(profileUpdates).await()
            
            // Create user document in Firestore
            val user = createUserDocument(firebaseUser, displayName)
            Result.success(user)
            
        } catch (e: Exception) {
            Log.e(TAG, "Email sign up failed", e)
            Result.failure(e)
        }
    }
    
    suspend fun signInWithEmail(email: String, password: String): Result<User> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: throw Exception("Sign in failed")
            
            val user = fetchUserFromFirestore(firebaseUser.uid)
            preferencesManager.saveUserSession(user)
            Result.success(user)
            
        } catch (e: Exception) {
            Log.e(TAG, "Email sign in failed", e)
            Result.failure(e)
        }
    }
    
    // Google Sign-In
    fun getGoogleSignInIntent() = googleSignInClient.signInIntent
    
    suspend fun handleGoogleSignInResult(task: Task<GoogleSignInAccount>): Result<User> {
        return try {
            val account = task.getResult(ApiException::class.java)
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            val result = auth.signInWithCredential(credential).await()
            
            val firebaseUser = result.user ?: throw Exception("Google sign in failed")
            
            // Check if user exists, if not create new user document
            val user = if (result.additionalUserInfo?.isNewUser == true) {
                createUserDocument(firebaseUser, account.displayName ?: "User")
            } else {
                fetchUserFromFirestore(firebaseUser.uid)
            }
            
            preferencesManager.saveUserSession(user)
            Result.success(user)
            
        } catch (e: Exception) {
            Log.e(TAG, "Google sign in failed", e)
            Result.failure(e)
        }
    }
    
    // Phone Authentication
    fun startPhoneNumberVerification(
        activity: Activity,
        phoneNumber: String,
        listener: PhoneAuthListener
    ) {
        this.phoneAuthListener = listener
        
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                Log.d(TAG, "Phone verification completed")
                listener.onVerificationCompleted(credential)
                signInWithPhoneAuthCredential(credential)
            }
            
            override fun onVerificationFailed(e: FirebaseException) {
                Log.w(TAG, "Phone verification failed", e)
                listener.onVerificationFailed(e.message ?: "Verification failed")
            }
            
            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                Log.d(TAG, "Phone verification code sent")
                this@FirebaseAuthManager.verificationId = verificationId
                this@FirebaseAuthManager.resendToken = token
                listener.onCodeSent(verificationId)
            }
            
            override fun onCodeAutoRetrievalTimeOut(verificationId: String) {
                Log.d(TAG, "Phone verification timeout")
                listener.onCodeAutoRetrievalTimeOut()
            }
        }
        
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()
        
        PhoneAuthProvider.verifyPhoneNumber(options)
    }
    
    suspend fun verifyPhoneNumberWithCode(code: String): Result<User> {
        return try {
            val verificationId = this.verificationId ?: throw Exception("No verification ID")
            val credential = PhoneAuthProvider.getCredential(verificationId, code)
            signInWithPhoneAuthCredential(credential)
        } catch (e: Exception) {
            Log.e(TAG, "Phone verification failed", e)
            Result.failure(e)
        }
    }
    
    private suspend fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential): Result<User> {
        return try {
            val result = auth.signInWithCredential(credential).await()
            val firebaseUser = result.user ?: throw Exception("Phone sign in failed")
            
            // Check if user exists, if not create new user document
            val user = if (result.additionalUserInfo?.isNewUser == true) {
                createUserDocument(firebaseUser, "User")
            } else {
                fetchUserFromFirestore(firebaseUser.uid)
            }
            
            preferencesManager.saveUserSession(user)
            Result.success(user)
            
        } catch (e: Exception) {
            Log.e(TAG, "Phone credential sign in failed", e)
            Result.failure(e)
        }
    }
    
    // Anonymous Authentication (for guest users)
    suspend fun signInAnonymously(): Result<User> {
        return try {
            val result = auth.signInAnonymously().await()
            val firebaseUser = result.user ?: throw Exception("Anonymous sign in failed")
            
            val user = createGuestUser(firebaseUser.uid)
            preferencesManager.saveUserSession(user)
            Result.success(user)
            
        } catch (e: Exception) {
            Log.e(TAG, "Anonymous sign in failed", e)
            Result.failure(e)
        }
    }
    
    // Password Reset
    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Password reset failed", e)
            Result.failure(e)
        }
    }
    
    // Update Password
    suspend fun updatePassword(newPassword: String): Result<Unit> {
        return try {
            val user = auth.currentUser ?: throw Exception("No authenticated user")
            user.updatePassword(newPassword).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Password update failed", e)
            Result.failure(e)
        }
    }
    
    // Update Email
    suspend fun updateEmail(newEmail: String): Result<Unit> {
        return try {
            val user = auth.currentUser ?: throw Exception("No authenticated user")
            user.updateEmail(newEmail).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Email update failed", e)
            Result.failure(e)
        }
    }
    
    // Link Accounts
    suspend fun linkWithPhoneCredential(credential: PhoneAuthCredential): Result<Unit> {
        return try {
            val user = auth.currentUser ?: throw Exception("No authenticated user")
            user.linkWithCredential(credential).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Account linking failed", e)
            Result.failure(e)
        }
    }
    
    // Delete Account
    suspend fun deleteAccount(): Result<Unit> {
        return try {
            val user = auth.currentUser ?: throw Exception("No authenticated user")
            
            // Delete user document from Firestore
            firestore.collection("users").document(user.uid).delete().await()
            
            // Delete Firebase Auth account
            user.delete().await()
            
            preferencesManager.clearUserSession()
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Account deletion failed", e)
            Result.failure(e)
        }
    }
    
    // Sign Out
    fun signOut() {
        try {
            auth.signOut()
            googleSignInClient.signOut()
            preferencesManager.clearUserSession()
        } catch (e: Exception) {
            Log.e(TAG, "Sign out failed", e)
        }
    }
    
    // User Management
    private suspend fun createUserDocument(firebaseUser: FirebaseUser, displayName: String): User {
        val user = User(
            id = firebaseUser.uid,
            username = generateUsername(displayName),
            displayName = displayName,
            email = firebaseUser.email ?: "",
            phoneNumber = firebaseUser.phoneNumber ?: "",
            profileImageUrl = firebaseUser.photoUrl?.toString() ?: "",
            isVerified = firebaseUser.isEmailVerified,
            createdAt = System.currentTimeMillis(),
            lastActiveAt = System.currentTimeMillis(),
            country = "Nigeria", // Default for 9jaChat
            language = "en"
        )
        
        // Save to Firestore
        firestore.collection("users").document(user.id).set(user).await()
        return user
    }
    
    private fun createGuestUser(uid: String): User {
        return User(
            id = uid,
            username = "guest_${uid.take(8)}",
            displayName = "Guest User",
            email = "",
            phoneNumber = "",
            profileImageUrl = "",
            isGuest = true,
            createdAt = System.currentTimeMillis(),
            lastActiveAt = System.currentTimeMillis(),
            country = "Nigeria",
            language = "en"
        )
    }
    
    private suspend fun fetchUserFromFirestore(uid: String): User {
        val document = firestore.collection("users").document(uid).get().await()
        return document.toObject(User::class.java) ?: throw Exception("User not found")
    }
    
    private fun fetchUserProfile(uid: String) {
        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val user = document.toObject(User::class.java)
                    if (user != null) {
                        preferencesManager.saveUserSession(user)
                        authStateListener?.onSignedIn(user)
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Failed to fetch user profile", exception)
                authStateListener?.onAuthError(exception.message ?: "Unknown error")
            }
    }
    
    private fun generateUsername(displayName: String): String {
        val cleanName = displayName.replace(Regex("[^a-zA-Z0-9]"), "").lowercase()
        val randomSuffix = (1000..9999).random()
        return "${cleanName}_$randomSuffix"
    }
    
    // Getters
    fun getCurrentUser(): FirebaseUser? = auth.currentUser
    
    fun getCurrentUserId(): String? = auth.currentUser?.uid
    
    fun isSignedIn(): Boolean = auth.currentUser != null
    
    fun isEmailVerified(): Boolean = auth.currentUser?.isEmailVerified ?: false
    
    // Listeners
    fun setAuthStateListener(listener: AuthStateListener) {
        this.authStateListener = listener
    }
    
    fun removeAuthStateListener() {
        this.authStateListener = null
    }
    
    companion object {
        private const val TAG = "FirebaseAuthManager"
    }
}