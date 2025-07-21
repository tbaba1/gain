package com.naijachat.naija_chat.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Video(
    val id: String = "",
    val userId: String = "",
    val username: String = "",
    val userProfileImage: String = "",
    val videoUrl: String = "",
    val thumbnailUrl: String = "",
    val description: String = "",
    val hashtags: List<String> = emptyList(),
    val soundName: String = "",
    val soundUrl: String = "",
    val duration: Long = 0L, // in milliseconds
    val viewsCount: Int = 0,
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val sharesCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val isLiked: Boolean = false,
    val isFollowing: Boolean = false,
    val isBookmarked: Boolean = false,
    val allowComments: Boolean = true,
    val allowDuets: Boolean = true,
    val allowStitch: Boolean = true,
    
    // Video quality and technical details
    val quality: VideoQuality = VideoQuality.HD,
    val fileSize: Long = 0L, // in bytes
    val aspectRatio: String = "9:16", // Default TikTok ratio
    
    // Location and culture
    val location: String = "",
    val isNigerianContent: Boolean = false,
    val language: String = "english",
    
    // Engagement metrics
    val engagementRate: Float = 0f,
    val watchTimeAverage: Long = 0L,
    val completionRate: Float = 0f,
    
    // Live streaming
    val isLiveStream: Boolean = false,
    val liveViewersCount: Int = 0,
    val streamStatus: String = "ended" // live, ended, scheduled
) : Parcelable {

    fun getFormattedDuration(): String {
        val seconds = duration / 1000
        return if (seconds < 60) {
            "${seconds}s"
        } else {
            val minutes = seconds / 60
            val remainingSeconds = seconds % 60
            "${minutes}:${String.format("%02d", remainingSeconds)}"
        }
    }

    fun getFormattedViewsCount(): String {
        return formatCount(viewsCount)
    }

    fun getFormattedLikesCount(): String {
        return formatCount(likesCount)
    }

    fun getFormattedCommentsCount(): String {
        return formatCount(commentsCount)
    }

    fun getFormattedSharesCount(): String {
        return formatCount(sharesCount)
    }

    fun getFormattedFileSize(): String {
        val mb = fileSize / (1024 * 1024)
        return "${mb}MB"
    }

    fun getTimeAgo(): String {
        val now = System.currentTimeMillis()
        val diff = now - createdAt
        
        return when {
            diff < 60_000 -> "Just now"
            diff < 3_600_000 -> "${diff / 60_000}m ago"
            diff < 86_400_000 -> "${diff / 3_600_000}h ago"
            diff < 604_800_000 -> "${diff / 86_400_000}d ago"
            diff < 2_592_000_000 -> "${diff / 604_800_000}w ago"
            else -> "${diff / 2_592_000_000}mo ago"
        }
    }

    fun isRecentlyCreated(): Boolean {
        val now = System.currentTimeMillis()
        val dayInMillis = 24 * 60 * 60 * 1000
        return (now - createdAt) < dayInMillis
    }

    fun isTrending(): Boolean {
        val recentViews = if (isRecentlyCreated()) viewsCount else 0
        val engagementThreshold = 0.05f // 5% engagement rate
        return recentViews > 1000 && engagementRate > engagementThreshold
    }

    fun hasNigerianHashtags(): Boolean {
        val nigerianTags = listOf("naija", "nigeria", "lagos", "abuja", "afrobeats", "nollywood")
        return hashtags.any { tag ->
            nigerianTags.any { nigerianTag ->
                tag.lowercase().contains(nigerianTag)
            }
        }
    }

    fun getTotalInteractions(): Int {
        return likesCount + commentsCount + sharesCount
    }

    fun getEngagementScore(): Float {
        return if (viewsCount > 0) {
            getTotalInteractions().toFloat() / viewsCount.toFloat()
        } else 0f
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
        fun createDefault(): Video {
            return Video(
                id = "default_${System.currentTimeMillis()}",
                isNigerianContent = true,
                language = "english"
            )
        }

        fun createFromUpload(
            userId: String,
            videoUrl: String,
            thumbnailUrl: String,
            description: String,
            hashtags: List<String>,
            duration: Long
        ): Video {
            return Video(
                id = "video_${System.currentTimeMillis()}",
                userId = userId,
                videoUrl = videoUrl,
                thumbnailUrl = thumbnailUrl,
                description = description,
                hashtags = hashtags,
                duration = duration,
                isNigerianContent = hashtags.any { it.lowercase().contains("naija") || it.lowercase().contains("nigeria") },
                createdAt = System.currentTimeMillis()
            )
        }
    }
}

@Parcelize
data class Comment(
    val id: String = "",
    val videoId: String = "",
    val userId: String = "",
    val username: String = "",
    val userProfileImage: String = "",
    val text: String = "",
    val likesCount: Int = 0,
    val repliesCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val isLiked: Boolean = false,
    val isPinned: Boolean = false,
    val isCreatorComment: Boolean = false,
    val parentCommentId: String? = null // For replies
) : Parcelable {

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

    fun getFormattedLikesCount(): String {
        return when {
            likesCount >= 1_000_000 -> "${(likesCount / 1_000_000.0).format(1)}M"
            likesCount >= 1_000 -> "${(likesCount / 1_000.0).format(1)}K"
            else -> if (likesCount > 0) likesCount.toString() else ""
        }
    }

    private fun Double.format(decimals: Int): String {
        return "%.${decimals}f".format(this).trimEnd('0').trimEnd('.')
    }

    fun isReply(): Boolean = parentCommentId != null
}

enum class VideoQuality {
    SD, HD, FULL_HD, UHD_4K;

    fun getDisplayName(): String {
        return when (this) {
            SD -> "480p"
            HD -> "720p"
            FULL_HD -> "1080p"
            UHD_4K -> "4K"
        }
    }

    fun getMaxBitrate(): Int {
        return when (this) {
            SD -> 1_000_000 // 1 Mbps
            HD -> 2_500_000 // 2.5 Mbps
            FULL_HD -> 5_000_000 // 5 Mbps
            UHD_4K -> 15_000_000 // 15 Mbps
        }
    }
}