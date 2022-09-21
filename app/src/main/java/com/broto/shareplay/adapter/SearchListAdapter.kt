package com.broto.shareplay.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.broto.shareplay.R
import com.broto.shareplay.repository.SharePlayApiRepository
import com.broto.shareplay.databinding.SearchResultItemBinding
import com.broto.shareplay.repository.PlaylistManager
import com.broto.shareplay.retrofit.model.PostSearchResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.URL

class SearchListAdapter(private val context: Context): RecyclerView.Adapter<SearchListAdapter.SearchItemHolder>() {

    private var dataList: List<PostSearchResponse.MessageBody.SearchItem> = ArrayList(0)

    companion object {
        private const val TAG = "SearchListAdapter"
    }

    class SearchItemHolder(private val binding: SearchResultItemBinding):
        RecyclerView.ViewHolder(binding.root) {

            fun bind(data: PostSearchResponse.MessageBody.SearchItem, context: Context) {
                binding.root.setOnClickListener {
                    Log.d(TAG, "Clicked on id: ${binding.tvItemId}")
                    SharePlayApiRepository.getInstance().getAudioUrl(context, binding.tvItemId.text.toString())
                }
                binding.ivFavIcon.setOnClickListener {
                    Log.d(TAG, "Adding to playlist. Id: ${data.id}")
                    if (PlaylistManager.getInstance().isQueued(data.id)) {
                        Log.d(TAG, "This media is already queued to playlist. Remove it.")
                        PlaylistManager.getInstance().removeItemFromPlaylist(data.id)
                        binding.ivFavIcon.setImageResource(R.drawable.ic_add_to_playlist)
                        Toast.makeText(context, "Removed from playlist", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    PlaylistManager.getInstance().addItemToPlayList(data.id)
                    Toast.makeText(context, "Added to playlist", Toast.LENGTH_SHORT).show()
                    binding.ivFavIcon.setImageResource(R.drawable.ic_tick)
                }
                if (PlaylistManager.getInstance().isQueued(data.id)) {
                    binding.ivFavIcon.setImageResource(R.drawable.ic_tick)
                } else {
                    binding.ivFavIcon.setImageResource(R.drawable.ic_add_to_playlist)
                }
                binding.searchResultItem = data
                binding.executePendingBindings()
                if (data.thumbnail != null)
                fetchImageFromUrl(data.thumbnail.url, binding.ivSongItemCover)
            }

        private fun fetchImageFromUrl(url: String, imageView: ImageView) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    @Suppress("BlockingMethodInNonBlockingContext")
                    val inputStream = URL(url).openStream()
                    val icon = BitmapFactory.decodeStream(inputStream)
                    CoroutineScope(Dispatchers.Main).launch {
                        imageView.setImageBitmap(icon)
                    }
                } catch (e: Exception) {
                    Log.e(this.javaClass.simpleName.toString(), "Exception loading search item image. ${e.message}")
                    Log.e(this.javaClass.simpleName.toString(), e.printStackTrace().toString())
                }
            }
        }

        companion object {
            fun from(parent: ViewGroup): SearchItemHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = SearchResultItemBinding.inflate(layoutInflater, parent, false)
                return SearchItemHolder(binding)
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchItemHolder {
        return SearchItemHolder.from(parent)
    }

    override fun onBindViewHolder(holder: SearchItemHolder, position: Int) {
        holder.bind(dataList[position], context)
    }

    override fun getItemCount(): Int {
        val size = dataList.size
        return size
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(data: List<PostSearchResponse.MessageBody.SearchItem>) {
        Log.d(TAG, "updateData: New List size: ${data.size}")
        dataList = data
        notifyDataSetChanged()
    }


}