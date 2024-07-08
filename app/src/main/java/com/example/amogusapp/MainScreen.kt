package com.example.amogusapp

import android.content.Context
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.AlignHorizontalLeft
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Camera
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.RemoveRedEye
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SettingsInputAntenna
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.launch


data class Connections (
    val osImgvector : ImageVector,
    val os : String,
    val device : String,
    var isOnsession : Boolean,
)

data class HistorialItem(
    val contenido: Any,
    var active : MutableState<Boolean> = mutableStateOf(false)
)

data class TabItem(
    val title : String,
    val unselectedIcon : ImageVector,
    val selectedIcon : ImageVector
)

data class NavigationItem(
    val title : String,
    val selectedIcon : ImageVector,
    val unselectedIcon: ImageVector
)

class MainUI : ComponentActivity() {
    init {
        try {
            System.loadLibrary("connections_api")
        } catch (e: UnsatisfiedLinkError) {
            Log.e("LibraryLoadError", "No se pudo cargar la biblioteca nativa 'connections_api'", e)
        }
    }

    private external fun fetchPageContent(): String
    private external fun uploadFileToFTP(ftpUrl: String, filePath: String): Boolean
    private external fun nativeTestConnection(ftpUrl: String?): Boolean
    private var ftpHost = ""
    private var ftpPort = 0
    private var ftpUser = ""
    private var ftpPasswd = ""
}

@Composable
fun Prueba(string : String){
    Text(string)
}

@Preview(showBackground = true)
@Composable
fun PromptScreenPreview() {
    val navController = rememberNavController()
    //Broccolink(navController, null)
    Broccolink(navController, null)
}

@Entity(tableName = "Chat")
data class ChatEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val titulo: String,
    val contenido: String
)

@Dao
interface ChatDao {
    @Query("SELECT * FROM Chat")
    fun getAllChats(): List<ChatEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: ChatEntity)
}

@Database(entities = [ChatEntity::class], version = 1)
abstract class ChatDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao

    companion object {
        @Volatile
        private var INSTANCE: ChatDatabase? = null

        fun getDatabase(context: Context): ChatDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ChatDatabase::class.java,
                    "chat_database" // Nombre de tu base de datos
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Broccolink(navController: NavController, credentials: String?) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    var isContextMenuVisible by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var selectedItemIndex by rememberSaveable { mutableIntStateOf(0) }
    var expanded by remember { mutableStateOf(false) }
    var currentText by remember { mutableStateOf("") }

    val context = LocalContext.current
    var isConnected by remember { mutableStateOf(false) }

    val navItems = listOf(
        NavigationItem(
            title = "Broccolink",
            selectedIcon = Icons.Filled.Home,
            unselectedIcon = Icons.Outlined.Home
        ),
        NavigationItem(
            title = "Settings",
            selectedIcon = Icons.Filled.Settings,
            unselectedIcon = Icons.Outlined.Settings
        )
    )

    //val chatDatabase = ChatDatabase.getDatabase(context)
    //val chatDao = chatDatabase.chatDao()
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
                    title = { Text("Broccolink 1.0 Alpha") },
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
                            Icon(imageVector = Icons.Outlined.QrCodeScanner, contentDescription = "Scan QR code")
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
                                text = { MenuItemContent("Manual Connections", Icons.Outlined.SettingsInputAntenna) }
                            )
                            DropdownMenuItem(
                                onClick = { isContextMenuVisible = false },
                                text = { MenuItemContent("Manual Connections", Icons.Outlined.Info) }
                            )
                            DropdownMenuItem(
                                onClick = { isContextMenuVisible = false },
                                text = { MenuItemContent("Manual Connections", Icons.Outlined.RemoveRedEye) }
                            )
                            DropdownMenuItem(
                                onClick = { isContextMenuVisible = false },
                                text = { MenuItemContent("Logout", Icons.Outlined.Delete) }
                            )
                        }
                    }
                )
            },
            bottomBar = {
                BottomAppBar(
                    content = {
                        Row(
                            modifier = Modifier
                                .fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AnimatedVisibility(
                                visible = expanded,
                                enter = expandHorizontally(animationSpec = tween(durationMillis = 150)),
                                exit = shrinkHorizontally(animationSpec = tween(durationMillis = 150))
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(onClick = { /*TODO*/ }) {
                                        Icon(imageVector = Icons.Outlined.Camera, contentDescription = "Manual Connections")
                                    }
                                    IconButton(onClick = { /*TODO*/ }) {
                                        Icon(imageVector = Icons.Outlined.FolderOpen, contentDescription = "Manual Connections")
                                    }
                                }
                            }
                            FloatingActionButton(
                                onClick = { expanded = !expanded },
                                shape = CircleShape,
                                backgroundColor = Color.Black,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = if(expanded) Icons.Default.Remove else Icons.Default.Add,
                                    contentDescription = "Add",
                                    tint = Color.White
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            TextField(
                                value = currentText,
                                onValueChange = { currentText = it },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("Type your message...") },
                                maxLines = Int.MAX_VALUE,
                                singleLine = false
                            )

                            IconButton(onClick = { /* Do something */ }) {
                                Icon(imageVector = Icons.Outlined.ArrowUpward, contentDescription = "Send")
                            }
                        }
                    },
                    modifier = Modifier.padding(16.dp)
                )
            }
        ) { paddingValues ->
            Box(modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()) {
                // Aquí va el contenido principal de tu pantalla
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(credentials?: "Welcome to My App!")
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { /* Do something */ }) {
                        Text("Click Me")
                    }
                }
            }
        }
    }

}

