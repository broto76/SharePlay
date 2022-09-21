package com.broto.shareplay.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.broto.shareplay.repository.SharePlayApiRepository
import com.broto.shareplay.adapter.SearchListAdapter
import com.broto.shareplay.databinding.FragmentSearchResultBinding

class SearchResultFragment : Fragment() {

    companion object {
        private const val TAG = "SearchResultFragment"
    }

    private var _binding: FragmentSearchResultBinding? = null
    private val mBinding get() = _binding!!
    private var recyclerView: RecyclerView? = null
    private val adapter: SearchListAdapter by lazy { SearchListAdapter(requireContext()) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentSearchResultBinding.inflate(inflater, container, false)
        mBinding.lifecycleOwner = this
        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = mBinding.rvSearchResults
        recyclerView?.adapter = adapter
        recyclerView?.layoutManager = LinearLayoutManager(requireContext())

        hideRecyclerView()
        SharePlayApiRepository.getInstance().mSearchResults.observe(viewLifecycleOwner) {
            Log.d(TAG, "Search result obtained. Count: ${it.size}")
            adapter.updateData(it)
            showRecyclerView()
        }
        SharePlayApiRepository.getInstance().mIsSearching.observe(viewLifecycleOwner) {
            Log.d(TAG, "Searching status: $it")
            if (it) {
                hideRecyclerView()
            } else {
                showRecyclerView()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showRecyclerView() {
        Log.d(TAG, "showRecyclerView: ")
        recyclerView?.visibility = View.VISIBLE
    }

    private fun hideRecyclerView() {
        Log.d(TAG, "hideRecyclerView:")
        mBinding.rvSearchResults.visibility = View.INVISIBLE
    }
}