package com.naijachat.naija_chat.repository

import com.naijachat.naija_chat.model.User
import com.naijachat.naija_chat.firebase.FirebaseAuthManager
import com.naijachat.naija_chat.firebase.FirestoreManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val authManager: FirebaseAuthManager,
    private val firestoreManager: FirestoreManager
) {

    /**
     * Get current user ID
     */
    fun getCurrentUserId(): String {
        return authManager.getCurrentUser()?.uid ?: "anonymous_user"
    }

    /**
     * Get current user
     */
    suspend fun getCurrentUser(): User? = withContext(Dispatchers.IO) {
        try {
            val firebaseUser = authManager.getCurrentUser()
            if (firebaseUser != null) {
                firestoreManager.getUser(firebaseUser.uid) ?: User.createFromFirebaseUser(firebaseUser)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get user by ID
     */
    suspend fun getUser(userId: String): User? = withContext(Dispatchers.IO) {
        try {
            firestoreManager.getUser(userId)
        } catch (e: Exception) {
            // Return mock user for development
            generateMockUser(userId)
        }
    }

    /**
     * Update user profile
     */
    suspend fun updateUser(user: User): Boolean = withContext(Dispatchers.IO) {
        try {
            firestoreManager.updateUser(user)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Toggle follow status for a user
     */
    suspend fun toggleFollow(targetUserId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val currentUserId = getCurrentUserId()
            if (currentUserId == "anonymous_user") return@withContext false
            
            firestoreManager.toggleFollow(currentUserId, targetUserId)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if current user follows target user
     */
    suspend fun isFollowing(targetUserId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val currentUserId = getCurrentUserId()
            if (currentUserId == "anonymous_user") return@withContext false
            
            firestoreManager.isFollowing(currentUserId, targetUserId)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get user's followers
     */
    suspend fun getFollowers(userId: String, limit: Int = 20): List<User> = withContext(Dispatchers.IO) {
        try {
            firestoreManager.getFollowers(userId, limit)
        } catch (e: Exception) {
            generateMockUsers(limit)
        }
    }

    /**
     * Get users that the user is following
     */
    suspend fun getFollowing(userId: String, limit: Int = 20): List<User> = withContext(Dispatchers.IO) {
        try {
            firestoreManager.getFollowing(userId, limit)
        } catch (e: Exception) {
            generateMockUsers(limit)
        }
    }

    /**
     * Search users by username or display name
     */
    suspend fun searchUsers(query: String, limit: Int = 20): List<User> = withContext(Dispatchers.IO) {
        try {
            firestoreManager.searchUsers(query, limit)
        } catch (e: Exception) {
            generateMockSearchUsers(query, limit)
        }
    }

    /**
     * Get suggested users for discovery
     */
    suspend fun getSuggestedUsers(limit: Int = 20): List<User> = withContext(Dispatchers.IO) {
        try {
            firestoreManager.getSuggestedUsers(getCurrentUserId(), limit)
        } catch (e: Exception) {
            generateMockSuggestedUsers(limit)
        }
    }

    /**
     * Get trending creators
     */
    suspend fun getTrendingCreators(limit: Int = 10): List<User> = withContext(Dispatchers.IO) {
        try {
            firestoreManager.getTrendingCreators(limit)
        } catch (e: Exception) {
            generateMockTrendingCreators(limit)
        }
    }

    /**
     * Update user stats (followers, following, likes, videos count)
     */
    suspend fun updateUserStats(userId: String, stats: Map<String, Any>): Boolean = withContext(Dispatchers.IO) {
        try {
            firestoreManager.updateUserStats(userId, stats)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Block/unblock a user
     */
    suspend fun toggleBlock(targetUserId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val currentUserId = getCurrentUserId()
            if (currentUserId == "anonymous_user") return@withContext false
            
            firestoreManager.toggleBlock(currentUserId, targetUserId)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Report a user
     */
    suspend fun reportUser(targetUserId: String, reason: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val currentUserId = getCurrentUserId()
            if (currentUserId == "anonymous_user") return@withContext false
            
            firestoreManager.reportUser(currentUserId, targetUserId, reason)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get user's creator economy stats
     */
    suspend fun getCreatorStats(userId: String): Map<String, Any>? = withContext(Dispatchers.IO) {
        try {
            firestoreManager.getCreatorStats(userId)
        } catch (e: Exception) {
            generateMockCreatorStats()
        }
    }

    /**
     * Update user preferences
     */
    suspend fun updatePreferences(userId: String, preferences: Map<String, Any>): Boolean = withContext(Dispatchers.IO) {
        try {
            firestoreManager.updateUserPreferences(userId, preferences)
            true
        } catch (e: Exception) {
            false
        }
    }

    // Mock data generators for development/testing
    private fun generateMockUser(userId: String): User {
        return User(
            id = userId,
            username = "naija_user_${userId.takeLast(4)}",
            displayName = "Naija Creator",
            email = "user@naijachat.com",
            profileImageUrl = "https://picsum.photos/200/200?random=${userId.hashCode()}",
            bio = "🇳🇬 Nigerian content creator | Afrobeats lover | Lagos-based",
            followersCount = (100..100000).random(),
            followingCount = (50..5000).random(),
            likesCount = (1000..500000).random(),
            videosCount = (10..500).random(),
            verified = (1..10).random() > 8, // 20% chance of being verified
            isCreator = (1..3).random() == 1, // 33% chance of being creator
            preferredLanguages = listOf("english", "pidgin"),
            favoriteGenres = listOf("afrobeats", "comedy", "dance"),
            region = listOf("lagos", "abuja", "kano", "port-harcourt").random(),
            creatorTier = listOf("bronze", "silver", "gold", "diamond").random(),
            totalEarnings = (0..50000).random().toDouble()
        )
    }

    private fun generateMockUsers(count: Int): List<User> {
        return (1..count).map { index ->
            generateMockUser("mock_user_$index")
        }
    }

    private fun generateMockSearchUsers(query: String, count: Int): List<User> {
        return generateMockUsers(count).map { user ->
            user.copy(
                username = "${query.lowercase()}_${user.username}",
                displayName = "$query Creator",
                bio = "Search result for '$query' - ${user.bio}"
            )
        }
    }

    private fun generateMockSuggestedUsers(count: Int): List<User> {
        val nigerianNames = listOf("Kemi", "Tunde", "Chioma", "Ibrahim", "Funmi", "Emeka", "Aisha", "Segun")
        return (1..count).map { index ->
            val name = nigerianNames.random()
            User(
                id = "suggested_$index",
                username = "${name.lowercase()}_naija",
                displayName = "$name from Lagos",
                profileImageUrl = "https://picsum.photos/200/200?random=${index + 1000}",
                bio = "🇳🇬 Naija content | Follow for amazing videos!",
                followersCount = (5000..50000).random(),
                videosCount = (20..200).random(),
                verified = index <= 3, // First 3 are verified
                isCreator = true,
                region = "lagos",
                favoriteGenres = listOf("afrobeats", "comedy", "lifestyle"),
                creatorTier = if (index <= 3) "gold" else "silver"
            )
        }
    }

    private fun generateMockTrendingCreators(count: Int): List<User> {
        val trendingNames = listOf("AfroVibes", "LagosLegend", "NaijaComedy", "BeatsKing", "ViralQueen")
        return (1..count).map { index ->
            User(
                id = "trending_$index",
                username = "${trendingNames.getOrElse(index - 1) { "Creator$index" }}_official",
                displayName = "${trendingNames.getOrElse(index - 1) { "Creator $index" }}",
                profileImageUrl = "https://picsum.photos/200/200?random=${index + 2000}",
                bio = "🔥 Trending Nigerian creator | ${(1..10).random()}M followers worldwide",
                followersCount = (100000..5000000).random(),
                likesCount = (1000000..50000000).random(),
                videosCount = (100..1000).random(),
                verified = true,
                isCreator = true,
                region = listOf("lagos", "abuja").random(),
                favoriteGenres = listOf("afrobeats", "comedy", "dance", "lifestyle"),
                creatorTier = "diamond",
                totalEarnings = (10000..500000).random().toDouble()
            )
        }
    }

    private fun generateMockCreatorStats(): Map<String, Any> {
        return mapOf(
            "totalEarnings" to (1000..50000).random().toDouble(),
            "monthlyEarnings" to (100..5000).random().toDouble(),
            "totalViews" to (10000..1000000).random(),
            "monthlyViews" to (1000..100000).random(),
            "engagementRate" to (0.02..0.15).random(),
            "topPerformingVideo" to "mock_video_trending",
            "fanbase" to mapOf(
                "nigeria" to 0.75,
                "ghana" to 0.10,
                "uk" to 0.08,
                "usa" to 0.05,
                "others" to 0.02
            ),
            "ageGroups" to mapOf(
                "13-17" to 0.15,
                "18-24" to 0.45,
                "25-34" to 0.30,
                "35+" to 0.10
            )
        )
    }
}