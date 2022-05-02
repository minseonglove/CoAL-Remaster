package com.minseonglove.coal.ui.search_result

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.emptyPreferences
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.minseonglove.coal.R
import com.minseonglove.coal.api.data.Constants
import com.minseonglove.coal.api.data.Constants.datastore
import com.minseonglove.coal.api.data.Constants.makeConditionString
import com.minseonglove.coal.databinding.FragmentSearchResultBinding
import com.minseonglove.coal.db.MyAlarm
import com.minseonglove.coal.service.SearchResultService
import com.minseonglove.coal.ui.coin_select.CoinListAdapter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.IOException

class SearchResultFragment : Fragment() {

    private lateinit var searchResultAdapter: CoinListAdapter
    private lateinit var searchResultService: SearchResultService
    private var _binding: FragmentSearchResultBinding? = null
    private var isBound = false

    private val binding get() = _binding!!
    private val args: SearchResultFragmentArgs by navArgs()
    private val viewModel: SearchResultViewModel by viewModels()

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            searchResultService = (service as SearchResultService.SearchResultBinder).getService()
            searchResultService.registerCallback(viewModel.callback)
            isBound = true
            getCoinList()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
        }
    }

    private val coinList: Flow<List<String>> by lazy {
        requireContext().datastore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[Constants.SAVED_COIN_LIST]?.toList() ?: listOf()
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbarSearchResult.buttonToolbarNavigation.setOnClickListener {
            findNavController().navigate(R.id.action_searchResultFragment_to_coinSearchFragment)
        }
        with(args.condition) {
            binding.textviewSearchResultCondition.text =
                makeConditionString(
                MyAlarm(
                    0,
                    "",
                    minute,
                    indicator,
                    candle,
                    stochasticK,
                    stochasticD,
                    macdM,
                    value,
                    valueCondition,
                    signal,
                    signalCondition,
                    true
                ),
                resources.getStringArray(R.array.indicator_items),
                resources.getStringArray(R.array.up_down_items),
                resources.getStringArray(R.array.cross_items)
            )
        }
        initRecyclerView()
        initCollector()
        initService()
    }

    private fun initCollector() {
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.searchList.collectLatest {
                    searchResultAdapter.submitList(it)
                }
            }
        }
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.searchCount.collectLatest {
                    val progress = (it / viewModel.totalCount.value.toDouble() * 100).toInt()
                    binding.textviewSearchResultProgress.text =
                        "$progress% (${viewModel.totalCount.value} / $it)"
                }
            }
        }
    }

    private fun initService() {
        Intent(requireActivity(), SearchResultService::class.java).let {
            it.putExtra("condition", args.condition)
            requireActivity().bindService(it, connection, Context.BIND_AUTO_CREATE)
        }
    }

    private fun getCoinList() {
        lifecycleScope.launch {
            coinList.collect {
                it.sorted().let { sortedList ->
                    viewModel.setTotalCount(sortedList.size)
                    // 하나씩 검사 시작
                    searchResultService.getStarted(sortedList)
                }
            }
        }
    }

    private fun initRecyclerView() {
        searchResultAdapter = CoinListAdapter { }.apply {
            submitList(emptyList())
        }
        val divider = DividerItemDecoration(requireContext(), LinearLayout.VERTICAL).apply {
            setDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.divider_coin_list)!!)
        }
        binding.recyclerSearchResult.apply {
            layoutManager = LinearLayoutManager(activity)
            addItemDecoration(divider)
            adapter = searchResultAdapter
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        requireActivity().unbindService(connection)
        isBound = false
        _binding = null
    }
}
