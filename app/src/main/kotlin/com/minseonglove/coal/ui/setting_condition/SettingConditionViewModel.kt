package com.minseonglove.coal.ui.setting_condition

import android.util.Log
import androidx.databinding.Bindable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minseonglove.coal.db.MyAlarm
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.minseonglove.coal.api.data.Constants.Companion.MACD
import com.minseonglove.coal.api.data.Constants.Companion.MOVING_AVERAGE
import com.minseonglove.coal.api.data.Constants.Companion.PRICE
import com.minseonglove.coal.api.data.Constants.Companion.RSI
import com.minseonglove.coal.api.data.Constants.Companion.STOCHASTIC

class SettingConditionViewModel : ViewModel() {

    val candle = MutableStateFlow<String?>(null)
    val stochasticK = MutableStateFlow<String?>(null)
    val stochasticD = MutableStateFlow<String?>(null)
    val macdM = MutableStateFlow<String?>(null)
    val limitValue = MutableStateFlow<String?>(null)
    val signal = MutableStateFlow<String?>(null)

    private val _priceVisible = MutableStateFlow(8)
    private val _candleVisible = MutableStateFlow(8)
    private val _maVisible = MutableStateFlow(8)
    private val _stochasticVisible = MutableStateFlow(8)
    private val _macdVisible = MutableStateFlow(8)
    private val _valueVisible = MutableStateFlow(8)
    private val _signalVisible = MutableStateFlow(8)
    private val _minutePos = MutableStateFlow(0)
    private val _indicator = MutableStateFlow(0)
    private val _valueCondition = MutableStateFlow(0)
    private val _signalCondition = MutableStateFlow(0)

    val priceVisible: StateFlow<Int> get() = _priceVisible
    val candleVisible: StateFlow<Int> get() = _candleVisible
    val maVisible: StateFlow<Int> get() = _maVisible
    val stochasticVisible: StateFlow<Int> get() = _stochasticVisible
    val macdVisible: StateFlow<Int> get() = _macdVisible
    val valueVisible: StateFlow<Int> get() = _valueVisible
    val signalVisible: StateFlow<Int> get() = _signalVisible
    val minutePos: StateFlow<Int> get() = _minutePos
    val indicator: StateFlow<Int> get() = _indicator
    val valueCondition: StateFlow<Int> get() = _valueCondition
    val signalCondition: StateFlow<Int> get() = _signalCondition

    fun setSpinner(type: Int, pos: Int) {
        viewModelScope.launch {
            when (type) {
                0 -> _minutePos.emit(pos)
                1 -> _indicator.emit(pos)
                2 -> _valueCondition.emit(pos)
                3 -> _signalCondition.emit(pos)
            }
        }
    }

    fun getVisible() {
        viewModelScope.launch {
            shutdownAll()
            when(indicator.value) {
                PRICE -> {
                    _priceVisible.emit(0)
                }
                MOVING_AVERAGE -> {
                    _candleVisible.emit(0)
                    _maVisible.emit(0)
                }
                RSI -> {
                    _candleVisible.emit(0)
                    _valueVisible.emit(0)
                    _maVisible.emit(4)
                    _signalVisible.emit(0)
                }
                STOCHASTIC -> {
                    _stochasticVisible.emit(0)
                    _valueVisible.emit(0)
                }
                MACD -> {
                    _macdVisible.emit(0)
                    _signalVisible.emit(0)
                }
            }
        }
    }

    private suspend fun shutdownAll() {
        _priceVisible.emit(8)
        _candleVisible.emit(8)
        _maVisible.emit(8)
        _stochasticVisible.emit(8)
        _macdVisible.emit(8)
        _valueVisible.emit(8)
        _signalVisible.emit(8)
    }

    fun getAlarm(coinName: String, minute: Int): MyAlarm =
        MyAlarm(
            0,
            coinName,
            minute,
            indicator.value,
            candle.value?.toInt(),
            stochasticK.value?.toInt(),
            stochasticD.value?.toInt(),
            macdM.value?.toInt(),
            limitValue.value?.toDouble(),
            valueCondition.value,
            signal.value?.toInt(),
            signalCondition.value,
            true
        )
}
