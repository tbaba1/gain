package com.nineja.chat.utils

import com.nineja.chat.model.Video
import com.nineja.chat.model.User
import com.nineja.chat.network.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

@Singleton
class AIRecommendationEngine @Inject constructor(
    private val apiService: ApiService,
    private val preferencesManager: PreferencesManager
) {
    
    data class UserInteraction(
        val userId: String,
        val videoId: String,
        val targetUserId: String? = null,
        val action: String, // like, view, share, comment, follow, hide
        val timestamp: Long = System.currentTimeMillis(),
        val watchTime: Long = 0L, // in milliseconds
        val engagement: Float = 0f // 0-1 score
    )
    
    data class ContentProfile(
        val categories: Map<String, Float> = emptyMap(), // category -> preference score
        val hashtags: Map<String, Float> = emptyMap(),
        val creators: Map<String, Float> = emptyMap(),
        val musicGenres: Map<String, Float> = emptyMap(),
        val languages: Map<String, Float> = emptyMap(),
        val videoLength: FloatRange = 0f..60f,
        val timeOfDay: Map<Int, Float> = emptyMap() // hour -> engagement
    )
    
    private val userProfiles = mutableMapOf<String, ContentProfile>()
    private val interactionHistory = mutableListOf<UserInteraction>()
    
    suspend fun getPersonalizedRecommendations(
        userId: String,
        page: Int = 0,
        limit: Int = 20,
        type: String = "mixed"
    ): List<Video> = withContext(Dispatchers.Default) {
        
        val userProfile = getUserProfile(userId)
        val baseVideos = getBaseVideos(type, page, limit)
        
        // Score and rank videos based on user preferences
        val scoredVideos = baseVideos.map { video ->
            val score = calculateRecommendationScore(video, userProfile, userId)
            Pair(video, score)
        }.sortedByDescending { it.second }
        
        // Apply diversity and freshness
        val diversifiedVideos = applyDiversification(scoredVideos.map { it.first }, userProfile)
        
        // Add trending Nigerian content boost
        val boostedVideos = applyNigerianContentBoost(diversifiedVideos)
        
        return@withContext boostedVideos.take(limit)
    }
    
    suspend fun recordUserInteraction(
        userId: String,
        videoId: String? = null,
        targetUserId: String? = null,
        action: String,
        watchTime: Long = 0L
    ) {
        val interaction = UserInteraction(
            userId = userId,
            videoId = videoId ?: "",
            targetUserId = targetUserId,
            action = action,
            watchTime = watchTime,
            engagement = calculateEngagementScore(action, watchTime)
        )
        
        interactionHistory.add(interaction)
        updateUserProfile(userId, interaction)
        
        // Send to backend for persistent storage
        try {
            apiService.recordInteraction(interaction)
        } catch (e: Exception) {
            // Handle offline mode
            preferencesManager.saveInteractionLocally(interaction)
        }
    }
    
    private suspend fun getUserProfile(userId: String): ContentProfile {
        return userProfiles[userId] ?: run {
            val profile = buildUserProfile(userId)
            userProfiles[userId] = profile
            profile
        }
    }
    
    private suspend fun buildUserProfile(userId: String): ContentProfile {
        val userInteractions = interactionHistory.filter { it.userId == userId }
        
        if (userInteractions.isEmpty()) {
            return getDefaultNigerianProfile()
        }
        
        val categories = mutableMapOf<String, Float>()
        val hashtags = mutableMapOf<String, Float>()
        val creators = mutableMapOf<String, Float>()
        val musicGenres = mutableMapOf<String, Float>()
        val languages = mutableMapOf<String, Float>()
        val timeOfDay = mutableMapOf<Int, Float>()
        
        var totalEngagement = 0f
        var videoLengthSum = 0f
        var videoCount = 0
        
        for (interaction in userInteractions) {
            val weight = getActionWeight(interaction.action)
            val timeDecay = getTimeDecay(interaction.timestamp)
            val finalWeight = weight * timeDecay * interaction.engagement
            
            totalEngagement += finalWeight
            
            // Get video details for this interaction
            try {
                val video = apiService.getVideo(interaction.videoId)
                video?.let {
                    // Update category preferences
                    updateMapWithWeight(categories, extractCategory(it), finalWeight)
                    
                    // Update hashtag preferences
                    it.hashtags.forEach { hashtag ->
                        updateMapWithWeight(hashtags, hashtag, finalWeight * 0.5f)
                    }
                    
                    // Update creator preferences
                    updateMapWithWeight(creators, it.userId, finalWeight)
                    
                    // Update music preferences
                    if (it.soundName.isNotEmpty()) {
                        val genre = extractMusicGenre(it.soundName)
                        updateMapWithWeight(musicGenres, genre, finalWeight)
                    }
                    
                    // Update time preferences
                    val hour = extractHourFromTimestamp(interaction.timestamp)
                    updateMapWithWeight(timeOfDay, hour.toString(), finalWeight)
                    
                    // Track video length preferences
                    videoLengthSum += it.duration / 1000f
                    videoCount++
                }
            } catch (e: Exception) {
                // Handle API error
            }
        }
        
        // Normalize scores
        val totalScore = categories.values.sum()
        if (totalScore > 0) {
            categories.replaceAll { _, score -> score / totalScore }
            hashtags.replaceAll { _, score -> score / totalScore }
            creators.replaceAll { _, score -> score / totalScore }
            musicGenres.replaceAll { _, score -> score / totalScore }
        }
        
        val avgVideoLength = if (videoCount > 0) videoLengthSum / videoCount else 30f
        
        return ContentProfile(
            categories = categories,
            hashtags = hashtags,
            creators = creators,
            musicGenres = musicGenres,
            videoLength = max(5f, avgVideoLength - 10f)..min(60f, avgVideoLength + 10f),
            timeOfDay = timeOfDay
        )
    }
    
    private fun calculateRecommendationScore(
        video: Video,
        userProfile: ContentProfile,
        userId: String
    ): Float {
        var score = 0f
        
        // Category match
        val category = extractCategory(video)
        score += (userProfile.categories[category] ?: 0f) * 3f
        
        // Hashtag match
        video.hashtags.forEach { hashtag ->
            score += (userProfile.hashtags[hashtag] ?: 0f) * 1f
        }
        
        // Creator preference
        score += (userProfile.creators[video.userId] ?: 0f) * 2f
        
        // Music genre match
        if (video.soundName.isNotEmpty()) {
            val genre = extractMusicGenre(video.soundName)
            score += (userProfile.musicGenres[genre] ?: 0f) * 1.5f
        }
        
        // Video length preference
        val duration = video.duration / 1000f
        if (duration in userProfile.videoLength) {
            score += 1f
        }
        
        // Time of day boost
        val currentHour = getCurrentHour()
        score += (userProfile.timeOfDay[currentHour.toString()] ?: 0f) * 0.5f
        
        // Nigerian content boost
        score += getNigerianContentBoost(video)
        
        // Freshness boost (newer content gets slight boost)
        val ageInHours = (System.currentTimeMillis() - video.createdAt) / (1000 * 60 * 60)
        if (ageInHours < 24) {
            score += 0.5f * (1f - ageInHours / 24f)
        }
        
        // Engagement quality boost
        val engagementRate = calculateVideoEngagementRate(video)
        score += engagementRate * 2f
        
        // Diversity penalty (avoid too similar content)
        score -= getDiversityPenalty(video, userId)
        
        return max(0f, score)
    }
    
    private fun getDefaultNigerianProfile(): ContentProfile {
        return ContentProfile(
            categories = mapOf(
                "afrobeats" to 0.3f,
                "comedy" to 0.25f,
                "dance" to 0.2f,
                "lifestyle" to 0.15f,
                "education" to 0.1f
            ),
            hashtags = mapOf(
                "naija" to 0.3f,
                "nigeria" to 0.25f,
                "afrobeats" to 0.2f,
                "lagos" to 0.15f,
                "comedy" to 0.1f
            ),
            musicGenres = mapOf(
                "afrobeats" to 0.4f,
                "afropop" to 0.3f,
                "highlife" to 0.2f,
                "gospel" to 0.1f
            ),
            languages = mapOf(
                "english" to 0.4f,
                "pidgin" to 0.3f,
                "yoruba" to 0.2f,
                "igbo" to 0.1f
            )
        )
    }
    
    private fun extractCategory(video: Video): String {
        // Analyze video content to determine category
        val description = video.description.lowercase()
        val hashtags = video.hashtags.map { it.lowercase() }
        
        return when {
            hashtags.any { it.contains("music") || it.contains("afrobeats") } -> "music"
            hashtags.any { it.contains("comedy") || it.contains("funny") } -> "comedy"
            hashtags.any { it.contains("dance") || it.contains("challenge") } -> "dance"
            hashtags.any { it.contains("food") || it.contains("cooking") } -> "food"
            hashtags.any { it.contains("fashion") || it.contains("style") } -> "fashion"
            hashtags.any { it.contains("education") || it.contains("learn") } -> "education"
            description.contains("music") || description.contains("song") -> "music"
            description.contains("funny") || description.contains("joke") -> "comedy"
            else -> "lifestyle"
        }
    }
    
    private fun extractMusicGenre(soundName: String): String {
        val sound = soundName.lowercase()
        return when {
            sound.contains("afrobeats") || sound.contains("afrobeat") -> "afrobeats"
            sound.contains("afropop") -> "afropop"
            sound.contains("highlife") -> "highlife"
            sound.contains("gospel") -> "gospel"
            sound.contains("hip hop") || sound.contains("rap") -> "hip_hop"
            sound.contains("r&b") || sound.contains("rnb") -> "rnb"
            else -> "afrobeats" // default for Nigerian content
        }
    }
    
    private fun getNigerianContentBoost(video: Video): Float {
        var boost = 0f
        
        val nigerianHashtags = listOf("naija", "nigeria", "lagos", "abuja", "afrobeats")
        val matchingTags = video.hashtags.count { hashtag ->
            nigerianHashtags.any { it in hashtag.lowercase() }
        }
        
        boost += matchingTags * 0.2f
        
        // Check description for Nigerian content
        val description = video.description.lowercase()
        if (description.contains("naija") || description.contains("nigeria")) {
            boost += 0.3f
        }
        
        return boost
    }
    
    private fun calculateEngagementScore(action: String, watchTime: Long): Float {
        return when (action) {
            "view" -> min(1f, watchTime / 30000f) // 30 seconds for full engagement
            "like" -> 0.8f
            "comment" -> 0.9f
            "share" -> 1.0f
            "follow" -> 1.0f
            "save" -> 0.7f
            "hide" -> -0.5f
            "report" -> -1.0f
            else -> 0.1f
        }
    }
    
    private fun getActionWeight(action: String): Float {
        return when (action) {
            "view" -> 1f
            "like" -> 2f
            "comment" -> 3f
            "share" -> 4f
            "follow" -> 3f
            "save" -> 2f
            "hide" -> -2f
            "report" -> -5f
            else -> 0.5f
        }
    }
    
    private fun getTimeDecay(timestamp: Long): Float {
        val ageInDays = (System.currentTimeMillis() - timestamp) / (1000 * 60 * 60 * 24)
        return exp(-ageInDays / 30f) // Exponential decay over 30 days
    }
    
    private fun updateMapWithWeight(map: MutableMap<String, Float>, key: String, weight: Float) {
        map[key] = (map[key] ?: 0f) + weight
    }
    
    private fun calculateVideoEngagementRate(video: Video): Float {
        val totalInteractions = video.likesCount + video.commentsCount + video.sharesCount
        return if (video.viewsCount > 0) {
            totalInteractions.toFloat() / video.viewsCount.toFloat()
        } else 0f
    }
    
    private fun getCurrentHour(): Int {
        return (System.currentTimeMillis() / (1000 * 60 * 60) % 24).toInt()
    }
    
    private fun extractHourFromTimestamp(timestamp: Long): Int {
        return (timestamp / (1000 * 60 * 60) % 24).toInt()
    }
    
    private suspend fun getBaseVideos(type: String, page: Int, limit: Int): List<Video> {
        return try {
            when (type) {
                "trending" -> apiService.getTrendingVideos(page, limit)
                "fresh" -> apiService.getLatestVideos(page, limit)
                "following" -> apiService.getFollowingVideos(page, limit)
                else -> apiService.getVideos(page, limit * 2) // Get more for better filtering
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun applyDiversification(videos: List<Video>, userProfile: ContentProfile): List<Video> {
        val diversified = mutableListOf<Video>()
        val usedCategories = mutableSetOf<String>()
        val usedCreators = mutableSetOf<String>()
        
        for (video in videos) {
            val category = extractCategory(video)
            val creator = video.userId
            
            // Ensure diversity in categories and creators
            if (diversified.size < 5 || 
                (!usedCategories.contains(category) || usedCategories.size < 3) ||
                (!usedCreators.contains(creator) || usedCreators.size < 8)) {
                
                diversified.add(video)
                usedCategories.add(category)
                usedCreators.add(creator)
            }
            
            if (diversified.size >= videos.size) break
        }
        
        return diversified
    }
    
    private fun applyNigerianContentBoost(videos: List<Video>): List<Video> {
        // Ensure at least 60% Nigerian content
        val nigerianVideos = videos.filter { getNigerianContentBoost(it) > 0 }
        val internationalVideos = videos.filter { getNigerianContentBoost(it) == 0f }
        
        val targetNigerianRatio = 0.6f
        val nigerianCount = (videos.size * targetNigerianRatio).toInt()
        
        return (nigerianVideos.take(nigerianCount) + 
                internationalVideos.take(videos.size - nigerianCount)).shuffled()
    }
    
    private fun getDiversityPenalty(video: Video, userId: String): Float {
        // Penalize if user has seen too much similar content recently
        val recentInteractions = interactionHistory
            .filter { it.userId == userId && it.timestamp > System.currentTimeMillis() - 24 * 60 * 60 * 1000 }
            .takeLast(20)
        
        val category = extractCategory(video)
        val categoryCount = recentInteractions.count { interaction ->
            // This would need actual video data, simplified for now
            extractCategory(video) == category
        }
        
        return if (categoryCount > 5) 0.5f else 0f
    }
    
    private fun updateUserProfile(userId: String, interaction: UserInteraction) {
        // Update user profile based on new interaction
        // This is a simplified version - in production, this would be more sophisticated
        val currentProfile = userProfiles[userId] ?: getDefaultNigerianProfile()
        userProfiles[userId] = currentProfile
    }
}