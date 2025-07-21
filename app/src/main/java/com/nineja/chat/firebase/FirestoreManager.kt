package com.nineja.chat.firebase

import android.util.Log
import com.google.firebase.firestore.*
import com.google.firebase.firestore.ktx.snapshots
import com.nineja.chat.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreManager @Inject constructor() {
    
    private val firestore = FirebaseFirestore.getInstance()
    
    // Collections
    private val usersCollection = firestore.collection("users")
    private val videosCollection = firestore.collection("videos")
    private val commentsCollection = firestore.collection("comments")
    private val likesCollection = firestore.collection("likes")
    private val followsCollection = firestore.collection("follows")
    private val analyticsCollection = firestore.collection("analytics")
    private val creatorFundCollection = firestore.collection("creator_fund")
    private val virtualGiftsCollection = firestore.collection("virtual_gifts")
    private val giftTransactionsCollection = firestore.collection("gift_transactions")
    private val liveStreamsCollection = firestore.collection("live_streams")
    private val chatMessagesCollection = firestore.collection("chat_messages")
    private val notificationsCollection = firestore.collection("notifications")
    private val reportsCollection = firestore.collection("reports")
    private val brandPartnershipsCollection = firestore.collection("brand_partnerships")
    
    // User Management
    suspend fun createUser(user: User): Result<Unit> {
        return try {
            usersCollection.document(user.id).set(user).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create user", e)
            Result.failure(e)
        }
    }
    
    suspend fun getUser(userId: String): Result<User> {
        return try {
            val document = usersCollection.document(userId).get().await()
            val user = document.toObject(User::class.java)
                ?: throw Exception("User not found")
            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get user", e)
            Result.failure(e)
        }
    }
    
    suspend fun updateUser(userId: String, updates: Map<String, Any>): Result<Unit> {
        return try {
            val updateMap = updates + mapOf("lastActiveAt" to System.currentTimeMillis())
            usersCollection.document(userId).update(updateMap).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update user", e)
            Result.failure(e)
        }
    }
    
    suspend fun searchUsers(query: String, limit: Int = 20): Result<List<User>> {
        return try {
            val documents = usersCollection
                .whereGreaterThanOrEqualTo("username", query)
                .whereLessThanOrEqualTo("username", query + '\uf8ff')
                .limit(limit.toLong())
                .get()
                .await()
            
            val users = documents.mapNotNull { it.toObject(User::class.java) }
            Result.success(users)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to search users", e)
            Result.failure(e)
        }
    }
    
    // Video Management
    suspend fun createVideo(video: Video): Result<String> {
        return try {
            val docRef = videosCollection.add(video).await()
            val videoId = docRef.id
            
            // Update video with its ID
            videosCollection.document(videoId).update("id", videoId).await()
            
            // Update user's video count
            incrementUserVideoCount(video.userId)
            
            Result.success(videoId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create video", e)
            Result.failure(e)
        }
    }
    
    suspend fun getVideo(videoId: String): Result<Video> {
        return try {
            val document = videosCollection.document(videoId).get().await()
            val video = document.toObject(Video::class.java)
                ?: throw Exception("Video not found")
            Result.success(video)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get video", e)
            Result.failure(e)
        }
    }
    
    suspend fun getVideos(page: Int, limit: Int): Result<List<Video>> {
        return try {
            val query = videosCollection
                .whereEqualTo("isPrivate", false)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit.toLong())
            
            val documents = if (page > 0) {
                // Implement pagination using document snapshots
                val lastDocument = getLastDocument(page, limit)
                if (lastDocument != null) {
                    query.startAfter(lastDocument).get().await()
                } else {
                    query.get().await()
                }
            } else {
                query.get().await()
            }
            
            val videos = documents.mapNotNull { it.toObject(Video::class.java) }
            Result.success(videos)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get videos", e)
            Result.failure(e)
        }
    }
    
    suspend fun getTrendingVideos(country: String = "NG", limit: Int = 30): Result<List<Video>> {
        return try {
            val documents = videosCollection
                .whereEqualTo("isPrivate", false)
                .orderBy("trendingScore", Query.Direction.DESCENDING)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()
            
            val videos = documents.mapNotNull { it.toObject(Video::class.java) }
            Result.success(videos)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get trending videos", e)
            Result.failure(e)
        }
    }
    
    suspend fun getUserVideos(userId: String, limit: Int = 20): Result<List<Video>> {
        return try {
            val documents = videosCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("isPrivate", false)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()
            
            val videos = documents.mapNotNull { it.toObject(Video::class.java) }
            Result.success(videos)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get user videos", e)
            Result.failure(e)
        }
    }
    
    suspend fun getFollowingVideos(userId: String, limit: Int = 20): Result<List<Video>> {
        return try {
            // First, get the list of users that this user follows
            val followingDocs = followsCollection
                .whereEqualTo("followerId", userId)
                .get()
                .await()
            
            val followingIds = followingDocs.map { it.getString("followingId")!! }
            
            if (followingIds.isEmpty()) {
                return Result.success(emptyList())
            }
            
            // Get videos from followed users (Firestore 'in' query limit is 10)
            val chunks = followingIds.chunked(10)
            val allVideos = mutableListOf<Video>()
            
            for (chunk in chunks) {
                val documents = videosCollection
                    .whereIn("userId", chunk)
                    .whereEqualTo("isPrivate", false)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .limit(limit.toLong())
                    .get()
                    .await()
                
                val videos = documents.mapNotNull { it.toObject(Video::class.java) }
                allVideos.addAll(videos)
            }
            
            // Sort by creation time and limit
            val sortedVideos = allVideos.sortedByDescending { it.createdAt }.take(limit)
            Result.success(sortedVideos)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get following videos", e)
            Result.failure(e)
        }
    }
    
    suspend fun incrementVideoViews(videoId: String): Result<Unit> {
        return try {
            firestore.runTransaction { transaction ->
                val videoRef = videosCollection.document(videoId)
                val snapshot = transaction.get(videoRef)
                val currentViews = snapshot.getLong("viewsCount") ?: 0
                transaction.update(videoRef, "viewsCount", currentViews + 1)
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to increment video views", e)
            Result.failure(e)
        }
    }
    
    // Like Management
    suspend fun toggleLike(userId: String, videoId: String): Result<Boolean> {
        return try {
            val likeId = "${userId}_$videoId"
            val likeRef = likesCollection.document(likeId)
            val videoRef = videosCollection.document(videoId)
            
            val isLiked = firestore.runTransaction { transaction ->
                val likeSnapshot = transaction.get(likeRef)
                val videoSnapshot = transaction.get(videoRef)
                
                val currentLikes = videoSnapshot.getLong("likesCount") ?: 0
                
                if (likeSnapshot.exists()) {
                    // Unlike
                    transaction.delete(likeRef)
                    transaction.update(videoRef, "likesCount", currentLikes - 1)
                    false
                } else {
                    // Like
                    val like = mapOf(
                        "userId" to userId,
                        "videoId" to videoId,
                        "timestamp" to System.currentTimeMillis()
                    )
                    transaction.set(likeRef, like)
                    transaction.update(videoRef, "likesCount", currentLikes + 1)
                    true
                }
            }.await()
            
            Result.success(isLiked)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to toggle like", e)
            Result.failure(e)
        }
    }
    
    suspend fun isVideoLiked(userId: String, videoId: String): Result<Boolean> {
        return try {
            val likeId = "${userId}_$videoId"
            val document = likesCollection.document(likeId).get().await()
            Result.success(document.exists())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check if video is liked", e)
            Result.failure(e)
        }
    }
    
    // Follow Management
    suspend fun toggleFollow(followerId: String, followingId: String): Result<Boolean> {
        return try {
            val followId = "${followerId}_$followingId"
            val followRef = followsCollection.document(followId)
            val followerRef = usersCollection.document(followerId)
            val followingRef = usersCollection.document(followingId)
            
            val isFollowing = firestore.runTransaction { transaction ->
                val followSnapshot = transaction.get(followRef)
                val followerSnapshot = transaction.get(followerRef)
                val followingSnapshot = transaction.get(followingRef)
                
                val followerFollowingCount = followerSnapshot.getLong("followingCount") ?: 0
                val followingFollowersCount = followingSnapshot.getLong("followersCount") ?: 0
                
                if (followSnapshot.exists()) {
                    // Unfollow
                    transaction.delete(followRef)
                    transaction.update(followerRef, "followingCount", followerFollowingCount - 1)
                    transaction.update(followingRef, "followersCount", followingFollowersCount - 1)
                    false
                } else {
                    // Follow
                    val follow = mapOf(
                        "followerId" to followerId,
                        "followingId" to followingId,
                        "timestamp" to System.currentTimeMillis()
                    )
                    transaction.set(followRef, follow)
                    transaction.update(followerRef, "followingCount", followerFollowingCount + 1)
                    transaction.update(followingRef, "followersCount", followingFollowersCount + 1)
                    true
                }
            }.await()
            
            Result.success(isFollowing)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to toggle follow", e)
            Result.failure(e)
        }
    }
    
    suspend fun isFollowing(followerId: String, followingId: String): Result<Boolean> {
        return try {
            val followId = "${followerId}_$followingId"
            val document = followsCollection.document(followId).get().await()
            Result.success(document.exists())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check follow status", e)
            Result.failure(e)
        }
    }
    
    // Live Stream Management
    suspend fun createLiveStream(streamData: LiveStreamData): Result<String> {
        return try {
            val docRef = liveStreamsCollection.add(streamData).await()
            val streamId = docRef.id
            
            liveStreamsCollection.document(streamId).update("id", streamId).await()
            Result.success(streamId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create live stream", e)
            Result.failure(e)
        }
    }
    
    suspend fun getLiveStreams(limit: Int = 10): Result<List<LiveStreamData>> {
        return try {
            val documents = liveStreamsCollection
                .whereEqualTo("isLive", true)
                .orderBy("startTime", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()
            
            val streams = documents.mapNotNull { it.toObject(LiveStreamData::class.java) }
            Result.success(streams)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get live streams", e)
            Result.failure(e)
        }
    }
    
    suspend fun endLiveStream(streamId: String): Result<Unit> {
        return try {
            val updates = mapOf(
                "isLive" to false,
                "endTime" to System.currentTimeMillis()
            )
            liveStreamsCollection.document(streamId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to end live stream", e)
            Result.failure(e)
        }
    }
    
    // Creator Fund Management
    suspend fun getCreatorFund(userId: String): Result<CreatorFund> {
        return try {
            val document = creatorFundCollection.document(userId).get().await()
            val creatorFund = document.toObject(CreatorFund::class.java)
                ?: CreatorFund(userId = userId)
            Result.success(creatorFund)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get creator fund", e)
            Result.failure(e)
        }
    }
    
    suspend fun updateCreatorEarnings(userId: String, earnings: Float): Result<Unit> {
        return try {
            val fundRef = creatorFundCollection.document(userId)
            
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(fundRef)
                val currentFund = snapshot.toObject(CreatorFund::class.java)
                    ?: CreatorFund(userId = userId)
                
                val updatedFund = currentFund.copy(
                    totalEarnings = currentFund.totalEarnings + earnings,
                    monthlyEarnings = currentFund.monthlyEarnings + earnings,
                    dailyEarnings = currentFund.dailyEarnings + earnings,
                    pendingPayment = currentFund.pendingPayment + earnings
                )
                
                transaction.set(fundRef, updatedFund)
            }.await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update creator earnings", e)
            Result.failure(e)
        }
    }
    
    // Virtual Gifts Management
    suspend fun sendVirtualGift(transaction: GiftTransaction): Result<Unit> {
        return try {
            firestore.runTransaction { firestoreTransaction ->
                // Record the gift transaction
                val transactionRef = giftTransactionsCollection.document()
                firestoreTransaction.set(transactionRef, transaction)
                
                // Update receiver's earnings
                val fundRef = creatorFundCollection.document(transaction.toUserId)
                val fundSnapshot = firestoreTransaction.get(fundRef)
                val currentFund = fundSnapshot.toObject(CreatorFund::class.java)
                    ?: CreatorFund(userId = transaction.toUserId)
                
                val updatedFund = currentFund.copy(
                    totalEarnings = currentFund.totalEarnings + transaction.totalValue,
                    pendingPayment = currentFund.pendingPayment + transaction.totalValue
                )
                
                firestoreTransaction.set(fundRef, updatedFund)
            }.await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send virtual gift", e)
            Result.failure(e)
        }
    }
    
    // Analytics Management
    suspend fun recordAnalytics(analytics: CreatorAnalytics): Result<Unit> {
        return try {
            val docId = "${analytics.userId}_${analytics.period}_${System.currentTimeMillis()}"
            analyticsCollection.document(docId).set(analytics).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to record analytics", e)
            Result.failure(e)
        }
    }
    
    suspend fun getAnalytics(userId: String, period: AnalyticsPeriod): Result<CreatorAnalytics> {
        return try {
            val documents = analyticsCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("period", period.name)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()
            
            val analytics = documents.firstOrNull()?.toObject(CreatorAnalytics::class.java)
                ?: CreatorAnalytics(userId = userId, period = period)
            
            Result.success(analytics)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get analytics", e)
            Result.failure(e)
        }
    }
    
    // Real-time listeners
    fun listenToLiveStreamChat(streamId: String): Flow<List<LiveChatMessage>> {
        return chatMessagesCollection
            .whereEqualTo("streamId", streamId)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.mapNotNull { it.toObject(LiveChatMessage::class.java) }
            }
    }
    
    fun listenToUserNotifications(userId: String): Flow<List<Notification>> {
        return notificationsCollection
            .whereEqualTo("userId", userId)
            .whereEqualTo("isRead", false)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.mapNotNull { it.toObject(Notification::class.java) }
            }
    }
    
    // Utility functions
    private suspend fun incrementUserVideoCount(userId: String) {
        try {
            firestore.runTransaction { transaction ->
                val userRef = usersCollection.document(userId)
                val snapshot = transaction.get(userRef)
                val currentCount = snapshot.getLong("videosCount") ?: 0
                transaction.update(userRef, "videosCount", currentCount + 1)
            }.await()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to increment user video count", e)
        }
    }
    
    private suspend fun getLastDocument(page: Int, limit: Int): DocumentSnapshot? {
        return try {
            val offset = (page - 1) * limit
            if (offset <= 0) return null
            
            val documents = videosCollection
                .whereEqualTo("isPrivate", false)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(offset.toLong())
                .get()
                .await()
            
            documents.lastOrNull()
        } catch (e: Exception) {
            null
        }
    }
    
    // Batch operations
    suspend fun batchUpdateVideos(updates: List<Pair<String, Map<String, Any>>>): Result<Unit> {
        return try {
            val batch = firestore.batch()
            
            updates.forEach { (videoId, updateMap) ->
                val videoRef = videosCollection.document(videoId)
                batch.update(videoRef, updateMap)
            }
            
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to batch update videos", e)
            Result.failure(e)
        }
    }
    
    // Report and moderation
    suspend fun reportContent(
        reporterId: String,
        contentId: String,
        contentType: String,
        reason: String,
        description: String
    ): Result<Unit> {
        return try {
            val report = mapOf(
                "reporterId" to reporterId,
                "contentId" to contentId,
                "contentType" to contentType,
                "reason" to reason,
                "description" to description,
                "timestamp" to System.currentTimeMillis(),
                "status" to "pending"
            )
            
            reportsCollection.add(report).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to report content", e)
            Result.failure(e)
        }
    }
    
    companion object {
        private const val TAG = "FirestoreManager"
        
        // Collection names
        const val USERS_COLLECTION = "users"
        const val VIDEOS_COLLECTION = "videos"
        const val COMMENTS_COLLECTION = "comments"
        const val LIKES_COLLECTION = "likes"
        const val FOLLOWS_COLLECTION = "follows"
        const val ANALYTICS_COLLECTION = "analytics"
        const val CREATOR_FUND_COLLECTION = "creator_fund"
        const val VIRTUAL_GIFTS_COLLECTION = "virtual_gifts"
        const val GIFT_TRANSACTIONS_COLLECTION = "gift_transactions"
        const val LIVE_STREAMS_COLLECTION = "live_streams"
        const val CHAT_MESSAGES_COLLECTION = "chat_messages"
        const val NOTIFICATIONS_COLLECTION = "notifications"
        const val REPORTS_COLLECTION = "reports"
        const val BRAND_PARTNERSHIPS_COLLECTION = "brand_partnerships"
    }
}

// Helper data class for notifications
data class Notification(
    val id: String = "",
    val userId: String = "",
    val type: String = "",
    val title: String = "",
    val message: String = "",
    val data: Map<String, String> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)