@Composable
fun MenuItemContent(text: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp)
    ) {
        Icon(imageVector = icon, contentDescription = text)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text)
    }
}


@Composable
fun Broccolink2(
    navController: NavController,
    credentials: String?
) {


//    Column(
//        modifier = Modifier.fillMaxSize(),
//        verticalArrangement = Arrangement.Center,
//        horizontalAlignment = Alignment.CenterHorizontally
//    ) {
//        Header(navController = navController)
//        if(credentials == null){
//            Text(text = "amogus")
//        }else{
//            Text(text = credentials)
//        }
//        Footer()
//    }
}

@Composable
fun Header(navController: NavController) {
    var isContextMenuVisible by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    Row(
        modifier = Modifier.fillMaxWidth()
    ){
        /*  1 */
        Row(
            modifier = Modifier
                //.wrapContentSize(Alignment.TopStart)
                .padding(16.dp)
                .weight(1f),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ){
            IconButton(
                onClick = {},
                enabled = false) {
                Icon(imageVector = Icons.AutoMirrored.Outlined.AlignHorizontalLeft, contentDescription = null)
            }
        }

        Row(
            modifier = Modifier
                .wrapContentSize(Alignment.TopCenter)
                .padding(16.dp)
                .weight(1f),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ){
            val imagePainter = painterResource(id = R.drawable.broccoli_stroke_rounded)
            IconButton(onClick = { /*TODO*/ }) {
                Icon(painter = imagePainter, contentDescription = null)
            }
        }

        /*Icon Buttons */
        Row(
            modifier = Modifier
                .padding(16.dp)
                .weight(1f)
                .wrapContentSize(Alignment.TopEnd),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                navController.navigate(Camera)
            }) {
                Icon(imageVector = Icons.Outlined.QrCodeScanner, contentDescription = "Qr Connection")
            }
            IconButton(onClick = {isContextMenuVisible = true}) {
                Icon(imageVector = Icons.Outlined.MoreVert, contentDescription = "More Options")
            }
            DropdownMenu(
                expanded = isContextMenuVisible,
                onDismissRequest = { isContextMenuVisible = false }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp, horizontal = 16.dp)
                ){
                    Icon(imageVector = Icons.Outlined.SettingsInputAntenna, contentDescription = "Manual Connections")

                    DropdownMenuItem(
                        text = { Text("Manual Connection", Modifier.padding(start=8.dp)) },
                        onClick = { isContextMenuVisible = false }
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp, horizontal = 16.dp)
                ){
                    Icon(imageVector = Icons.Outlined.Info, contentDescription = null)
                    DropdownMenuItem(
                        text = { Text("View Details", Modifier.padding(start=8.dp)) },
                        onClick = { isContextMenuVisible = false }
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp, horizontal = 16.dp)
                ) {
                    Icon(imageVector = Icons.Outlined.RemoveRedEye, contentDescription = null)
                    DropdownMenuItem(
                        text = { Text("Fade Chat",  Modifier.padding(start=8.dp)) },
                        onClick = { isContextMenuVisible = false }
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp, horizontal = 16.dp)
                ) {
                    Icon(imageVector = Icons.Outlined.Delete, contentDescription = null)
                    DropdownMenuItem(
                        text = { Text("Delete Chats",  Modifier.padding(start=8.dp)) },
                        onClick = { isContextMenuVisible = false }
                    )
                }
            }
        }
        /*Fin Icon Buttons */
    }
}

