package com.nineja.chat.ui.live

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.nineja.chat.databinding.ActivityLiveStreamBinding
import com.nineja.chat.model.LiveChatMessage
import com.nineja.chat.model.LiveStreamData
import com.nineja.chat.ui.live.adapter.LiveChatAdapter
import com.nineja.chat.utils.LiveStreamManager
import com.nineja.chat.utils.PermissionUtils
import kotlinx.coroutines.launch
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LiveStreamActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityLiveStreamBinding
    private val viewModel: LiveStreamViewModel by viewModels()
    private lateinit var liveStreamManager: LiveStreamManager
    private lateinit var chatAdapter: LiveChatAdapter
    
    private var isStreaming = false
    private var isHost = false
    private var streamId: String? = null
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            startLiveStream()
        } else {
            showPermissionDeniedDialog()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLiveStreamBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupWindow()
        setupUI()
        setupLiveStream()
        observeViewModel()
        
        // Check if starting as host or viewer
        isHost = intent.getBooleanExtra("is_host", false)
        streamId = intent.getStringExtra("stream_id")
        
        if (isHost) {
            checkPermissionsAndStart()
        } else {
            streamId?.let { joinLiveStream(it) }
        }
    }
    
    private fun setupWindow() {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.statusBarColor = ContextCompat.getColor(this, android.R.color.transparent)
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
    }
    
    private fun setupUI() {
        // Setup chat RecyclerView
        chatAdapter = LiveChatAdapter { message ->
            handleChatAction(message)
        }
        
        binding.chatRecyclerView.apply {
            adapter = chatAdapter
            layoutManager = LinearLayoutManager(this@LiveStreamActivity).apply {
                stackFromEnd = true
            }
        }
        
        // Setup click listeners
        binding.btnEndStream.setOnClickListener {
            if (isHost) {
                endLiveStream()
            } else {
                finish()
            }
        }
        
        binding.btnSwitchCamera.setOnClickListener {
            if (isHost) {
                liveStreamManager.switchCamera()
            }
        }
        
        binding.btnToggleMute.setOnClickListener {
            if (isHost) {
                val isMuted = liveStreamManager.toggleMute()
                binding.btnToggleMute.isSelected = isMuted
            }
        }
        
        binding.btnSendMessage.setOnClickListener {
            sendChatMessage()
        }
        
        binding.btnGifts.setOnClickListener {
            showGiftsBottomSheet()
        }
        
        binding.btnFollow.setOnClickListener {
            followStreamer()
        }
        
        binding.btnShare.setOnClickListener {
            shareStream()
        }
        
        // Setup message input
        binding.etMessage.setOnEditorActionListener { _, _, _ ->
            sendChatMessage()
            true
        }
        
        // Show/hide controls based on role
        binding.hostControls.visibility = if (isHost) View.VISIBLE else View.GONE
        binding.viewerControls.visibility = if (isHost) View.GONE else View.VISIBLE
    }
    
    private fun setupLiveStream() {
        liveStreamManager = LiveStreamManager(this)
        
        liveStreamManager.setCallbacks(object : LiveStreamManager.Callbacks {
            override fun onStreamStarted(streamData: LiveStreamData) {
                isStreaming = true
                streamId = streamData.id
                binding.btnEndStream.text = "End Stream"
                
                // Update UI
                binding.viewerCount.text = "0"
                binding.streamTitle.text = streamData.title
            }
            
            override fun onStreamEnded() {
                isStreaming = false
                if (isHost) {
                    finish()
                }
            }
            
            override fun onViewerJoined(userId: String) {
                viewModel.addViewer(userId)
            }
            
            override fun onViewerLeft(userId: String) {
                viewModel.removeViewer(userId)
            }
            
            override fun onChatMessage(message: LiveChatMessage) {
                runOnUiThread {
                    chatAdapter.addMessage(message)
                    binding.chatRecyclerView.scrollToPosition(chatAdapter.itemCount - 1)
                }
            }
            
            override fun onGiftReceived(giftId: String, fromUserId: String, amount: Int) {
                runOnUiThread {
                    showGiftAnimation(giftId, amount)
                    viewModel.addGift(giftId, fromUserId, amount)
                }
            }
            
            override fun onError(error: String) {
                runOnUiThread {
                    showError(error)
                }
            }
        })
    }
    
    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.viewerCount.collect { count ->
                binding.viewerCount.text = count.toString()
            }
        }
        
        lifecycleScope.launch {
            viewModel.totalEarnings.collect { earnings ->
                if (isHost) {
                    binding.earningsText.text = "₦$earnings"
                    binding.earningsText.visibility = View.VISIBLE
                }
            }
        }
        
        lifecycleScope.launch {
            viewModel.streamData.collect { streamData ->
                streamData?.let {
                    binding.streamTitle.text = it.title
                    binding.streamerName.text = it.streamerName
                    binding.streamerAvatar.let { imageView ->
                        // Load streamer avatar with Glide
                        // Glide.with(this@LiveStreamActivity).load(it.streamerAvatar).into(imageView)
                    }
                }
            }
        }
        
        lifecycleScope.launch {
            viewModel.chatMessages.collect { messages ->
                chatAdapter.submitList(messages)
                if (messages.isNotEmpty()) {
                    binding.chatRecyclerView.scrollToPosition(messages.size - 1)
                }
            }
        }
    }
    
    private fun checkPermissionsAndStart() {
        val requiredPermissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
        
        val missingPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        
        if (missingPermissions.isEmpty()) {
            startLiveStream()
        } else {
            permissionLauncher.launch(requiredPermissions)
        }
    }
    
    private fun startLiveStream() {
        val streamData = LiveStreamData(
            id = generateStreamId(),
            title = intent.getStringExtra("stream_title") ?: "Live Stream",
            streamerName = intent.getStringExtra("streamer_name") ?: "Streamer",
            streamerAvatar = intent.getStringExtra("streamer_avatar") ?: "",
            category = intent.getStringExtra("category") ?: "General",
            isLive = true
        )
        
        binding.cameraPreview.let { surfaceView ->
            liveStreamManager.startStream(streamData, surfaceView)
        }
        
        viewModel.startStream(streamData)
    }
    
    private fun joinLiveStream(streamId: String) {
        viewModel.joinStream(streamId)
        binding.cameraPreview.let { surfaceView ->
            liveStreamManager.joinStream(streamId, surfaceView)
        }
    }
    
    private fun endLiveStream() {
        if (isHost && isStreaming) {
            liveStreamManager.endStream()
            viewModel.endStream()
            
            // Show stream summary
            showStreamSummary()
        } else {
            finish()
        }
    }
    
    private fun sendChatMessage() {
        val messageText = binding.etMessage.text.toString().trim()
        if (messageText.isNotEmpty()) {
            val message = LiveChatMessage(
                id = generateMessageId(),
                userId = viewModel.getCurrentUserId(),
                username = viewModel.getCurrentUsername(),
                message = messageText,
                timestamp = System.currentTimeMillis(),
                type = LiveChatMessage.Type.TEXT
            )
            
            liveStreamManager.sendChatMessage(message)
            viewModel.sendMessage(message)
            
            binding.etMessage.setText("")
        }
    }
    
    private fun showGiftsBottomSheet() {
        val giftsBottomSheet = LiveGiftsBottomSheet { gift ->
            sendGift(gift)
        }
        giftsBottomSheet.show(supportFragmentManager, "gifts")
    }
    
    private fun sendGift(gift: LiveGift) {
        if (!isHost) {
            liveStreamManager.sendGift(gift)
            viewModel.sendGift(gift)
            
            // Show gift animation
            showGiftAnimation(gift.id, gift.value)
        }
    }
    
    private fun followStreamer() {
        if (!isHost) {
            viewModel.followStreamer()
            binding.btnFollow.text = "Following"
            binding.btnFollow.isEnabled = false
        }
    }
    
    private fun shareStream() {
        streamId?.let { id ->
            val shareText = "Join me on 9jaChat Live! \nStream: ${binding.streamTitle.text}\nLink: https://9jachat.com/live/$id"
            val shareIntent = android.content.Intent().apply {
                action = android.content.Intent.ACTION_SEND
                type = "text/plain"
                putExtra(android.content.Intent.EXTRA_TEXT, shareText)
            }
            startActivity(android.content.Intent.createChooser(shareIntent, "Share Stream"))
        }
    }
    
    private fun showGiftAnimation(giftId: String, amount: Int) {
        // Implement gift animation using Lottie or custom animations
        binding.giftAnimationView.visibility = View.VISIBLE
        binding.giftAnimationView.animate()
            .alpha(1f)
            .scaleX(1.2f)
            .scaleY(1.2f)
            .setDuration(1000)
            .withEndAction {
                binding.giftAnimationView.animate()
                    .alpha(0f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(500)
                    .withEndAction {
                        binding.giftAnimationView.visibility = View.GONE
                    }
            }
        
        // Show gift notification
        val giftMessage = LiveChatMessage(
            id = generateMessageId(),
            userId = "",
            username = "System",
            message = "Sent $amount coins gift!",
            timestamp = System.currentTimeMillis(),
            type = LiveChatMessage.Type.GIFT,
            giftId = giftId,
            giftAmount = amount
        )
        
        chatAdapter.addMessage(giftMessage)
    }
    
    private fun handleChatAction(message: LiveChatMessage) {
        when (message.type) {
            LiveChatMessage.Type.FOLLOW -> {
                // Handle follow notification
            }
            LiveChatMessage.Type.GIFT -> {
                // Handle gift notification
            }
            LiveChatMessage.Type.JOIN -> {
                // Handle viewer join notification
            }
            else -> {
                // Handle regular text message
            }
        }
    }
    
    private fun showStreamSummary() {
        val summary = viewModel.getStreamSummary()
        val summaryDialog = LiveStreamSummaryDialog(summary) {
            finish()
        }
        summaryDialog.show(supportFragmentManager, "stream_summary")
    }
    
    private fun showPermissionDeniedDialog() {
        android.app.AlertDialog.Builder(this)
            .setTitle("Permissions Required")
            .setMessage("Camera and microphone permissions are required for live streaming")
            .setPositiveButton("Settings") { _, _ ->
                PermissionUtils.openAppSettings(this)
            }
            .setNegativeButton("Cancel") { _, _ ->
                finish()
            }
            .show()
    }
    
    private fun showError(error: String) {
        android.app.AlertDialog.Builder(this)
            .setTitle("Stream Error")
            .setMessage(error)
            .setPositiveButton("OK") { _, _ ->
                finish()
            }
            .show()
    }
    
    private fun generateStreamId(): String {
        return "stream_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }
    
    private fun generateMessageId(): String {
        return "msg_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }
    
    override fun onResume() {
        super.onResume()
        if (isStreaming) {
            liveStreamManager.resumeStream()
        }
    }
    
    override fun onPause() {
        super.onPause()
        if (isStreaming) {
            liveStreamManager.pauseStream()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        if (isStreaming) {
            liveStreamManager.cleanup()
        }
    }
    
    override fun onBackPressed() {
        if (isHost && isStreaming) {
            android.app.AlertDialog.Builder(this)
                .setTitle("End Stream?")
                .setMessage("Are you sure you want to end your live stream?")
                .setPositiveButton("End Stream") { _, _ ->
                    endLiveStream()
                }
                .setNegativeButton("Continue") { _, _ -> }
                .show()
        } else {
            super.onBackPressed()
        }
    }
}