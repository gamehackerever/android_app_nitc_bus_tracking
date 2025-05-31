package com.nitc.nitcbustracker

import Adapters.NoticeAdapter
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class GetNoticeFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var noticeAdapter: NoticeAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_notice_get, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.noticeRecyclerView)

        // Setup adapter with empty list initially
        noticeAdapter = NoticeAdapter(mutableListOf())
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = noticeAdapter

        lifecycleScope.launch {
            while (isActive) {
                try {
                    val response = RetrofitClient.api.getNotices()
                    val notices = response.body()?.takeLast(10)?.reversed() // Take last 10 records
                    Log.d("GetNoticeFragment", "Fetched notices: $notices")

                    if (!notices.isNullOrEmpty()) {
                        noticeAdapter.updateNotices(notices)
                    }
                } catch (e: Exception) {
                    Log.e("GetNoticeFragment", "Failed to fetch notices", e)
                }

                delay(10000L) // Refresh every 10 seconds
            }
        }
    }
}