@Composable
fun Footer() {
    var expanded by remember { mutableStateOf(false) }
    var currentText by remember { mutableStateOf("") }
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                Row {
                    AnimatedVisibility(
                        visible = expanded,
                        enter = expandHorizontally(animationSpec = tween(durationMillis = 150)),
                        exit = shrinkHorizontally(animationSpec = tween(durationMillis = 150))
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { /*TODO*/ }) {
                                Icon(imageVector = Icons.Outlined.Camera, contentDescription = "Manual Connections")
                            }
                            IconButton(onClick = { /*TODO*/ }) {
                                Icon(imageVector = Icons.Outlined.FolderOpen, contentDescription = "Manual Connections")
                            }
                        }
                    }
                }

                Row{
                    AnimatedVisibility(
                        visible = !expanded && currentText.isEmpty(),
                        enter = expandHorizontally(animationSpec = tween(durationMillis = 150)),
                        exit = shrinkHorizontally(animationSpec = tween(durationMillis = 150))
                    ) {
                        FloatingActionButton(
                            onClick = { expanded = true },
                            shape = CircleShape,
                            backgroundColor = Color.Black,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Open Menu",
                                tint = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            TextField(
                value = currentText,
                onValueChange = {
                    currentText = it
                },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type your message...") }
            )

            IconButton(onClick = {  }) {
                Icon(imageVector = Icons.Outlined.ArrowUpward, contentDescription = "Manual Connections")
            }

        }
    }
}

