package com.naijachat.naija_chat.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class User(
    val id: String = "",
    val username: String = "",
    val displayName: String = "",
    val email: String = "",
    val profileImageUrl: String = "",
    val bio: String = "",
    val website: String = "",
    val location: String = "",
    val verified: Boolean = false,
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val likesCount: Int = 0,
    val videosCount: Int = 0,
    val isPrivate: Boolean = false,
    val isFollowing: Boolean = false,
    val isFollowedBy: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val lastActiveAt: Long = System.currentTimeMillis(),
    
    // Nigerian cultural preferences
    val preferredLanguages: List<String> = listOf("english"),
    val favoriteGenres: List<String> = listOf("afrobeats"),
    val region: String = "lagos",
    
    // Creator economy
    val isCreator: Boolean = false,
    val creatorTier: String = "bronze", // bronze, silver, gold, diamond
    val totalEarnings: Double = 0.0,
    val withdrawableBalance: Double = 0.0,
    
    // Preferences
    val allowComments: Boolean = true,
    val allowDuets: Boolean = true,
    val allowStitch: Boolean = true,
    val privateMessages: Boolean = true,
    val pushNotifications: Boolean = true
) : Parcelable {

    fun getFormattedFollowersCount(): String {
        return formatCount(followersCount)
    }

    fun getFormattedFollowingCount(): String {
        return formatCount(followingCount)
    }

    fun getFormattedLikesCount(): String {
        return formatCount(likesCount)
    }

    fun getFormattedVideosCount(): String {
        return formatCount(videosCount)
    }

    fun getFormattedEarnings(): String {
        return "₦${String.format("%.2f", totalEarnings)}"
    }

    fun getCreatorTierDisplayName(): String {
        return when (creatorTier.lowercase()) {
            "bronze" -> "Bronze Creator"
            "silver" -> "Silver Creator"
            "gold" -> "Gold Creator"
            "diamond" -> "Diamond Creator"
            else -> "Creator"
        }
    }

    fun canWithdraw(): Boolean {
        return withdrawableBalance >= 1000.0 // Minimum ₦1,000
    }

    private fun formatCount(count: Int): String {
        return when {
            count >= 1_000_000_000 -> "${(count / 1_000_000_000.0).format(1)}B"
            count >= 1_000_000 -> "${(count / 1_000_000.0).format(1)}M"
            count >= 1_000 -> "${(count / 1_000.0).format(1)}K"
            else -> count.toString()
        }
    }

    private fun Double.format(decimals: Int): String {
        return "%.${decimals}f".format(this).trimEnd('0').trimEnd('.')
    }

    companion object {
        fun createDefault(id: String = "", username: String = "", email: String = ""): User {
            return User(
                id = id,
                username = username,
                email = email,
                displayName = username,
                preferredLanguages = listOf("english", "pidgin"),
                favoriteGenres = listOf("afrobeats", "comedy"),
                region = "lagos"
            )
        }

        fun createFromFirebaseUser(firebaseUser: com.google.firebase.auth.FirebaseUser): User {
            return User(
                id = firebaseUser.uid,
                username = generateUsername(firebaseUser.displayName ?: firebaseUser.email ?: ""),
                displayName = firebaseUser.displayName ?: "",
                email = firebaseUser.email ?: "",
                profileImageUrl = firebaseUser.photoUrl?.toString() ?: "",
                verified = firebaseUser.isEmailVerified,
                preferredLanguages = listOf("english", "pidgin"),
                favoriteGenres = listOf("afrobeats", "comedy"),
                region = "lagos"
            )
        }

        private fun generateUsername(name: String): String {
            val cleanName = name.replace("@.*".toRegex(), "")
                .replace("[^a-zA-Z0-9]".toRegex(), "")
                .lowercase()
            
            return if (cleanName.length >= 3) {
                cleanName
            } else {
                "user${System.currentTimeMillis().toString().takeLast(6)}"
            }
        }
    }
}