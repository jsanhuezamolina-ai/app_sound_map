package com.jonathan.soundmap.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jonathan.soundmap.data.repository.NoiseSpotRepository
import com.jonathan.soundmap.domain.model.NoiseSpot
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapHistoryViewModel @Inject constructor(
    private val repository: NoiseSpotRepository,
) : ViewModel() {

    val spots: StateFlow<List<NoiseSpot>> = repository.observeSpots()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun delete(spot: NoiseSpot) {
        viewModelScope.launch { repository.delete(spot.id) }
    }
}