//
//
//@OptIn(ExperimentalFoundationApi::class)
//@Composable
//fun MainScreen(navController: NavController, stringQr: String?) {
//    val bottomNavItems = listOf(
//        BottomNavItem.Home,
//        BottomNavItem.Sents,
//        BottomNavItem.MLLogs
//    )
//    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
//    val pagerState = rememberPagerState { bottomNavItems.size }
//
//    var offsetY by remember { mutableFloatStateOf(0f) }
//    val animatedOffset by animateFloatAsState(
//        targetValue = if (isVisible) 0f else 200f,
//        label = "tabrowScrollable"
//    )
//
//    var isContextMenuVisible by remember { mutableStateOf(false) }
//
//    LaunchedEffect(selectedTabIndex) {
//        pagerState.animateScrollToPage(selectedTabIndex)
//    }
//    LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
//        if (!pagerState.isScrollInProgress) {
//            selectedTabIndex = pagerState.currentPage
//        }
//    }
//
//    Scaffold(
//        topBar = {
//
//        },
//        bottomBar = {
//            AnimatedVisibility(
//                visible = isVisible,
//                enter = fadeIn(),
//                exit = fadeOut()
//            ) {
//                BottomNavigation(
//                    modifier = Modifier.offset(y = animatedOffset.dp)
//                ) {
//                    bottomNavItems.forEachIndexed { index, item ->
//                        BottomNavigationItem(
//                            icon = { Icon(item.icon, contentDescription = item.title) },
//                            label = { Text(item.title) },
//                            selected = selectedTabIndex == index,
//                            onClick = {
//                                selectedTabIndex = index
//                                navController.navigate(item.screenroute) {
//                                    navController.graph.startDestinationRoute?.let { route ->
//                                        popUpTo(route) {
//                                            saveState = true
//                                        }
//                                    }
//                                    launchSingleTop = true
//                                    restoreState = true
//                                }
//                            }
//                        )
//                    }
//                }
//            }
//        }
//    ) { innerPadding ->
//        Box(
//            modifier = Modifier
//                .padding(innerPadding)
//                .pointerInput(Unit) {
//                    detectVerticalDragGestures(
//                        onDragEnd = {
//                            isVisible = offsetY < -100f
//                            offsetY = 0f
//                        },
//                        onVerticalDrag = { _, dragAmount ->
//                            offsetY += dragAmount
//                        }
//                    )
//                }
//        ) {
//            Text("amogus")
////                HorizontalPager(
////                    state = pagerState,
////                    modifier = Modifier.fillMaxSize()
////                ) { page ->
////                    when (page) {
////                        0 -> HomeContent(
////                            stringQr = stringQr,
////                            navController = navController
////                        )
////                        1 -> Recieveds()
////                        2 -> ML_Logs()
////                    }
////                }
//        }
//    }
//}
//
//
//@Composable
//fun BottomBar(navController: NavController) {
//    val items = listOf(
//        BottomNavItem.Home,
//        BottomNavItem.Sents,
//        BottomNavItem.MLLogs
//    )
//    BottomNavigation(
//        backgroundColor = Color.White,
//        contentColor = Color.Black
//    ) {
//        items.forEach { item ->
//            BottomNavigationItem(
//                icon = { Icon(item.icon, contentDescription = item.title) },
//                label = { Text(text = item.title) },
//                selectedContentColor = Color.Blue,
//                unselectedContentColor = Color.Gray,
//                alwaysShowLabel = true,
//                selected = false, // Aquí podrías manejar la lógica de selección
//                onClick = {
//                    navController.navigate(item.screenroute) {
//                        // Evitar múltiples copias del mismo destino cuando se vuelve a seleccionar el mismo ítem
//                        navController.graph.startDestinationRoute?.let { route ->
//                            popUpTo(route) {
//                                saveState = true
//                            }
//                        }
//                        // Evitar múltiples copias del mismo destino en la pila de back stack
//                        launchSingleTop = true
//                        // Restaura el estado cuando vuelva a seleccionarse
//                        restoreState = true
//                    }
//                }
//            )
//        }
//    }
//}
//
//@SuppressLint("MutableCollectionMutableState")
//@Composable
//fun HomeContent(
//    navController: NavController,
//    stringQr: String?
//
//) {
//    var message by remember { mutableStateOf("") }
//    val fileList = remember { mutableStateListOf<HistorialItem>() }
//    val scope = rememberCoroutineScope()
//    val context = LocalContext.current
//    var isLoading by remember { mutableStateOf(false) }
//    val filePickerLauncher = rememberLauncherForActivityResult(
//        ActivityResultContracts.GetContent()
//    ) { uri ->
//        uri?.let {
//            val file = uriToFile(context, uri)
//            Log.d("file", uri.toString())
//            file?.let {
//                if (stringQr != null) {
//                    connectAndUploadFile(it, scope, stringQr)
//                }
//                fileList.add(HistorialItem(contenido = file))
//            }
//        }
//    }
//    Column(
//        modifier = Modifier
//            .fillMaxWidth(),
//        verticalArrangement = Arrangement.Center
//    ) {
//        LazyColumn(
//            modifier = Modifier
//                .fillMaxWidth()
//                .weight(1f),
//            verticalArrangement = Arrangement.spacedBy(8.dp)
//        ) {
//            items(fileList) { item ->
//                Row(
//                    verticalAlignment = Alignment.CenterVertically,
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(4.dp)
//                        .clickable {
//                            item.active.value = !item.active.value
//                            Log.d(
//                                "SwitchClick",
//                                "\"Item ${item.contenido} seleccionado: ${item.active.value}\""
//                            )
//                        }
//                ) {
//                    val imageModifier = Modifier
//                        .size(48.dp)
//                        .padding(end = 8.dp)
//                    val image = when (item.contenido) {
//                        is String -> Icons.Outlined.Textsms
//                        is File -> Icons.Outlined.UploadFile
//                        else -> Icons.Outlined.NotInterested
//                    }
//                    Icon(
//                        imageVector = image,
//                        contentDescription = "File icon",
//                        modifier = imageModifier
//                    )
//
//                    Text(
//                        text = when (val contenido = item.contenido) {
//                            is String -> contenido
//                            is File -> contenido.name
//                            else -> "Desconocido"
//                        },
//                        modifier = Modifier
//                            .weight(1f)
//                            .padding(end = 8.dp)
//                    )
//
//                    RadioButton(
//                        selected = item.active.value,
//                        onClick = {
//                            item.active.value = !item.active.value
//                            Log.e("SwitchClick", "Item ${item.contenido} seleccionado: ${item.active.value}")
//                        },
//                        modifier = Modifier.size(24.dp)
//                    )
//                }
//            }
//        }
//
//        val isActive by remember { mutableStateOf(true) }
//        AnimatedVisibility(
//            visible = isActive,
//            enter = fadeIn(),
//            exit = fadeOut()
//        ) {
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                TextField(
//                    value = message,
//                    onValueChange = { message = it },
//                    modifier = Modifier
//                        .weight(1f)
//                        .height(56.dp)
//                        .heightIn(min = 56.dp, max = 200.dp)
//                        .verticalScroll(rememberScrollState()),
//                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Start),
//                    label = { Text("Ingrese Texto Aquí") },
//                    singleLine = false,
//                    maxLines = 10,
//                )
//
//                IconButton(
//                    onClick = {
//                        filePickerLauncher.launch("*/*")
//                    }
//                ) {
//                    Icon(
//                        imageVector = Icons.Outlined.AttachFile,
//                        contentDescription = "Open Folder",
//                        modifier = Modifier.size(36.dp)
//                    )
//                }
//
//                IconButton(
//                    onClick = {
//                        filePickerLauncher.launch("*/*")
//                    },
//                ) {
//                    Icon(
//                        imageVector = Icons.Outlined.FolderOpen,
//                        contentDescription = "Open Folder",
//                        modifier = Modifier.size(36.dp)
//                    )
//                }
//
//                IconButton(
//                    onClick = {
//                        isLoading = true
//                        scope.launch {
//                            try {
//                                if (message.isNotEmpty()) {
//                                    sendMessage(message, this, URLEncoder.encode(stringQr, StandardCharsets.UTF_8.toString()))
//                                    fileList.add(HistorialItem(contenido = message))
//                                    message = ""
//                                } else {
//                                    fileList.filter { it.active.value }.forEach { item ->
//                                        when (val contenido = item.contenido) {
//                                            is String -> sendMessage(contenido, this, URLEncoder.encode(
//                                                stringQr, StandardCharsets.UTF_8.toString()))
//                                            is File -> connectAndUploadFile(contenido, this, URLEncoder.encode(
//                                                stringQr, StandardCharsets.UTF_8.toString()))
//                                        }
//                                        item.active.value = false
//                                    }
//                                }
//                            } catch (e: Exception) {
//                                Log.e("HomeContent", "Error sending message or file", e)
//                            } finally {
//                                isLoading = false
//                            }
//                        }
//                    }
//                ) {
//                    Icon(
//                        imageVector = Icons.Outlined.PlayArrow,
//                        contentDescription = "Enviar Todo",
//                        modifier = Modifier.size(36.dp)
//                    )
//                }
//            }
//        }
//
//        if (isLoading) {
//            CircularProgressIndicator(
//                modifier = Modifier.size(24.dp),
//                color = MaterialTheme.colorScheme.onPrimary
//            )
//        }
//    }
//}
//
//
//@Composable
//fun Recieveds() {
//    var isStarted = false
//    Column(
//        modifier = Modifier
//            .fillMaxHeight()
//            .padding(16.dp),
//        verticalArrangement = Arrangement.spacedBy(8.dp)
//    ) {
//        //Imagen
//        Row(
//            modifier = Modifier
//                .fillMaxWidth()
//                .height(300.dp),
//            verticalAlignment = Alignment.Top,
//            horizontalArrangement = Arrangement.Center
//        ) {
//            Icon(
//                imageVector = Icons.Outlined.Computer,
//                contentDescription = "Start FTP Service",
//                modifier = Modifier.fillMaxSize()
//            )
//        }
//
//        //Botton Start Service
//        Row(
//            modifier = Modifier.fillMaxWidth(),
//            horizontalArrangement = Arrangement.Center
//        ) {
//            Button(
//                onClick = {
//                    if(!isStarted) {
//                        isStarted = true
//                    }
//                },
//                colors = ButtonDefaults.buttonColors(Color.Blue)
//            ) {
//                if(!isStarted){
//                    Text(text = "Start Service")
//                }else{
//                    Text(text = "End Service")
//                }
//            }
//        }
//
//        //Fila Credenciales
//        Row(
//            modifier = Modifier.fillMaxWidth(),
//            horizontalArrangement = Arrangement.Center
//        ) {
//            Text(text = "pageContent")
//        }
//
//        //Fila Logs
//        Row(
//            modifier = Modifier.fillMaxWidth(),
//            horizontalArrangement = Arrangement.Center
//        ) {
//            TextField(
//                value = "logText",
//                onValueChange = {},
//                readOnly = true,
//                modifier = Modifier.fillMaxWidth(),
//                label = {Text("Logs")}
//            )
//        }
//    }
//}
//
//@Composable
//fun ML_Logs() {
//    val pageContent = fetchPageContent()
//    Text(pageContent)
//}
//
//
//
//@SuppressLint("Range")
//fun getFileNameFromUri(context: Context, uri: Uri): String? {
//    var fileName: String? = null
//    val cursor = context.contentResolver.query(uri, null, null, null, null)
//    cursor?.use {
//        fileName = if (it.moveToFirst()) {
//            val displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
//            if (displayNameIndex >= 0) {
//                cursor.getString(displayNameIndex)
//            } else {
//                null
//            }
//        } else {
//            null
//        }
//    }
//    return fileName
//}
//
//
//@SuppressLint("Recycle")
//fun uriToFile(context: Context, uri: Uri): File? {
//    val fileDescriptor = context.contentResolver.openFileDescriptor(uri, "r") ?: return null
//    val mimeType = context.contentResolver.getType(uri)
//    Log.d("UriToFile", "URI: $uri, MIME Type: $mimeType")
//
//    val extension = mimeType?.split("/")?.lastOrNull()
//    val fileName = getFileNameFromUri(context, uri) ?: "tempFile" // Obtiene el nombre de archivo
//
//    // Añade la extensión si se pudo determinar
//    val finalFileName = if (extension != null) {
//        fileName
//    } else {
//        fileName // Si no hay extensión, usa el nombre base
//    }
//
//    Log.d("UriToFile", "Generated file name: $finalFileName")
//
//    val file = File(context.cacheDir, finalFileName)
//
//    try {
//        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
//        val outputStream = FileOutputStream(file)
//        inputStream.copyTo(outputStream)
//        inputStream.close()
//        outputStream.close()
//        return file
//    } catch (e: Exception) {
//        Log.e("UriToFile", "Error copying file", e)
//    } finally {
//        fileDescriptor.close()
//    }
//    return null
//}
//
//private fun connectAndUploadFile(
//    file: File,
//    scope: CoroutineScope,
//    stringQr : String
//) {
//    val regularExpression = "ftp://(\\S+):(\\S+)@(.*):(\\d+)".toRegex()
//    val matchResult = regularExpression.find(stringQr)
//
//    if (matchResult != null) {
//        ftpUser = matchResult.groupValues[1]
//        ftpPasswd = matchResult.groupValues[2]
//        ftpHost = matchResult.groupValues[3]
//        ftpPort = matchResult.groupValues[4].toInt()
//
//        scope.launch(Dispatchers.IO) {
//            val encodedFileName = URLEncoder.encode(file.name, "UTF-8").replace("+", "%20")
//            val string = "$stringQr/$encodedFileName"
//            val connected = uploadFileToFTP(string, file.absolutePath)
//            if (connected) {
//                Log.d("FTP connection", "Successfully uploaded file")
//            } else {
//                Log.d("FTP connection", "Failed to upload file")
//            }
//        }
//    }else{
//        Log.d("Cadenas", "Cadena no valida")
//    }
//
//
//
//    /*
//    //Antigua Funcion KOTLIN CONNECT-UPLOAD FILE
//    scope.launch(Dispatchers.IO) {
//        val ftpClient = FTPClient()
//        try {
//            ftpClient.connect(ftpHost, ftpPort)
//            ftpClient.login(ftpUser, ftpPasswd)
//            ftpClient.enterLocalPassiveMode()
//            ftpClient.setFileType(FTP.BINARY_FILE_TYPE)
//
//            if (!FTPReply.isPositiveCompletion(ftpClient.replyCode)) {
//                Log.e("FTP", "Failed to connect to FTP server")
//                return@launch
//            }
//
//            Log.d("FTP", "Connected to FTP server")
//
//            val inputStream = FileInputStream(file)
//            val remoteFileName = file.name
//
//            val done = ftpClient.storeFile(remoteFileName, inputStream)
//            inputStream.close()
//
//            if (done) {
//                Log.d("FTP", "File uploaded successfully")
//            } else {
//                Log.e("FTP", "Failed to upload file")
//            }
//
//            ftpClient.logout()
//        } catch (ex: IOException) {
//            Log.e("FTP", "Error: ${ex.message}", ex)
//        } finally {
//            if (ftpClient.isConnected) {
//                try {
//                    ftpClient.disconnect()
//                } catch (ex: IOException) {
//                    Log.e("FTP", "Error disconnecting: ${ex.message}", ex)
//                }
//            }
//        }
//    }*/
//}
//
//private fun sendMessage(message: String, scope: CoroutineScope, stringQr: String) {
//    val regularExpression = "ftp://(\\S+):(\\S+)@(.*):(\\d+)".toRegex()
//    val matchResult = regularExpression.find(stringQr)
//
//    if (matchResult != null) {
//        val ftpHost = matchResult.groupValues[1]
//        val ftpPort = matchResult.groupValues[2].toInt()
//        val ftpUser = matchResult.groupValues[3]
//        val ftpPasswd = matchResult.groupValues[4]
//
//        scope.launch {
//            var ftpClient: FTPClient? = null
//            try {
//                ftpClient = FTPClient()
//                ftpClient.connect(ftpHost, ftpPort)
//                val loginSuccess = ftpClient.login(ftpUser, ftpPasswd)
//
//                if (!loginSuccess) {
//                    Log.d("FTP", "Failed to login to FTP server")
//                    return@launch
//                }
//
//                ftpClient.enterLocalPassiveMode()
//                ftpClient.setFileType(FTP.BINARY_FILE_TYPE)
//
//                if (!FTPReply.isPositiveCompletion(ftpClient.replyCode)) {
//                    Log.d("FTP", "Failed to connect to FTP server")
//                    return@launch
//                }
//
//                // Send message as a file with a temporary name
//                val temporaryFileName = "temp_message.txt"
//                val inputStream = message.byteInputStream()
//                val done = ftpClient.storeFile(temporaryFileName, inputStream)
//                inputStream.close()
//
//                if (done) {
//                    Log.d("FTP", "Message uploaded successfully")
//                } else {
//                    Log.d("FTP", "Failed to upload message")
//                }
//
//                ftpClient.logout()
//            } catch (ex: IOException) {
//                Log.d("FTP", "Error: ${ex.message}", ex)
//            } finally {
//                ftpClient?.disconnect()
//            }
//        }
//    }
//}
//
//
//@Preview(showBackground = true)
//@Composable
//fun MainScreenPreview() {
//    val navController = rememberNavController()
//    MainUI().MainScreen(navController, "amogus")
//}
//
