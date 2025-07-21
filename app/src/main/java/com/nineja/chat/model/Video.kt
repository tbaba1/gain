package com.nineja.chat.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Video(
    val id: String = "",
    val userId: String = "",
    val user: User? = null,
    val videoUrl: String = "",
    val thumbnailUrl: String = "",
    val description: String = "",
    val hashtags: List<String> = emptyList(),
    val mentions: List<String> = emptyList(),
    val soundId: String = "",
    val soundName: String = "",
    val soundUrl: String = "",
    val duration: Long = 0L, // in milliseconds
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    
    // Engagement metrics
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val sharesCount: Int = 0,
    val viewsCount: Int = 0,
    val downloadsCount: Int = 0,
    
    // User interactions
    val isLiked: Boolean = false,
    val isBookmarked: Boolean = false,
    val isFollowing: Boolean = false,
    
    // Video properties
    val width: Int = 0,
    val height: Int = 0,
    val aspectRatio: Float = 9f/16f, // TikTok default
    val fileSize: Long = 0L, // in bytes
    val quality: VideoQuality = VideoQuality.HD,
    
    // Privacy and moderation
    val isPrivate: Boolean = false,
    val isBlocked: Boolean = false,
    val isReported: Boolean = false,
    val allowComments: Boolean = true,
    val allowDownload: Boolean = true,
    val allowDuet: Boolean = true,
    val allowStitch: Boolean = true,
    
    // Location and effects
    val location: String = "",
    val effects: List<String> = emptyList(),
    val filters: List<String> = emptyList()
) : Parcelable {
    
    fun getFormattedLikesCount(): String {
        return formatCount(likesCount)
    }
    
    fun getFormattedCommentsCount(): String {
        return formatCount(commentsCount)
    }
    
    fun getFormattedSharesCount(): String {
        return formatCount(sharesCount)
    }
    
    fun getFormattedViewsCount(): String {
        return formatCount(viewsCount)
    }
    
    fun getFormattedDuration(): String {
        val seconds = duration / 1000
        return if (seconds < 60) {
            "${seconds}s"
        } else {
            val minutes = seconds / 60
            val remainingSeconds = seconds % 60
            "${minutes}m ${remainingSeconds}s"
        }
    }
    
    fun getHashtagString(): String {
        return hashtags.joinToString(" ") { "#$it" }
    }
    
    fun getMentionString(): String {
        return mentions.joinToString(" ") { "@$it" }
    }
    
    private fun formatCount(count: Int): String {
        return when {
            count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
            count >= 1_000 -> String.format("%.1fK", count / 1_000.0)
            else -> count.toString()
        }
    }
}

enum class VideoQuality {
    SD, HD, FULL_HD, ULTRA_HD
}

@Parcelize
data class Comment(
    val id: String = "",
    val videoId: String = "",
    val userId: String = "",
    val user: User? = null,
    val content: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val likesCount: Int = 0,
    val repliesCount: Int = 0,
    val isLiked: Boolean = false,
    val isPinned: Boolean = false,
    val parentCommentId: String? = null, // For replies
    val mentions: List<String> = emptyList()
) : Parcelable {
    
    fun getFormattedLikesCount(): String {
        return when {
            likesCount >= 1_000_000 -> String.format("%.1fM", likesCount / 1_000_000.0)
            likesCount >= 1_000 -> String.format("%.1fK", likesCount / 1_000.0)
            else -> if (likesCount > 0) likesCount.toString() else ""
        }
    }
    
    fun getTimeAgo(): String {
        val now = System.currentTimeMillis()
        val diff = now - createdAt
        
        return when {
            diff < 60_000 -> "now"
            diff < 3_600_000 -> "${diff / 60_000}m"
            diff < 86_400_000 -> "${diff / 3_600_000}h"
            diff < 604_800_000 -> "${diff / 86_400_000}d"
            else -> "${diff / 604_800_000}w"
        }
    }
}