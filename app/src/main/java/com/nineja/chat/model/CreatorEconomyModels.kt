package com.nineja.chat.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CreatorFund(
    val userId: String,
    val totalEarnings: Float = 0f,
    val monthlyEarnings: Float = 0f,
    val dailyEarnings: Float = 0f,
    val totalViews: Long = 0L,
    val eligibleViews: Long = 0L, // Views that count for monetization
    val cpm: Float = 0f, // Cost per mille (1000 views)
    val tier: CreatorTier = CreatorTier.BRONZE,
    val lastPayment: Long = 0L,
    val pendingPayment: Float = 0f,
    val isEligible: Boolean = false,
    val requirements: CreatorRequirements = CreatorRequirements()
) : Parcelable

@Parcelize
data class CreatorRequirements(
    val minFollowers: Int = 1000,
    val minVideoViews: Long = 10000L,
    val minAge: Int = 18,
    val hasValidId: Boolean = false,
    val hasPaymentMethod: Boolean = false,
    val isNigerianResident: Boolean = false,
    val hasNoViolations: Boolean = true
) : Parcelable {
    
    fun isEligible(user: User): Boolean {
        return user.followersCount >= minFollowers &&
               user.videosCount >= 10 &&
               hasValidId &&
               hasPaymentMethod &&
               hasNoViolations
    }
}

enum class CreatorTier(val multiplier: Float, val minViews: Long, val benefits: List<String>) {
    BRONZE(1.0f, 0L, listOf("Basic monetization", "Creator badge")),
    SILVER(1.5f, 100000L, listOf("Higher CPM", "Early access to features", "Priority support")),
    GOLD(2.0f, 500000L, listOf("Premium CPM", "Exclusive events", "Brand partnerships")),
    PLATINUM(2.5f, 1000000L, listOf("Maximum CPM", "Personal manager", "Custom features")),
    DIAMOND(3.0f, 5000000L, listOf("Ultra premium benefits", "Revenue guarantees", "Platform partnership"))
}

@Parcelize
data class VirtualGift(
    val id: String,
    val name: String,
    val description: String,
    val iconUrl: String,
    val animationUrl: String = "",
    val price: Int, // in app coins
    val value: Float, // real money value in Naira
    val category: GiftCategory = GiftCategory.GENERAL,
    val rarity: GiftRarity = GiftRarity.COMMON,
    val isLimited: Boolean = false,
    val availableUntil: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
) : Parcelable

enum class GiftCategory {
    GENERAL, LOVE, CELEBRATION, GAMING, MUSIC, DANCE, COMEDY, NIGERIAN_SPECIAL
}

enum class GiftRarity(val multiplier: Float) {
    COMMON(1.0f),
    RARE(1.5f),
    EPIC(2.0f),
    LEGENDARY(3.0f),
    MYTHICAL(5.0f)
}

@Parcelize
data class GiftTransaction(
    val id: String,
    val fromUserId: String,
    val toUserId: String,
    val giftId: String,
    val gift: VirtualGift,
    val quantity: Int = 1,
    val totalCoins: Int,
    val totalValue: Float,
    val timestamp: Long = System.currentTimeMillis(),
    val videoId: String? = null,
    val liveStreamId: String? = null,
    val message: String = ""
) : Parcelable

@Parcelize
data class AppCoin(
    val userId: String,
    val balance: Int = 0,
    val totalPurchased: Int = 0,
    val totalSpent: Int = 0,
    val totalEarned: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
) : Parcelable {
    
    fun canAfford(amount: Int): Boolean = balance >= amount
    
    fun getFormattedBalance(): String {
        return when {
            balance >= 1_000_000 -> String.format("%.1fM", balance / 1_000_000.0)
            balance >= 1_000 -> String.format("%.1fK", balance / 1_000.0)
            else -> balance.toString()
        }
    }
}

@Parcelize
data class CreatorAnalytics(
    val userId: String,
    val period: AnalyticsPeriod,
    val totalViews: Long = 0L,
    val totalLikes: Long = 0L,
    val totalComments: Long = 0L,
    val totalShares: Long = 0L,
    val totalEarnings: Float = 0f,
    val avgEngagementRate: Float = 0f,
    val topPerformingVideo: String? = null,
    val audienceDemographics: AudienceDemographics = AudienceDemographics(),
    val growthMetrics: GrowthMetrics = GrowthMetrics(),
    val revenueBreakdown: RevenueBreakdown = RevenueBreakdown(),
    val contentPerformance: List<ContentMetric> = emptyList()
) : Parcelable

enum class AnalyticsPeriod {
    DAILY, WEEKLY, MONTHLY, YEARLY, ALL_TIME
}

@Parcelize
data class AudienceDemographics(
    val ageGroups: Map<String, Float> = emptyMap(), // "18-24" -> percentage
    val genderDistribution: Map<String, Float> = emptyMap(), // "male" -> percentage
    val locationDistribution: Map<String, Float> = emptyMap(), // "Lagos" -> percentage
    val deviceTypes: Map<String, Float> = emptyMap(), // "android" -> percentage
    val topLanguages: Map<String, Float> = emptyMap() // "english" -> percentage
) : Parcelable

@Parcelize
data class GrowthMetrics(
    val followerGrowthRate: Float = 0f,
    val viewGrowthRate: Float = 0f,
    val engagementGrowthRate: Float = 0f,
    val revenueGrowthRate: Float = 0f,
    val contentFrequency: Float = 0f, // videos per week
    val retentionRate: Float = 0f, // audience retention
    val viralityScore: Float = 0f // how often content goes viral
) : Parcelable

