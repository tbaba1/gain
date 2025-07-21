package com.nineja.chat.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nineja.chat.model.Video
import com.nineja.chat.model.User
import com.nineja.chat.repository.VideoRepository
import com.nineja.chat.repository.UserRepository
import com.nineja.chat.utils.AIRecommendationEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

class HomeViewModel @Inject constructor(
    private val videoRepository: VideoRepository,
    private val userRepository: UserRepository,
    private val aiEngine: AIRecommendationEngine
) : ViewModel() {
    
    private val _videos = MutableStateFlow<List<Video>>(emptyList())
    val videos: StateFlow<List<Video>> = _videos.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val _liveStreams = MutableStateFlow<List<Video>>(emptyList())
    val liveStreams: StateFlow<List<Video>> = _liveStreams.asStateFlow()
    
    private val _trendingContent = MutableStateFlow<List<Video>>(emptyList())
    val trendingContent: StateFlow<List<Video>> = _trendingContent.asStateFlow()
    
    private val _forYouContent = MutableStateFlow<List<Video>>(emptyList())
    val forYouContent: StateFlow<List<Video>> = _forYouContent.asStateFlow()
    
    private var currentPage = 0
    private var isLoadingMore = false
    
    init {
        loadInitialContent()
    }
    
    fun loadVideos() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Load AI-powered recommendations
                val personalizedVideos = aiEngine.getPersonalizedRecommendations(
                    userId = userRepository.getCurrentUserId(),
                    page = 0,
                    limit = 20
                )
                
                _videos.value = personalizedVideos
                currentPage = 1
                
                // Load additional content types
                loadLiveStreams()
                loadTrendingContent()
                
            } catch (e: Exception) {
                _error.value = "Failed to load videos: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun loadMoreVideos() {
        if (isLoadingMore) return
        
        viewModelScope.launch {
            isLoadingMore = true
            try {
                val moreVideos = aiEngine.getPersonalizedRecommendations(
                    userId = userRepository.getCurrentUserId(),
                    page = currentPage,
                    limit = 20
                )
                
                _videos.value = _videos.value + moreVideos
                currentPage++
                
            } catch (e: Exception) {
                _error.value = "Failed to load more videos: ${e.message}"
            } finally {
                isLoadingMore = false
            }
        }
    }
    
    fun refreshVideos() {
        currentPage = 0
        loadVideos()
    }
    
    fun toggleLike(videoId: String) {
        viewModelScope.launch {
            try {
                val success = videoRepository.toggleLike(videoId)
                if (success) {
                    updateVideoInList(videoId) { video ->
                        video.copy(
                            isLiked = !video.isLiked,
                            likesCount = if (video.isLiked) video.likesCount - 1 else video.likesCount + 1
                        )
                    }
                    
                    // Update AI recommendations based on like
                    aiEngine.recordUserInteraction(
                        userId = userRepository.getCurrentUserId(),
                        videoId = videoId,
                        action = if (_videos.value.find { it.id == videoId }?.isLiked == true) "unlike" else "like"
                    )
                }
            } catch (e: Exception) {
                _error.value = "Failed to toggle like: ${e.message}"
            }
        }
    }
    
    fun toggleFollow(userId: String) {
        viewModelScope.launch {
            try {
                val success = userRepository.toggleFollow(userId)
                if (success) {
                    updateVideosFromUser(userId) { video ->
                        video.copy(isFollowing = !video.isFollowing)
                    }
                    
                    // Update AI recommendations based on follow
                    aiEngine.recordUserInteraction(
                        userId = userRepository.getCurrentUserId(),
                        targetUserId = userId,
                        action = "follow"
                    )
                }
            } catch (e: Exception) {
                _error.value = "Failed to toggle follow: ${e.message}"
            }
        }
    }
    
    fun shareVideo(videoId: String) {
        viewModelScope.launch {
            try {
                videoRepository.incrementShareCount(videoId)
                updateVideoInList(videoId) { video ->
                    video.copy(sharesCount = video.sharesCount + 1)
                }
                
                // Record share for AI
                aiEngine.recordUserInteraction(
                    userId = userRepository.getCurrentUserId(),
                    videoId = videoId,
                    action = "share"
                )
            } catch (e: Exception) {
                _error.value = "Failed to record share: ${e.message}"
            }
        }
    }
    
    fun toggleVideoPlayback(videoId: String) {
        // This will be handled by the video adapter
        viewModelScope.launch {
            // Record view for AI recommendations
            aiEngine.recordUserInteraction(
                userId = userRepository.getCurrentUserId(),
                videoId = videoId,
                action = "view"
            )
        }
    }
    
    private fun loadInitialContent() {
        viewModelScope.launch {
            // Load different content feeds simultaneously
            launch { loadForYouContent() }
            launch { loadTrendingContent() }
            launch { loadLiveStreams() }
        }
    }
    
    private suspend fun loadForYouContent() {
        try {
            val forYouVideos = aiEngine.getPersonalizedRecommendations(
                userId = userRepository.getCurrentUserId(),
                page = 0,
                limit = 50,
                type = "for_you"
            )
            _forYouContent.value = forYouVideos
        } catch (e: Exception) {
            // Handle error silently for background loading
        }
    }
    
    private suspend fun loadTrendingContent() {
        try {
            val trending = videoRepository.getTrendingVideos(
                country = "NG", // Nigeria
                limit = 30
            )
            _trendingContent.value = trending
        } catch (e: Exception) {
            // Handle error silently
        }
    }
    
    private suspend fun loadLiveStreams() {
        try {
            val liveVideos = videoRepository.getLiveStreams(limit = 10)
            _liveStreams.value = liveVideos
        } catch (e: Exception) {
            // Handle error silently
        }
    }
    
    private fun updateVideoInList(videoId: String, update: (Video) -> Video) {
        _videos.value = _videos.value.map { video ->
            if (video.id == videoId) update(video) else video
        }
    }
    
    private fun updateVideosFromUser(userId: String, update: (Video) -> Video) {
        _videos.value = _videos.value.map { video ->
            if (video.userId == userId) update(video) else video
        }
    }
    
    fun switchToTab(tab: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val videos = when (tab) {
                    "for_you" -> _forYouContent.value
                    "following" -> videoRepository.getFollowingVideos(
                        userId = userRepository.getCurrentUserId()
                    )
                    "trending" -> _trendingContent.value
                    "live" -> _liveStreams.value
                    else -> _forYouContent.value
                }
                _videos.value = videos
            } catch (e: Exception) {
                _error.value = "Failed to switch tab: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun reportVideo(videoId: String, reason: String) {
        viewModelScope.launch {
            try {
                videoRepository.reportVideo(videoId, reason)
                // Remove from current feed
                _videos.value = _videos.value.filter { it.id != videoId }
            } catch (e: Exception) {
                _error.value = "Failed to report video: ${e.message}"
            }
        }
    }
    
    fun hideVideo(videoId: String) {
        _videos.value = _videos.value.filter { it.id != videoId }
        
        viewModelScope.launch {
            // Record negative feedback for AI
            aiEngine.recordUserInteraction(
                userId = userRepository.getCurrentUserId(),
                videoId = videoId,
                action = "hide"
            )
        }
    }
}