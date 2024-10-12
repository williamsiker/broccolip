package com.example.testapp.lalgebra

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class MatrixState(
    val matrixA: List<List<Number>> = listOf(),
    val matrixB: List<List<Number>> = listOf(),
    val result: List<List<Number>> = listOf()
)

sealed class MatrixEvent {
    data class InputMatrix(val matrix: List<List<Number>>, val target: MatrixTarget): MatrixEvent()
    data object ComputeAddition: MatrixEvent()
    data object ComputeSubtraction: MatrixEvent()
    data object ComputeDeterminant: MatrixEvent()
    data object ComputeScalarMultiplication: MatrixEvent()
}

enum class MatrixTarget { MATRIX_A, MATRIX_B }

class MatrixViewModel : ViewModel() {
    private val _state = MutableStateFlow(MatrixState())
    val state = _state.asStateFlow()

    fun onEvent(event: MatrixEvent) {
        when (event) {
            is MatrixEvent.InputMatrix -> {
                updateMatrix(event.matrix, event.target)
            }
            is MatrixEvent.ComputeAddition -> {
                val result = addMatrices(_state.value.matrixA, _state.value.matrixB!!)
                _state.value = _state.value.copy(result = result)
            }
            is MatrixEvent.ComputeDeterminant -> {
                val result = computeDeterminant(_state.value.matrixA)
                // Actualiza el estado del resultado con el determinante
            }

            MatrixEvent.ComputeScalarMultiplication -> TODO()
            MatrixEvent.ComputeSubtraction -> TODO()
        }
    }

    private fun updateMatrix(matrix: List<List<Number>>, target: MatrixTarget) {
        when (target) {
            MatrixTarget.MATRIX_A -> _state.value = _state.value.copy(matrixA = matrix)
            MatrixTarget.MATRIX_B -> _state.value = _state.value.copy(matrixB = matrix)
        }
    }

    private fun addMatrices(a: List<List<Number>>, b: List<List<Number>>): List<List<Number>> {
        assert(a.size == b.size && a[0].size == b[0].size)
        val rows = a.size
        val cols = a[0].size

        val result = MutableList(rows) { MutableList<Number>(cols) { 0 } }
        for(i in 0 until rows)
        {
            for(j in 0 until cols)
            {
                val sum = (a[i][j].toDouble() + b[i][j].toDouble())
                result[i][j] = sum
            }
        }
        return result.map { it.toList() }
    }

    private fun computeDeterminant(matrix: List<List<Number>>): Number {
        // Lógica de cálculo del determinante
        return 0 // Resultado
    }
}