@Parcelize
data class RevenueBreakdown(
    val creatorFund: Float = 0f,
    val virtualGifts: Float = 0f,
    val liveStreamDonations: Float = 0f,
    val brandPartnerships: Float = 0f,
    val merchandiseSales: Float = 0f,
    val tipJar: Float = 0f,
    val other: Float = 0f
) : Parcelable {
    
    fun getTotalRevenue(): Float {
        return creatorFund + virtualGifts + liveStreamDonations + 
               brandPartnerships + merchandiseSales + tipJar + other
    }
}

@Parcelize
data class ContentMetric(
    val videoId: String,
    val title: String,
    val thumbnailUrl: String,
    val views: Long,
    val likes: Long,
    val comments: Long,
    val shares: Long,
    val earnings: Float,
    val engagementRate: Float,
    val retentionRate: Float,
    val ctr: Float, // click-through rate
    val publishedAt: Long
) : Parcelable

@Parcelize
data class BrandPartnership(
    val id: String,
    val creatorId: String,
    val brandId: String,
    val brandName: String,
    val brandLogo: String,
    val campaignTitle: String,
    val campaignDescription: String,
    val budget: Float,
    val cpm: Float,
    val targetViews: Long,
    val currentViews: Long = 0L,
    val status: PartnershipStatus = PartnershipStatus.PENDING,
    val startDate: Long,
    val endDate: Long,
    val requirements: List<String> = emptyList(),
    val deliverables: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
) : Parcelable

enum class PartnershipStatus {
    PENDING, ACTIVE, COMPLETED, CANCELLED, EXPIRED
}

@Parcelize
data class CreatorStore(
    val creatorId: String,
    val storeName: String,
    val storeDescription: String,
    val storeLogoUrl: String = "",
    val isActive: Boolean = false,
    val products: List<Product> = emptyList(),
    val totalSales: Float = 0f,
    val totalOrders: Int = 0,
    val rating: Float = 0f,
    val reviewCount: Int = 0
) : Parcelable

@Parcelize
data class Product(
    val id: String,
    val name: String,
    val description: String,
    val price: Float,
    val currency: String = "NGN",
    val images: List<String> = emptyList(),
    val category: ProductCategory = ProductCategory.MERCHANDISE,
    val stock: Int = 0,
    val isDigital: Boolean = false,
    val tags: List<String> = emptyList(),
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) : Parcelable

enum class ProductCategory {
    MERCHANDISE, CLOTHING, ACCESSORIES, DIGITAL_CONTENT, COURSES, BOOKS, MUSIC
}

@Parcelize
data class LiveStreamData(
    val id: String,
    val streamerId: String,
    val streamerName: String,
    val streamerAvatar: String,
    val title: String,
    val description: String = "",
    val category: String = "",
    val thumbnailUrl: String = "",
    val streamUrl: String = "",
    val chatUrl: String = "",
    val viewerCount: Int = 0,
    val maxViewers: Int = 0,
    val totalGifts: Float = 0f,
    val totalEarnings: Float = 0f,
    val isLive: Boolean = false,
    val startTime: Long = 0L,
    val endTime: Long? = null,
    val duration: Long = 0L,
    val tags: List<String> = emptyList(),
    val isRecorded: Boolean = false,
    val recordingUrl: String = ""
) : Parcelable

@Parcelize
data class LiveChatMessage(
    val id: String,
    val userId: String,
    val username: String,
    val userAvatar: String = "",
    val message: String,
    val timestamp: Long,
    val type: Type = Type.TEXT,
    val giftId: String? = null,
    val giftAmount: Int = 0,
    val isFromStreamer: Boolean = false,
    val isHighlighted: Boolean = false,
    val replyToId: String? = null
) : Parcelable {
    
    enum class Type {
        TEXT, GIFT, FOLLOW, JOIN, LEAVE, LIKE, SHARE, SYSTEM
    }
}

@Parcelize
data class LiveGift(
    val id: String,
    val name: String,
    val description: String,
    val iconUrl: String,
    val animationUrl: String = "",
    val price: Int, // in coins
    val value: Float, // in Naira
    val category: GiftCategory = GiftCategory.GENERAL,
    val rarity: GiftRarity = GiftRarity.COMMON,
    val isSpecialEffect: Boolean = false,
    val cooldownMs: Long = 0L,
    val isNigerianExclusive: Boolean = false
) : Parcelable

@Parcelize
data class CreatorGoal(
    val id: String,
    val creatorId: String,
    val title: String,
    val description: String,
    val targetAmount: Float,
    val currentAmount: Float = 0f,
    val targetDate: Long,
    val category: GoalCategory = GoalCategory.GENERAL,
    val isActive: Boolean = true,
    val contributors: Int = 0,
    val rewards: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
) : Parcelable {
    
    fun getProgressPercentage(): Float {
        return if (targetAmount > 0) (currentAmount / targetAmount) * 100f else 0f
    }
    
    fun isCompleted(): Boolean = currentAmount >= targetAmount
    
    fun getDaysRemaining(): Long {
        val now = System.currentTimeMillis()
        return if (targetDate > now) (targetDate - now) / (24 * 60 * 60 * 1000) else 0L
    }
}

enum class GoalCategory {
    GENERAL, EQUIPMENT, EDUCATION, CHARITY, CONTENT_CREATION, BUSINESS_EXPANSION
}

@Parcelize
data class PaymentMethod(
    val id: String,
    val userId: String,
    val type: PaymentType,
    val accountName: String,
    val accountNumber: String = "",
    val bankName: String = "",
    val bankCode: String = "",
    val isVerified: Boolean = false,
    val isPrimary: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) : Parcelable

enum class PaymentType {
    BANK_ACCOUNT, MOBILE_MONEY, CRYPTO_WALLET, PAYPAL
}