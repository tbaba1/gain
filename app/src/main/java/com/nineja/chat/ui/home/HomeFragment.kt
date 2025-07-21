package com.nineja.chat.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.nineja.chat.adapter.VideoFeedAdapter
import com.nineja.chat.databinding.FragmentHomeBinding
import com.nineja.chat.model.Video
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {
    
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: HomeViewModel by viewModels()
    private lateinit var videoAdapter: VideoFeedAdapter
    private var currentPosition = 0
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupVideoFeed()
        observeViewModel()
        loadInitialVideos()
    }
    
    private fun setupVideoFeed() {
        videoAdapter = VideoFeedAdapter(
            onVideoClick = { video ->
                // Handle video tap (pause/play)
                viewModel.toggleVideoPlayback(video.id)
            },
            onLikeClick = { video ->
                viewModel.toggleLike(video.id)
            },
            onCommentClick = { video ->
                // Open comments bottom sheet
                showCommentsBottomSheet(video)
            },
            onShareClick = { video ->
                shareVideo(video)
            },
            onFollowClick = { user ->
                viewModel.toggleFollow(user.id)
            },
            onProfileClick = { user ->
                navigateToProfile(user.id)
            }
        )
        
        binding.videosRecyclerView.apply {
            adapter = videoAdapter
            layoutManager = LinearLayoutManager(context)
            
            // Add snap helper for full screen videos
            val snapHelper = PagerSnapHelper()
            snapHelper.attachToRecyclerView(this)
            
            // Preload videos for smooth playback
            setItemViewCacheSize(10)
            
            // Handle scroll events for auto-play
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                        val position = layoutManager.findFirstCompletelyVisibleItemPosition()
                        
                        if (position != RecyclerView.NO_POSITION && position != currentPosition) {
                            currentPosition = position
                            videoAdapter.playVideoAt(position)
                        }
                    }
                }
            })
        }
        
        // Setup pull to refresh
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshVideos()
        }
        
        // Setup scroll to load more
        binding.videosRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val totalItemCount = layoutManager.itemCount
                val lastVisibleItem = layoutManager.findLastVisibleItemPosition()
                
                if (lastVisibleItem >= totalItemCount - 3) {
                    viewModel.loadMoreVideos()
                }
            }
        })
    }
    
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.videos.collect { videos ->
                videoAdapter.submitList(videos)
                binding.swipeRefresh.isRefreshing = false
                
                // Auto-play first video if list is not empty
                if (videos.isNotEmpty() && currentPosition == 0) {
                    videoAdapter.playVideoAt(0)
                }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                binding.progressBar.visibility = if (isLoading && videoAdapter.itemCount == 0) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.error.collect { error ->
                error?.let {
                    showError(it)
                }
            }
        }
    }
    
    private fun loadInitialVideos() {
        viewModel.loadVideos()
    }
    
    private fun showCommentsBottomSheet(video: Video) {
        val commentsBottomSheet = CommentsBottomSheetFragment.newInstance(video.id)
        commentsBottomSheet.show(parentFragmentManager, "comments")
    }
    
    private fun shareVideo(video: Video) {
        // Implement video sharing
        viewModel.shareVideo(video.id)
    }
    
    private fun navigateToProfile(userId: String) {
        // Navigate to profile fragment
        // findNavController().navigate(
        //     HomeFragmentDirections.actionHomeToProfile(userId)
        // )
    }
    
    private fun showError(message: String) {
        binding.errorText.text = message
        binding.errorText.visibility = View.VISIBLE
        
        // Hide error after 3 seconds
        binding.errorText.postDelayed({
            binding.errorText.visibility = View.GONE
        }, 3000)
    }
    
    override fun onResume() {
        super.onResume()
        // Resume video playback
        videoAdapter.resumeCurrentVideo()
    }
    
    override fun onPause() {
        super.onPause()
        // Pause video playback
        videoAdapter.pauseCurrentVideo()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        videoAdapter.releaseAllPlayers()
        _binding = null
    }
}