package com.nineja.chat.model

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
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val likesCount: Int = 0,
    val videosCount: Int = 0,
    val isVerified: Boolean = false,
    val isPrivate: Boolean = false,
    val website: String = "",
    val location: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val lastActive: Long = System.currentTimeMillis(),
    val isFollowing: Boolean = false,
    val isFollowedBy: Boolean = false,
    val isFriend: Boolean = false,
    val isBlocked: Boolean = false
) : Parcelable {
    
    fun getDisplayUsername(): String {
        return if (username.startsWith("@")) username else "@$username"
    }
    
    fun getFormattedFollowersCount(): String {
        return when {
            followersCount >= 1_000_000 -> String.format("%.1fM", followersCount / 1_000_000.0)
            followersCount >= 1_000 -> String.format("%.1fK", followersCount / 1_000.0)
            else -> followersCount.toString()
        }
    }
    
    fun getFormattedFollowingCount(): String {
        return when {
            followingCount >= 1_000_000 -> String.format("%.1fM", followingCount / 1_000_000.0)
            followingCount >= 1_000 -> String.format("%.1fK", followingCount / 1_000.0)
            else -> followingCount.toString()
        }
    }
    
    fun getFormattedLikesCount(): String {
        return when {
            likesCount >= 1_000_000 -> String.format("%.1fM", likesCount / 1_000_000.0)
            likesCount >= 1_000 -> String.format("%.1fK", likesCount / 1_000.0)
            else -> likesCount.toString()
        }
    }
}