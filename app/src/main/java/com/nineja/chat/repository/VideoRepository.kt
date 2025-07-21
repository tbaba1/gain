package com.naijachat.naija_chat.repository

import com.naijachat.naija_chat.model.Video
import com.naijachat.naija_chat.firebase.FirestoreManager
import com.naijachat.naija_chat.firebase.FirebaseStorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoRepository @Inject constructor(
    private val firestoreManager: FirestoreManager,
    private val storageManager: FirebaseStorageManager
) {

    /**
     * Get videos for the home feed
     */
    suspend fun getVideos(page: Int = 0, limit: Int = 20): List<Video> = withContext(Dispatchers.IO) {
        try {
            firestoreManager.getVideos(limit, page * limit)
        } catch (e: Exception) {
            // Return mock data for development
            generateMockVideos(limit)
        }
    }

    /**
     * Get trending videos
     */
    suspend fun getTrendingVideos(country: String = "NG", limit: Int = 20): List<Video> = withContext(Dispatchers.IO) {
        try {
            firestoreManager.getTrendingVideos(country, limit)
        } catch (e: Exception) {
            generateMockTrendingVideos(limit)
        }
    }

    /**
     * Get videos from users the current user follows
     */
    suspend fun getFollowingVideos(userId: String, page: Int = 0, limit: Int = 20): List<Video> = withContext(Dispatchers.IO) {
        try {
            firestoreManager.getFollowingVideos(userId, limit, page * limit)
        } catch (e: Exception) {
            generateMockVideos(limit)
        }
    }

    /**
     * Get live stream videos
     */
    suspend fun getLiveStreams(limit: Int = 10): List<Video> = withContext(Dispatchers.IO) {
        try {
            firestoreManager.getLiveStreams(limit)
        } catch (e: Exception) {
            generateMockLiveStreams(limit)
        }
    }

    /**
     * Get latest/fresh videos
     */
    suspend fun getLatestVideos(page: Int = 0, limit: Int = 20): List<Video> = withContext(Dispatchers.IO) {
        try {
            firestoreManager.getLatestVideos(limit, page * limit)
        } catch (e: Exception) {
            generateMockVideos(limit)
        }
    }

    /**
     * Get a single video by ID
     */
    suspend fun getVideo(videoId: String): Video? = withContext(Dispatchers.IO) {
        try {
            firestoreManager.getVideo(videoId)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Toggle like on a video
     */
    suspend fun toggleLike(videoId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            firestoreManager.toggleVideoLike(videoId)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Increment share count
     */
    suspend fun incrementShareCount(videoId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            firestoreManager.incrementShareCount(videoId)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Report a video
     */
    suspend fun reportVideo(videoId: String, reason: String): Boolean = withContext(Dispatchers.IO) {
        try {
            firestoreManager.reportVideo(videoId, reason)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Upload a new video
     */
    suspend fun uploadVideo(
        videoPath: String,
        thumbnailPath: String,
        description: String,
        hashtags: List<String>
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            // Upload video file
            val videoUrl = storageManager.uploadVideo(videoPath)
            
            // Upload thumbnail
            val thumbnailUrl = storageManager.uploadThumbnail(thumbnailPath)
            
            // Create video object
            val video = Video.createFromUpload(
                userId = "current_user_id", // This should come from auth
                videoUrl = videoUrl,
                thumbnailUrl = thumbnailUrl,
                description = description,
                hashtags = hashtags,
                duration = 30000L // This should be calculated from video
            )
            
            // Save to Firestore
            firestoreManager.saveVideo(video)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Search videos by query
     */
    suspend fun searchVideos(query: String, limit: Int = 20): List<Video> = withContext(Dispatchers.IO) {
        try {
            firestoreManager.searchVideos(query, limit)
        } catch (e: Exception) {
            generateMockSearchResults(query, limit)
        }
    }

    /**
     * Get videos by hashtag
     */
    suspend fun getVideosByHashtag(hashtag: String, limit: Int = 20): List<Video> = withContext(Dispatchers.IO) {
        try {
            firestoreManager.getVideosByHashtag(hashtag, limit)
        } catch (e: Exception) {
            generateMockHashtagVideos(hashtag, limit)
        }
    }

    /**
     * Get user's videos
     */
    suspend fun getUserVideos(userId: String, limit: Int = 20): List<Video> = withContext(Dispatchers.IO) {
        try {
            firestoreManager.getUserVideos(userId, limit)
        } catch (e: Exception) {
            generateMockUserVideos(userId, limit)
        }
    }

    // Mock data generators for development/testing
    private fun generateMockVideos(count: Int): List<Video> {
        return (1..count).map { index ->
            Video(
                id = "mock_video_$index",
                userId = "user_$index",
                username = "@naija_creator_$index",
                userProfileImage = "https://picsum.photos/100/100?random=$index",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                thumbnailUrl = "https://picsum.photos/400/600?random=$index",
                description = "Amazing Nigerian content #$index! Check out this incredible video from Lagos 🇳🇬 #Naija #Afrobeats #Lagos",
                hashtags = listOf("Naija", "Afrobeats", "Lagos", "Comedy", "Dance"),
                soundName = "Afrobeats Hit $index",
                duration = (15000..60000).random().toLong(),
                viewsCount = (1000..1000000).random(),
                likesCount = (50..50000).random(),
                commentsCount = (10..5000).random(),
                sharesCount = (5..1000).random(),
                isNigerianContent = true,
                language = "english",
                createdAt = System.currentTimeMillis() - (index * 3600000L) // Stagger creation times
            )
        }
    }

    private fun generateMockTrendingVideos(count: Int): List<Video> {
        val trendingHashtags = listOf("ViralNaija", "TrendingNow", "AfrobeatsCraze", "NollywoodVibes", "LagosTrends")
        return generateMockVideos(count).map { video ->
            video.copy(
                hashtags = video.hashtags + trendingHashtags.random(),
                viewsCount = video.viewsCount * 2, // Trending videos have more views
                engagementRate = 0.08f // Higher engagement for trending
            )
        }
    }

    private fun generateMockLiveStreams(count: Int): List<Video> {
        return (1..count).map { index ->
            Video(
                id = "live_stream_$index",
                userId = "streamer_$index",
                username = "@live_naija_$index",
                userProfileImage = "https://picsum.photos/100/100?random=${index + 100}",
                videoUrl = "https://live-stream-url-$index.com",
                thumbnailUrl = "https://picsum.photos/400/600?random=${index + 100}",
                description = "🔴 LIVE: Amazing content from Nigeria! Join the conversation #LiveNaija",
                hashtags = listOf("Live", "Naija", "Interactive", "RealTime"),
                isLiveStream = true,
                liveViewersCount = (50..5000).random(),
                streamStatus = "live",
                isNigerianContent = true,
                createdAt = System.currentTimeMillis() - (index * 600000L) // Recent streams
            )
        }
    }

    private fun generateMockSearchResults(query: String, count: Int): List<Video> {
        return generateMockVideos(count).map { video ->
            video.copy(
                description = "Search result for '$query': ${video.description}",
                hashtags = video.hashtags + listOf(query.replace(" ", ""))
            )
        }
    }

    private fun generateMockHashtagVideos(hashtag: String, count: Int): List<Video> {
        return generateMockVideos(count).map { video ->
            video.copy(
                hashtags = listOf(hashtag) + video.hashtags.take(3),
                description = "Check out this amazing #$hashtag content! ${video.description}"
            )
        }
    }

    private fun generateMockUserVideos(userId: String, count: Int): List<Video> {
        return generateMockVideos(count).map { video ->
            video.copy(
                userId = userId,
                username = "@user_profile"
            )
        }
    }
}