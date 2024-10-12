package com.example.testapp.lalgebra

import android.content.res.Configuration
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.AlignHorizontalLeft
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PhotoCameraBack
import androidx.compose.material.icons.outlined.RemoveRedEye
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SettingsInputAntenna
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.testapp.Camera
import com.example.testapp.R
import com.example.testapp.Screen
import com.example.testapp.filecnt.MenuItemContent
import com.example.testapp.filecnt.NavigationItem
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatrixLayout(
    navController: NavController,
    state: MatrixState,
    onEvent: (MatrixEvent) -> Unit
) {
    val navHostController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    var isContextMenuVisible by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var selectedItemIndex by rememberSaveable { mutableIntStateOf(0) }

    var showResult by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val navItems = listOf(
        NavigationItem(
            title = "Basic Operations",
            selectedIcon = ImageVector.vectorResource(id = R.drawable.calculator_variant),
            unselectedIcon = ImageVector.vectorResource(id = R.drawable.calculator_variant_outline),
            screen = BscOps
        ),
        NavigationItem(
            title = "Determinant",
            selectedIcon = ImageVector.vectorResource(id = R.drawable.code_array),
            unselectedIcon = ImageVector.vectorResource(id = R.drawable.code_array),
            screen = Det
        ),
        NavigationItem(
            title = "Inverse Matrix",
            selectedIcon = ImageVector.vectorResource(id = R.drawable.multiplication_box),
            unselectedIcon = ImageVector.vectorResource(id = R.drawable.multiplication),
            screen = Invs
        )
    )

    ModalNavigationDrawer(
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(16.dp))
                navItems.forEachIndexed{index,item ->
                    NavigationDrawerItem(
                        label = { Text(item.title)},
                        selected = index == selectedItemIndex,
                        onClick = {
                            selectedItemIndex = index
                            navHostController.navigate(item.screen)
                            scope.launch {
                                drawerState.close()
                            }
                        },
                        icon = {
                            Icon(imageVector = if(index==selectedItemIndex){ item.selectedIcon }else item.unselectedIcon
                                ,contentDescription = item.title
                            )
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        },
        drawerState = drawerState,
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Mathsrooms") },
                    modifier = Modifier.padding(16.dp),
                    navigationIcon = {
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } }
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Outlined.AlignHorizontalLeft, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        IconButton(onClick = { navController.navigate(Camera) }) {
                            Icon(imageVector = Icons.Outlined.PhotoCameraBack, contentDescription = "Scan Input Matrix")
                        }
                        IconButton(onClick = { isContextMenuVisible = true }) {
                            Icon(imageVector = Icons.Outlined.MoreVert, contentDescription = "More Options")
                        }
                        DropdownMenu(
                            expanded = isContextMenuVisible,
                            onDismissRequest = { isContextMenuVisible = false }
                        ) {
                            DropdownMenuItem(
                                onClick = { isContextMenuVisible = false },
                                text = { MenuItemContent("Search", Icons.Outlined.Search) }
                            )
                            DropdownMenuItem(
                                onClick = { isContextMenuVisible = false },
                                text = { MenuItemContent("Manual Connection", Icons.Outlined.SettingsInputAntenna) }
                            )
                            DropdownMenuItem(
                                onClick = { isContextMenuVisible = false },
                                text = { MenuItemContent("App Info", Icons.Outlined.Info) }
                            )
                            DropdownMenuItem(
                                onClick = { isContextMenuVisible = false },
                                text = { MenuItemContent("Fade Chat", Icons.Outlined.RemoveRedEye) }
                            )
                            DropdownMenuItem(
                                onClick = { isContextMenuVisible = false },
                                text = { MenuItemContent("Delete Chat", Icons.Outlined.Delete) }
                            )
                        }
                    }
                )
            },
            bottomBar = {
                BottomAppBar(
                    content = {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            IconButton(
                                onClick = {
                                    onEvent(MatrixEvent.ComputeAddition)
                                    showResult = true
                                }
                            ) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(id = R.drawable.equal), // Cambia 'equal' por el nombre de tu archivo
                                    contentDescription = "Equal Icon"
                                )
                            }
                        }
                    },
                    modifier = Modifier.padding(16.dp)
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                focusManager.clearFocus() // Clear focus from any focused element
                            }
                        )
                    }
            ) {
                NavHost(navController = navHostController, startDestination = BscOps) {
                    composable<BscOps> {
                        BasicOperations(modifier = Modifier, state, onEvent)
                    }
                    composable<MM> {
                        MatrixMultiplication(modifier = Modifier)
                    }
                    composable<Det> {
                        Determinant(modifier = Modifier)
                    }
                    composable<Invs> {
                        Inverse(modifier = Modifier)
                    }
                }
                if (showResult) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                    ) {
                        if (state.result.isNotEmpty()) {
                            Text(
                                text = "Resultado: ${state.result.joinToString("\n") { it.joinToString(", ") }}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        } else {
                            Text(
                                text = "No hay resultado disponible.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Grid(
    n: Int,
    matrix: List<List<Number>>,
    onMatrixChange: (List<List<Number>>) -> Unit
) {
    // Cada celda se inicia como una cadena vacía
    val gridValues = remember(n) {
        List(n) { List(n) { mutableStateOf("") } }
    }

    // Obtenemos las dimensiones y la orientación de la pantalla
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT

    val cellSize = if (isPortrait) {
        (screenWidth / (n + 1)).coerceAtMost(80.dp) // Ajustamos el tamaño máximo a 80dp
    } else {
        (screenHeight / (n + 1)).coerceAtMost(80.dp)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        for (i in 0 until n) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                for (j in 0 until n) {
                    OutlinedTextField(
                        value = gridValues[i][j].value,
                        onValueChange = { newValue ->
                            gridValues[i][j].value = newValue
                            val updatedValue = newValue.toDoubleOrNull() ?: 0.0
                            val newMatrix = matrix.mapIndexed { rowIndex, row ->
                                row.mapIndexed { colIndex, value ->
                                    if (rowIndex == i && colIndex == j) {
                                        updatedValue  // Asigna el valor convertido a la posición correspondiente
                                    } else {
                                        value  // Mantiene el valor existente
                                    }
                                }
                            }

                            onMatrixChange(newMatrix)
                        },
                        modifier = Modifier
                            .padding(4.dp)
                            .size(cellSize),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun BasicOperations(
    modifier: Modifier,
    matrixState: MatrixState,
    onEvent: (MatrixEvent) -> Unit
) {
    var sliderValue by remember { mutableFloatStateOf(3f) }
    val intSliderValue = sliderValue.toInt()

    var sliderValue2 by remember { mutableFloatStateOf(3f) }
    val intSliderValue2 = sliderValue2.toInt()

    Column(
        modifier = Modifier
            .padding(4.dp)
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 10.dp, end = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Slider(
                value = sliderValue,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.secondary,
                    activeTrackColor = MaterialTheme.colorScheme.secondary,
                    inactiveTrackColor = MaterialTheme.colorScheme.secondaryContainer,
                ),
                onValueChange = { sliderValue = it },
                valueRange = 1f..10f,
                steps = 8
            )
        }

        Grid(intSliderValue, matrixState.matrixA) {
                updatedMatrix -> onEvent(MatrixEvent.InputMatrix(updatedMatrix, MatrixTarget.MATRIX_A))
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 10.dp, end = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Slider(
                value = sliderValue2,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.secondary,
                    activeTrackColor = MaterialTheme.colorScheme.secondary,
                    inactiveTrackColor = MaterialTheme.colorScheme.secondaryContainer,
                ),
                onValueChange = { sliderValue2 = it },
                valueRange = 1f..10f,
                steps = 8
            )
        }
        Grid(intSliderValue2, matrixState.matrixB) {
                updatedMatrix -> onEvent(MatrixEvent.InputMatrix(updatedMatrix, MatrixTarget.MATRIX_B))
        }
    }
}

@Composable
fun MatrixMultiplication(modifier: Modifier)
{
    Text("d creo", modifier)
}

@Composable
fun Determinant(modifier: Modifier)
{
    Text("x creo", modifier)
}

@Composable
fun Inverse(modifier: Modifier)
{
    Text("inversa creo", modifier)
}


@Serializable
object BscOps : Screen

@Serializable
object MM : Screen

@Serializable
object Det : Screen

@Serializable
object Invs : Screen

