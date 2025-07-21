package com.nineja.chat.firebase

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileInputStream
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseStorageManager @Inject constructor(
    private val context: Context
) {
    
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
    private val storageRef: StorageReference = storage.reference
    
    interface UploadProgressListener {
        fun onProgress(progress: Int)
        fun onSuccess(downloadUrl: String)
        fun onFailure(error: String)
        fun onPaused()
        fun onResumed()
    }
    
    // Upload video file
    suspend fun uploadVideo(
        videoFile: File,
        userId: String,
        progressListener: UploadProgressListener? = null
    ): Result<String> {
        return try {
            val fileName = generateFileName("video", "mp4")
            val videoRef = storageRef.child("videos/$userId/$fileName")
            
            val uploadTask = videoRef.putFile(Uri.fromFile(videoFile))
            
            // Add progress listener
            progressListener?.let { listener ->
                uploadTask.addOnProgressListener { taskSnapshot ->
                    val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
                    listener.onProgress(progress)
                }.addOnPausedListener {
                    listener.onPaused()
                }.addOnResumedListener {
                    listener.onResumed()
                }
            }
            
            // Wait for upload completion
            val taskSnapshot = uploadTask.await()
            val downloadUrl = taskSnapshot.storage.downloadUrl.await().toString()
            
            progressListener?.onSuccess(downloadUrl)
            Result.success(downloadUrl)
            
        } catch (e: Exception) {
            Log.e(TAG, "Video upload failed", e)
            progressListener?.onFailure(e.message ?: "Upload failed")
            Result.failure(e)
        }
    }
    
    // Upload video thumbnail
    suspend fun uploadVideoThumbnail(
        thumbnailFile: File,
        userId: String,
        videoId: String
    ): Result<String> {
        return try {
            val fileName = "${videoId}_thumbnail.jpg"
            val thumbnailRef = storageRef.child("thumbnails/$userId/$fileName")
            
            val downloadUrl = thumbnailRef.putFile(Uri.fromFile(thumbnailFile))
                .await()
                .storage
                .downloadUrl
                .await()
                .toString()
            
            Result.success(downloadUrl)
            
        } catch (e: Exception) {
            Log.e(TAG, "Thumbnail upload failed", e)
            Result.failure(e)
        }
    }
    
    // Upload profile image
    suspend fun uploadProfileImage(
        imageFile: File,
        userId: String,
        progressListener: UploadProgressListener? = null
    ): Result<String> {
        return try {
            val fileName = "profile_${System.currentTimeMillis()}.jpg"
            val imageRef = storageRef.child("profile_images/$userId/$fileName")
            
            val uploadTask = imageRef.putFile(Uri.fromFile(imageFile))
            
            // Add progress listener
            progressListener?.let { listener ->
                uploadTask.addOnProgressListener { taskSnapshot ->
                    val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
                    listener.onProgress(progress)
                }
            }
            
            val downloadUrl = uploadTask.await().storage.downloadUrl.await().toString()
            
            progressListener?.onSuccess(downloadUrl)
            Result.success(downloadUrl)
            
        } catch (e: Exception) {
            Log.e(TAG, "Profile image upload failed", e)
            progressListener?.onFailure(e.message ?: "Upload failed")
            Result.failure(e)
        }
    }
    
    // Upload live stream thumbnail
    suspend fun uploadLiveStreamThumbnail(
        imageUri: Uri,
        streamId: String,
        userId: String
    ): Result<String> {
        return try {
            val fileName = "${streamId}_live_thumbnail.jpg"
            val thumbnailRef = storageRef.child("live_thumbnails/$userId/$fileName")
            
            val downloadUrl = thumbnailRef.putUri(imageUri)
                .await()
                .storage
                .downloadUrl
                .await()
                .toString()
            
            Result.success(downloadUrl)
            
        } catch (e: Exception) {
            Log.e(TAG, "Live thumbnail upload failed", e)
            Result.failure(e)
        }
    }
    
    // Upload audio file (for sounds/music)
    suspend fun uploadAudio(
        audioFile: File,
        userId: String,
        soundName: String
    ): Result<String> {
        return try {
            val fileName = "${soundName}_${System.currentTimeMillis()}.mp3"
            val audioRef = storageRef.child("sounds/$userId/$fileName")
            
            val downloadUrl = audioRef.putFile(Uri.fromFile(audioFile))
                .await()
                .storage
                .downloadUrl
                .await()
                .toString()
            
            Result.success(downloadUrl)
            
        } catch (e: Exception) {
            Log.e(TAG, "Audio upload failed", e)
            Result.failure(e)
        }
    }
    
    // Upload story content
    suspend fun uploadStoryContent(
        contentFile: File,
        userId: String,
        isVideo: Boolean
    ): Result<String> {
        return try {
            val extension = if (isVideo) "mp4" else "jpg"
            val fileName = "story_${System.currentTimeMillis()}.$extension"
            val storyRef = storageRef.child("stories/$userId/$fileName")
            
            val downloadUrl = storyRef.putFile(Uri.fromFile(contentFile))
                .await()
                .storage
                .downloadUrl
                .await()
                .toString()
            
            Result.success(downloadUrl)
            
        } catch (e: Exception) {
            Log.e(TAG, "Story upload failed", e)
            Result.failure(e)
        }
    }
    
    // Upload chat attachment
    suspend fun uploadChatAttachment(
        file: File,
        chatId: String,
        userId: String,
        fileType: String
    ): Result<String> {
        return try {
            val extension = when (fileType) {
                "image" -> "jpg"
                "video" -> "mp4"
                "audio" -> "mp3"
                else -> "bin"
            }
            
            val fileName = "attachment_${System.currentTimeMillis()}.$extension"
            val attachmentRef = storageRef.child("chat_attachments/$chatId/$userId/$fileName")
            
            val downloadUrl = attachmentRef.putFile(Uri.fromFile(file))
                .await()
                .storage
                .downloadUrl
                .await()
                .toString()
            
            Result.success(downloadUrl)
            
        } catch (e: Exception) {
            Log.e(TAG, "Chat attachment upload failed", e)
            Result.failure(e)
        }
    }
    
    // Upload multiple files (batch upload)
    suspend fun uploadMultipleFiles(
        files: List<Pair<File, String>>, // File to folder path
        userId: String,
        progressListener: UploadProgressListener? = null
    ): Result<List<String>> {
        return try {
            val uploadTasks = mutableListOf<UploadTask>()
            val downloadUrls = mutableListOf<String>()
            
            files.forEachIndexed { index, (file, folderPath) ->
                val fileName = generateFileName("file", getFileExtension(file.name))
                val fileRef = storageRef.child("$folderPath/$userId/$fileName")
                
                val uploadTask = fileRef.putFile(Uri.fromFile(file))
                uploadTasks.add(uploadTask)
                
                // Track overall progress
                uploadTask.addOnProgressListener { taskSnapshot ->
                    val fileProgress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
                    val overallProgress = ((index * 100 + fileProgress) / files.size)
                    progressListener?.onProgress(overallProgress)
                }
            }
            
            // Wait for all uploads to complete
            uploadTasks.forEach { task ->
                val snapshot = task.await()
                val url = snapshot.storage.downloadUrl.await().toString()
                downloadUrls.add(url)
            }
            
            progressListener?.onSuccess("All files uploaded")
            Result.success(downloadUrls)
            
        } catch (e: Exception) {
            Log.e(TAG, "Batch upload failed", e)
            progressListener?.onFailure(e.message ?: "Batch upload failed")
            Result.failure(e)
        }
    }
    
    // Delete file from storage
    suspend fun deleteFile(downloadUrl: String): Result<Unit> {
        return try {
            val fileRef = storage.getReferenceFromUrl(downloadUrl)
            fileRef.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "File deletion failed", e)
            Result.failure(e)
        }
    }
    
    // Delete user's all content (for account deletion)
    suspend fun deleteUserContent(userId: String): Result<Unit> {
        return try {
            val userFolders = listOf("videos", "thumbnails", "profile_images", "sounds", "stories", "live_thumbnails")
            
            userFolders.forEach { folder ->
                val folderRef = storageRef.child("$folder/$userId")
                deleteFolder(folderRef)
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "User content deletion failed", e)
            Result.failure(e)
        }
    }
    
    // Get file metadata
    suspend fun getFileMetadata(downloadUrl: String): Result<Map<String, Any>> {
        return try {
            val fileRef = storage.getReferenceFromUrl(downloadUrl)
            val metadata = fileRef.metadata.await()
            
            val metadataMap = mapOf(
                "name" to (metadata.name ?: ""),
                "size" to metadata.sizeBytes,
                "contentType" to (metadata.contentType ?: ""),
                "timeCreated" to (metadata.creationTimeMillis),
                "updated" to (metadata.updatedTimeMillis),
                "downloadTokens" to (metadata.getCustomMetadata("firebaseStorageDownloadTokens") ?: "")
            )
            
            Result.success(metadataMap)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get file metadata", e)
            Result.failure(e)
        }
    }
    
    // Get download URL from file path
    suspend fun getDownloadUrl(filePath: String): Result<String> {
        return try {
            val fileRef = storageRef.child(filePath)
            val downloadUrl = fileRef.downloadUrl.await().toString()
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get download URL", e)
            Result.failure(e)
        }
    }
    
    // Update file metadata
    suspend fun updateFileMetadata(
        downloadUrl: String,
        customMetadata: Map<String, String>
    ): Result<Unit> {
        return try {
            val fileRef = storage.getReferenceFromUrl(downloadUrl)
            val metadata = com.google.firebase.storage.StorageMetadata.Builder()
            
            customMetadata.forEach { (key, value) ->
                metadata.setCustomMetadata(key, value)
            }
            
            fileRef.updateMetadata(metadata.build()).await()
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update file metadata", e)
            Result.failure(e)
        }
    }
    
    // Compress and upload video (for better performance)
    suspend fun compressAndUploadVideo(
        videoFile: File,
        userId: String,
        compressionQuality: Int = 80,
        progressListener: UploadProgressListener? = null
    ): Result<String> {
        return try {
            // In a real app, you would compress the video here
            // For now, we'll just upload the original file
            val compressedFile = videoFile // TODO: Implement video compression
            
            uploadVideo(compressedFile, userId, progressListener)
            
        } catch (e: Exception) {
            Log.e(TAG, "Video compression and upload failed", e)
            Result.failure(e)
        }
    }
    
    // Helper functions
    private fun generateFileName(prefix: String, extension: String): String {
        val timestamp = System.currentTimeMillis()
        val random = UUID.randomUUID().toString().take(8)
        return "${prefix}_${timestamp}_$random.$extension"
    }
    
    private fun getFileExtension(fileName: String): String {
        return fileName.substringAfterLast('.', "")
    }
    
    private suspend fun deleteFolder(folderRef: StorageReference) {
        try {
            val listResult = folderRef.listAll().await()
            
            // Delete all files in the folder
            listResult.items.forEach { fileRef ->
                fileRef.delete().await()
            }
            
            // Recursively delete subfolders
            listResult.prefixes.forEach { subfolderRef ->
                deleteFolder(subfolderRef)
            }
            
        } catch (e: Exception) {
            Log.w(TAG, "Failed to delete folder: ${folderRef.path}", e)
        }
    }
    
    // Create resumable upload for large files
    fun createResumableUpload(
        file: File,
        storagePath: String,
        progressListener: UploadProgressListener
    ): UploadTask {
        val fileRef = storageRef.child(storagePath)
        val uploadTask = fileRef.putFile(Uri.fromFile(file))
        
        uploadTask.addOnProgressListener { taskSnapshot ->
            val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
            progressListener.onProgress(progress)
        }.addOnSuccessListener { taskSnapshot ->
            taskSnapshot.storage.downloadUrl.addOnSuccessListener { uri ->
                progressListener.onSuccess(uri.toString())
            }
        }.addOnFailureListener { exception ->
            progressListener.onFailure(exception.message ?: "Upload failed")
        }.addOnPausedListener {
            progressListener.onPaused()
        }.addOnResumedListener {
            progressListener.onResumed()
        }
        
        return uploadTask
    }
    
    // Get storage usage for user
    suspend fun getUserStorageUsage(userId: String): Result<Long> {
        return try {
            var totalSize = 0L
            val userFolders = listOf("videos", "thumbnails", "profile_images", "sounds", "stories")
            
            userFolders.forEach { folder ->
                val folderRef = storageRef.child("$folder/$userId")
                try {
                    val listResult = folderRef.listAll().await()
                    for (item in listResult.items) {
                        val metadata = item.metadata.await()
                        totalSize += metadata.sizeBytes
                    }
                } catch (e: Exception) {
                    // Folder might not exist, continue
                }
            }
            
            Result.success(totalSize)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to calculate storage usage", e)
            Result.failure(e)
        }
    }
    
    companion object {
        private const val TAG = "FirebaseStorageManager"
        
        // Storage limits
        const val MAX_VIDEO_SIZE_MB = 100L
        const val MAX_IMAGE_SIZE_MB = 10L
        const val MAX_AUDIO_SIZE_MB = 25L
        
        // Quality settings
        const val VIDEO_QUALITY_HIGH = 90
        const val VIDEO_QUALITY_MEDIUM = 70
        const val VIDEO_QUALITY_LOW = 50
    }
}