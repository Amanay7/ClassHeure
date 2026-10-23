package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.example.data.DocumentCategoryEntity
import com.example.data.DocumentEntity
import com.example.ui.AppViewModel
import com.example.ui.components.FineSearchBar
import com.example.ui.components.GlassmorphicBackground
import com.example.ui.components.GlassmorphicCard
import com.example.util.Tr
import com.example.util.tr
import com.example.util.AdaptiveScreenContainer
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentsScreen(viewModel: AppViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val categories by viewModel.allCategories.collectAsState()
    val allDocuments by viewModel.allDocuments.collectAsState()
    val isAr = Tr.isArabic()

    LaunchedEffect(Unit) {
        viewModel.seedRegulatoryDocs(context)
    }

    val onOpenDocument: (DocumentEntity) -> Unit = { doc ->
        openFile(context, doc.fileUri)
    }

    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    var presetParentCategoryId by remember { mutableStateOf<Int?>(null) }

    // Launcher to import an entire folder tree from device as a root folder
    val importRootTreeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri: Uri? ->
        treeUri?.let { uri ->
            val docTree = DocumentFile.fromTreeUri(context, uri)
            if (docTree != null) {
                coroutineScope.launch(Dispatchers.IO) {
                    val (fCount, dCount) = importDirectoryTree(context, null, docTree, viewModel)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, if (isAr) "تم استيراد المجلد : $dCount ملف في $fCount مجلد !" else "Dossier importé : $dCount fichier(s) dans $fCount dossier(s) !", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    // Deletion confirmation states
    var categoryToDelete by remember { mutableStateOf<DocumentCategoryEntity?>(null) }
    var documentToDelete by remember { mutableStateOf<DocumentEntity?>(null) }

    // Renaming states
    var categoryToRename by remember { mutableStateOf<DocumentCategoryEntity?>(null) }
    var documentToRename by remember { mutableStateOf<DocumentEntity?>(null) }

    // State for the folder detail modal/window
    var selectedMainFolderForDetail by remember { mutableStateOf<DocumentCategoryEntity?>(null) }

    // Filter main (root) folders
    val mainFolders = remember(categories) {
        categories.filter { it.parentCategoryId == null }
    }

    // Compute document counts for main folders (including their subfolders)
    val mainFolderCounts = remember(categories, allDocuments) {
        categories.filter { it.parentCategoryId == null }.associate { mainCat ->
            val subCatIds = categories.filter { it.parentCategoryId == mainCat.id }.map { it.id }
            val directDocs = allDocuments.count { it.categoryId == mainCat.id }
            val subDocs = allDocuments.count { it.categoryId in subCatIds }
            mainCat.id to (directDocs + subDocs)
        }
    }

    // Compute subfolder counts for main folders
    val subfolderCounts = remember(categories) {
        categories.filter { it.parentCategoryId == null }.associate { mainCat ->
            mainCat.id to categories.count { it.parentCategoryId == mainCat.id }
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    val filteredMainFolders = remember(mainFolders, categories, allDocuments, searchQuery, isAr) {
        if (searchQuery.isBlank()) mainFolders
        else {
            mainFolders.filter { mainCat ->
                // 1. Le dossier principal lui-même
                val locMain = getLocalizedCategoryName(mainCat.name, isAr)
                val nameMatches = mainCat.name.contains(searchQuery, ignoreCase = true) || locMain.contains(searchQuery, ignoreCase = true)
                // 2. Un de ses sous-dossiers
                val subCatIds = categories.filter { it.parentCategoryId == mainCat.id }.map { it.id }
                val subCatMatches = categories.any {
                    it.parentCategoryId == mainCat.id && (
                        it.name.contains(searchQuery, ignoreCase = true) ||
                        getLocalizedCategoryName(it.name, isAr).contains(searchQuery, ignoreCase = true)
                    )
                }
                // 3. Un document contenu directement ou dans un de ses sous-dossiers
                val docMatches = allDocuments.any { doc ->
                    (doc.categoryId == mainCat.id || doc.categoryId in subCatIds) &&
                    (doc.title.contains(searchQuery, ignoreCase = true) || doc.fileUri.contains(searchQuery, ignoreCase = true))
                }
                nameMatches || subCatMatches || docMatches
            }
        }
    }

    // Direct document search results across all documents
    val matchingDocuments = remember(allDocuments, searchQuery) {
        if (searchQuery.isBlank()) emptyList()
        else {
            allDocuments.filter { doc ->
                doc.title.contains(searchQuery, ignoreCase = true) ||
                doc.fileUri.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(containerColor = Color.Transparent) { innerPadding ->
        AdaptiveScreenContainer(maxWidth = 960.dp) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            // Header Section : Action Buttons (Importer & Nouveau dossier)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { importRootTreeLauncher.launch(null) },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DriveFolderUpload,
                            contentDescription = if (isAr) "استيراد مجلد" else "Importer dossier",
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (isAr) "استيراد مجلد" else "Importer dossier",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    FilledTonalButton(
                        onClick = {
                            newCategoryName = ""
                            presetParentCategoryId = null
                            showAddCategoryDialog = true
                        },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .testTag("add_folder_header_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CreateNewFolder,
                            contentDescription = if (isAr) "مجلد جديد" else "Nouveau dossier",
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (isAr) "مجلد جديد" else "Nouveau dossier",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Fine Search Bar
            item {
                FineSearchBar(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = if (isAr) "البحث عن مجلد أو محتواه..." else "Rechercher un dossier ou son contenu...",
                    testTag = "search_documents_field",
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (mainFolders.isEmpty()) {
                // Empty state to initialize categories
                item {
                    GlassmorphicCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        shape = RoundedCornerShape(24.dp),
                        elevation = 2.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = "Folder Empty",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(64.dp)
                            )
                            Text(
                                text = if (isAr) "لا توجد مجلدات حالياً" else "Aucun dossier créé",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = if (isAr) "قم بإنشاء مجلداتك الخاصة أو ابدأ بالمجلدات التربوية المقترحة." else "Initialisez les dossiers pédagogiques par défaut ou créez vos propres dossiers principaux.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = {
                                        viewModel.addCategory(if (isAr) "التدرجات السنوية" else "Progressions annuelles")
                                        viewModel.addCategory(if (isAr) "مذكرات الدروس" else "Fiches de cours")
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Text(if (isAr) "تهيئة أولية" else "Initialiser")
                                }
                                OutlinedButton(
                                    onClick = {
                                        newCategoryName = ""
                                        presetParentCategoryId = null
                                        showAddCategoryDialog = true
                                    }
                                ) {
                                    Text(if (isAr) "إنشاء مجلد" else "Créer dossier")
                                }
                            }
                        }
                    }
                }
            } else if (filteredMainFolders.isEmpty() && matchingDocuments.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "No results",
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (isAr) "لا توجد نتائج مطابقة لـ « $searchQuery »." else "Aucun dossier ou document ne correspond à « $searchQuery ».",
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            } else {
                // Dossiers section
                if (filteredMainFolders.isNotEmpty()) {
                    if (searchQuery.isNotBlank()) {
                        item {
                            Text(
                                text = "Dossiers (${filteredMainFolders.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                            )
                        }
                    }
                    items(filteredMainFolders, key = { it.id }) { folder ->
                        val subfolderCount = subfolderCounts[folder.id] ?: 0
                        val docCount = mainFolderCounts[folder.id] ?: 0

                        MainFolderCard(
                            folder = folder,
                            subfolderCount = subfolderCount,
                            docCount = docCount,
                            onClick = { selectedMainFolderForDetail = folder },
                            onRename = { categoryToRename = folder },
                            onDelete = { categoryToDelete = folder }
                        )
                    }
                }

                // Documents section when searching
                if (searchQuery.isNotBlank() && matchingDocuments.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isAr) "الوثائق والملفات (${matchingDocuments.size})" else "Documents & Fichiers (${matchingDocuments.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                        )
                    }
                    items(matchingDocuments, key = { "search_doc_${it.id}" }) { doc ->
                        val docCat = categories.find { it.id == doc.categoryId }
                        val parentCat = if (docCat?.parentCategoryId != null) categories.find { it.id == docCat.parentCategoryId } else null
                        val path = when {
                            parentCat != null && docCat != null -> "${getLocalizedCategoryName(parentCat.name, isAr)} / ${getLocalizedCategoryName(docCat.name, isAr)}"
                            docCat != null -> getLocalizedCategoryName(docCat.name, isAr)
                            else -> if (isAr) "عام" else "Général"
                        }
                        val rootFolder = parentCat ?: docCat

                        SearchDocumentResultCard(
                            document = doc,
                            categoryPath = path,
                            onOpen = { onOpenDocument(doc) },
                            onNavigateToFolder = {
                                if (rootFolder != null) {
                                    selectedMainFolderForDetail = rootFolder
                                }
                            },
                            onRename = { documentToRename = doc },
                            onDelete = { documentToDelete = doc }
                        )
                    }
                }
            }
        }
    }
}

    // --- FOLDER DETAIL MODAL / WINDOW (Full screen Dialog) ---
    if (selectedMainFolderForDetail != null) {
        FolderDetailWindow(
            mainFolder = selectedMainFolderForDetail!!,
            categories = categories,
            allDocuments = allDocuments,
            viewModel = viewModel,
            onDismiss = { selectedMainFolderForDetail = null },
            onDeleteDocumentRequest = { doc -> documentToDelete = doc },
            onDeleteCategoryRequest = { cat -> categoryToDelete = cat },
            onRenameDocumentRequest = { doc -> documentToRename = doc },
            onRenameCategoryRequest = { cat -> categoryToRename = cat }
        )
    }

    // --- ADD CATEGORY DIALOG (Unified with Parent Selection Dropdown) ---
    if (showAddCategoryDialog) {
        var selectedParentId by remember { mutableStateOf(presetParentCategoryId) }
        var dropdownExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CreateNewFolder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (selectedParentId != null) (if (isAr) "مجلد فرعي جديد" else "Nouveau sous-dossier") else (if (isAr) "مجلد جديد" else "Nouveau dossier"),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        label = { Text(if (isAr) "اسم المجلد" else "Nom de la catégorie") },
                        placeholder = { Text(if (isAr) "مثال: الفروض، التدرج 2026..." else "Ex: Devoirs, Progression 2026...") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("category_name_input")
                    )

                    // Parent selection dropdown card
                    Column {
                        Text(
                            text = if (isAr) "المجلد الرئيسي (المكان)" else "Dossier d'emplacement (Parent)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Box(modifier = Modifier.fillMaxWidth()) {
                            val parentCats = categories.filter { it.parentCategoryId == null }
                            val parentName = if (selectedParentId == null) {
                                if (isAr) "بدون (مجلد رئيسي)" else "Aucun (Dossier principal)"
                            } else {
                                parentCats.find { it.id == selectedParentId }?.let { getLocalizedCategoryName(it.name, isAr) } ?: (if (isAr) "مجلد رئيسي" else "Dossier principal")
                            }

                            OutlinedCard(
                                onClick = { dropdownExpanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (selectedParentId == null) Icons.Default.Folder else Icons.Default.SubdirectoryArrowRight,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(parentName, style = MaterialTheme.typography.bodyMedium)
                                    }
                                    Icon(
                                        imageVector = if (dropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                        contentDescription = null
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = dropdownExpanded,
                                onDismissRequest = { dropdownExpanded = false },
                                modifier = Modifier.fillMaxWidth(0.85f)
                            ) {
                                DropdownMenuItem(
                                    text = { Text(if (isAr) "بدون (مجلد رئيسي)" else "Aucun (Dossier principal)", style = MaterialTheme.typography.bodyMedium) },
                                    leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                    onClick = {
                                        selectedParentId = null
                                        dropdownExpanded = false
                                    }
                                )
                                parentCats.forEach { parent ->
                                    DropdownMenuItem(
                                        text = { Text(getLocalizedCategoryName(parent.name, isAr), style = MaterialTheme.typography.bodyMedium) },
                                        leadingIcon = { Icon(Icons.Default.SubdirectoryArrowRight, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                        onClick = {
                                            selectedParentId = parent.id
                                            dropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCategoryName.isNotBlank()) {
                            viewModel.addCategory(newCategoryName.trim(), selectedParentId)
                            showAddCategoryDialog = false
                        }
                    },
                    modifier = Modifier.testTag("confirm_create_category_button")
                ) {
                    Text(if (isAr) "إنشاء" else "Créer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) {
                    Text(if (isAr) "إلغاء" else "Annuler")
                }
            }
        )
    }

    // --- CONFIRM DELETE CATEGORY DIALOG ---
    if (categoryToDelete != null) {
        val cat = categoryToDelete!!
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = { Text(if (isAr) "حذف المجلد ؟" else "Supprimer le dossier ?", fontWeight = FontWeight.Bold) },
            text = {
                val locCatName = getLocalizedCategoryName(cat.name, isAr)
                Text(if (isAr) "هل تريد حقاً حذف المجلد \"$locCatName\" ؟ سيتم حذف جميع الوثائق والمجلدات الفرعية داخله نهائياً." else "Voulez-vous vraiment supprimer le dossier \"$locCatName\" ? Tous les documents et sous-dossiers qu'il contient seront définitivement effacés de votre appareil.")
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        viewModel.deleteCategory(cat)
                        if (selectedMainFolderForDetail?.id == cat.id) {
                            selectedMainFolderForDetail = null
                        }
                        categoryToDelete = null
                    }
                ) {
                    Text(if (isAr) "حذف" else "Supprimer")
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) {
                    Text(if (isAr) "إلغاء" else "Annuler")
                }
            }
        )
    }

    // --- CONFIRM DELETE DOCUMENT DIALOG ---
    if (documentToDelete != null) {
        val doc = documentToDelete!!
        val docCat = categories.find { it.id == doc.categoryId }
        if (isProtectedRegulatoryDoc(doc) || (docCat != null && isProtectedCategory(docCat))) {
            Toast.makeText(context, if (isAr) "هذا النص التشريعي الرسمي محمي ضد الحذف." else "Ce texte réglementaire officiel est protégé contre la suppression.", Toast.LENGTH_SHORT).show()
            documentToDelete = null
        } else {
            AlertDialog(
                onDismissRequest = { documentToDelete = null },
                title = { Text(if (isAr) "حذف الوثيقة ؟" else "Supprimer le document ?", fontWeight = FontWeight.Bold) },
                text = {
                    Text(if (isAr) "هل تريد بالتأكيد حذف الملف \"${doc.title}\" ؟ سيتم إزالته من الذاكرة." else "Voulez-vous vraiment supprimer le document \"${doc.title}\" ? Le fichier sera effacé de votre stockage.")
                },
                confirmButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        onClick = {
                            viewModel.deleteDocument(doc)
                            documentToDelete = null
                        }
                    ) {
                        Text(if (isAr) "حذف" else "Supprimer")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { documentToDelete = null }) {
                        Text(if (isAr) "إلغاء" else "Annuler")
                    }
                }
            )
        }
    }

    // --- DIALOGUE RENOMMER UN DOSSIER ---
    if (categoryToRename != null) {
        val cat = categoryToRename!!
        var renameText by remember(cat) { mutableStateOf(getLocalizedCategoryName(cat.name, isAr)) }
        AlertDialog(
            onDismissRequest = { categoryToRename = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = { Text(if (isAr) "إعادة تسمية المجلد" else "Renommer le dossier", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (isAr) "أدخل الاسم الجديد للمجلد :" else "Entrez le nouveau nom pour ce dossier :", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = renameText,
                        onValueChange = { renameText = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(if (isAr) "اسم المجلد" else "Nom du dossier") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameText.isNotBlank()) {
                            viewModel.renameCategory(cat, renameText)
                            if (selectedMainFolderForDetail?.id == cat.id) {
                                selectedMainFolderForDetail = cat.copy(name = renameText.trim())
                            }
                            categoryToRename = null
                        }
                    },
                    enabled = renameText.isNotBlank() && renameText.trim() != cat.name
                ) {
                    Text(if (isAr) "تغيير الاسم" else "Renommer")
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToRename = null }) {
                    Text(if (isAr) "إلغاء" else "Annuler")
                }
            }
        )
    }

    // --- DIALOGUE RENOMMER UN DOCUMENT ---
    if (documentToRename != null) {
        val doc = documentToRename!!
        var renameDocText by remember(doc) { mutableStateOf(doc.title) }
        AlertDialog(
            onDismissRequest = { documentToRename = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = { Text(if (isAr) "إعادة تسمية الملف" else "Renommer le fichier", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (isAr) "أدخل العنوان أو الاسم الجديد للملف :" else "Entrez le nouveau titre ou nom du fichier :", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = renameDocText,
                        onValueChange = { renameDocText = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(if (isAr) "عنوان الوثيقة" else "Nom ou titre du document") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameDocText.isNotBlank()) {
                            viewModel.renameDocument(doc, renameDocText)
                            documentToRename = null
                        }
                    },
                    enabled = renameDocText.isNotBlank() && renameDocText.trim() != doc.title
                ) {
                    Text(if (isAr) "تغيير الاسم" else "Renommer")
                }
            },
            dismissButton = {
                TextButton(onClick = { documentToRename = null }) {
                    Text(if (isAr) "إلغاء" else "Annuler")
                }
            }
        )
    }
}

@Composable
fun MainFolderCard(
    folder: DocumentCategoryEntity,
    subfolderCount: Int,
    docCount: Int,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val isAr = Tr.isArabic()
    GlassmorphicCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isProtected = isProtectedCategory(folder)
            val folderGradient = if (isProtected) {
                listOf(Color(0xFFD97706), Color(0xFFF59E0B))
            } else {
                listOf(Color(0xFF2563EB), Color(0xFF3B82F6))
            }

            // Folder Visual Icon (Artistic jewel gradient)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .shadow(3.dp, RoundedCornerShape(14.dp))
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(folderGradient)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isProtected) Icons.Default.FolderSpecial else Icons.Default.Folder,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(25.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Text Metadata
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = getLocalizedCategoryName(folder.name, isAr),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 15.sp,
                        lineHeight = 18.sp
                    ),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val badgeColor = if (isProtected) Color(0xFFD97706) else Color(0xFF2563EB)
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = badgeColor.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(0.7.dp, badgeColor.copy(alpha = 0.35f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = badgeColor,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(Modifier.width(3.dp))
                            val pdfText = if (docCount <= 1) "$docCount ${if (isAr) "ملف" else "fichier"}" else "$docCount ${if (isAr) "ملفات" else "fichiers"}"
                            Text(
                                text = pdfText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = badgeColor
                            )
                        }
                    }
                    if (subfolderCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = if (subfolderCount <= 1) "$subfolderCount ${if (isAr) "مجلد فرعي" else "sous-dossier"}" else "$subfolderCount ${if (isAr) "مجلدات فرعية" else "sous-dossiers"}",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Bouton Renommer le dossier
            IconButton(
                onClick = onRename,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Renommer le dossier",
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                    modifier = Modifier.size(18.dp)
                )
            }

            // Bouton Supprimer ou Verrouillé si protégé
            if (isProtectedCategory(folder)) {
                IconButton(
                    onClick = {
                        Toast.makeText(context, if (isAr) "مجلد 'النصوص التشريعية والتنظيمية' محمي ضد الحذف." else "Le dossier 'Textes réglementaires' est protégé contre la suppression.", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Dossier réglementaire protégé",
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Supprimer le dossier",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(2.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Ouvrir",
                tint = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun SearchDocumentResultCard(
    document: DocumentEntity,
    categoryPath: String,
    onOpen: () -> Unit,
    onNavigateToFolder: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val isAr = Tr.isArabic()
    val formattedDate = remember(document.createdAt) {
        try {
            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            sdf.format(Date(document.createdAt))
        } catch (e: Exception) {
            ""
        }
    }
    val fileInfo = remember(document.fileUri) { getFileTypeInfo(document.fileUri) }

    GlassmorphicCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() },
        shape = RoundedCornerShape(14.dp),
        elevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(fileInfo.containerColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = fileInfo.icon,
                    contentDescription = fileInfo.label,
                    tint = fileInfo.color,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = document.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = categoryPath,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (formattedDate.isNotBlank()) {
                        Text(
                            text = "• $formattedDate",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            IconButton(
                onClick = onNavigateToFolder,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = "Ouvrir dossier",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Bouton Renommer
            IconButton(
                onClick = onRename,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Renommer le fichier",
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                    modifier = Modifier.size(18.dp)
                )
            }

            // Bouton Supprimer ou Verrouillé si réglementaire
            if (isProtectedRegulatoryDoc(document)) {
                IconButton(
                    onClick = {
                        Toast.makeText(context, if (isAr) "هذا النص التشريعي الرسمي محمي ضد الحذف." else "Ce texte réglementaire officiel est protégé contre la suppression.", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Document officiel protégé",
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Supprimer",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderDetailWindow(
    mainFolder: DocumentCategoryEntity,
    categories: List<DocumentCategoryEntity>,
    allDocuments: List<DocumentEntity>,
    viewModel: AppViewModel,
    onDismiss: () -> Unit,
    onDeleteDocumentRequest: (DocumentEntity) -> Unit,
    onDeleteCategoryRequest: (DocumentCategoryEntity) -> Unit,
    onRenameDocumentRequest: (DocumentEntity) -> Unit,
    onRenameCategoryRequest: (DocumentCategoryEntity) -> Unit
) {
    val context = LocalContext.current
    val isAr = Tr.isArabic()

    // We use a list to keep track of folder ID navigation history (stack)
    val folderStack = remember { mutableStateListOf<Int>(mainFolder.id) }
    val currentFolderId = folderStack.last()

    // Resolve active folder entity dynamically
    val activeFolder = remember(categories, currentFolderId, mainFolder) {
        if (currentFolderId == mainFolder.id) mainFolder
        else categories.find { it.id == currentFolderId } ?: mainFolder
    }

    // Subcategories (subfolders) inside the current active folder
    val subfolders = remember(categories, currentFolderId) {
        categories.filter { it.parentCategoryId == currentFolderId }
    }

    // Filter documents of the current active folder
    val activeDocuments = remember(allDocuments, currentFolderId) {
        allDocuments.filter { it.categoryId == currentFolderId }
    }

    // Floating dialog states
    var showAddSubfolderDialog by remember { mutableStateOf(false) }
    var newSubfolderName by remember { mutableStateOf("") }

    var showAddFileDialog by remember { mutableStateOf(false) }
    var newFileTitle by remember { mutableStateOf("") }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf("") }

    // File picker launcher supporting single file
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedFileUri = it
            val fileName = getFileNameFromUri(context, it) ?: "document"
            selectedFileName = fileName
            if (newFileTitle.isBlank()) {
                newFileTitle = if (fileName.contains('.')) fileName.substringBeforeLast('.') else fileName
            }
        }
    }

    val coroutineScope = rememberCoroutineScope()

    // Multi-file picker launcher allowing multiple files to be imported at once
    val multipleFilesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            coroutineScope.launch(Dispatchers.IO) {
                var count = 0
                for (u in uris) {
                    val name = getFileNameFromUri(context, u) ?: "document"
                    val title = if (name.contains('.')) name.substringBeforeLast('.') else name
                    val savedPath = copyUriToInternalStorage(context, u, name)
                    if (savedPath != null) {
                        viewModel.addDocumentDirect(currentFolderId, title, savedPath)
                        count++
                    }
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, if (isAr) "تم استيراد $count ملف(ات) بنجاح !" else "$count fichier(s) importé(s) avec succès !", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Subfolder tree picker launcher
    val subfolderTreeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri: Uri? ->
        treeUri?.let { uri ->
            val docTree = DocumentFile.fromTreeUri(context, uri)
            if (docTree != null) {
                coroutineScope.launch(Dispatchers.IO) {
                    val (fCount, dCount) = importDirectoryTree(context, currentFolderId, docTree, viewModel)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, if (isAr) "تم استيراد المجلد : $dCount ملف(ات) في $fCount مجلد(ات) !" else "Dossier importé : $dCount fichier(s) dans $fCount dossier(s) !", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    // Full Screen Dialog
    Dialog(
        onDismissRequest = {
            onDismiss()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = true
        )
    ) {
        // Geste retour : remonte l'arborescence s'il y a un parent, sinon ferme la fenêtre
        BackHandler {
            if (folderStack.size > 1) {
                folderStack.removeAt(folderStack.size - 1)
            } else {
                onDismiss()
            }
        }

        GlassmorphicBackground {
            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                        tonalElevation = 2.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            // 1. Full Folder Title Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = {
                                    if (folderStack.size > 1) {
                                        folderStack.removeAt(folderStack.size - 1)
                                    } else {
                                        onDismiss()
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = if (folderStack.size > 1) (if (isAr) "رجوع للمجلد السابق" else "Dossier parent") else (if (isAr) "إغلاق" else "Fermer")
                                    )
                                }

                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 6.dp)
                                ) {
                                    Text(
                                        text = getLocalizedCategoryName(activeFolder.name, isAr),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val subtitle = if (currentFolderId == mainFolder.id) {
                                        if (isAr) "الملف البيداغوجي الرئيسي" else "Dossier Pédagogique Principal"
                                    } else {
                                        val parentName = getLocalizedCategoryName(mainFolder.name, isAr)
                                        if (isAr) "في : $parentName" else "Dans : $parentName"
                                    }
                                    Text(
                                        text = subtitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }

                                if (folderStack.size > 1) {
                                    IconButton(
                                        onClick = {
                                            while (folderStack.size > 1) {
                                                folderStack.removeAt(folderStack.size - 1)
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Home,
                                            contentDescription = if (isAr) "المجلد الرئيسي" else "Dossier principal",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            // 2. Action Icons Row (Placed cleanly BELOW the folder title)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 2.dp, bottom = 4.dp, start = 4.dp, end = 4.dp),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Single file dialog action
                                IconButton(
                                    onClick = {
                                        newFileTitle = ""
                                        selectedFileUri = null
                                        selectedFileName = ""
                                        showAddFileDialog = true
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = if (isAr) "إضافة ملف" else "Ajouter un fichier",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                // Import multiple files at once
                                IconButton(
                                    onClick = { multipleFilesLauncher.launch("*/*") }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.UploadFile,
                                        contentDescription = if (isAr) "استيراد عدة ملفات" else "Importer plusieurs fichiers",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                // Import complete folder tree
                                IconButton(
                                    onClick = { subfolderTreeLauncher.launch(null) }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DriveFolderUpload,
                                        contentDescription = if (isAr) "استيراد مجلد فرعي" else "Importer un sous-dossier",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                // Rename current folder action
                                IconButton(
                                    onClick = { onRenameCategoryRequest(activeFolder) }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = if (isAr) "إعادة تسمية هذا المجلد" else "Renommer ce dossier",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                // Create subfolder action
                                IconButton(
                                    onClick = {
                                        newSubfolderName = ""
                                        showAddSubfolderDialog = true
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CreateNewFolder,
                                        contentDescription = if (isAr) "مجلد فرعي جديد" else "Nouveau sous-dossier",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            ) { paddingValues ->
                var detailSearchQuery by remember { mutableStateOf("") }
                val filteredSubfolders = remember(subfolders, allDocuments, detailSearchQuery) {
                    if (detailSearchQuery.isBlank()) subfolders
                    else subfolders.filter { sub ->
                        val nameMatches = sub.name.contains(detailSearchQuery, ignoreCase = true) ||
                                          getLocalizedCategoryName(sub.name, isAr).contains(detailSearchQuery, ignoreCase = true)
                        val docMatches = allDocuments.any { doc ->
                            doc.categoryId == sub.id && (
                                doc.title.contains(detailSearchQuery, ignoreCase = true) ||
                                doc.fileUri.contains(detailSearchQuery, ignoreCase = true)
                            )
                        }
                        nameMatches || docMatches
                    }
                }
                val filteredActiveDocuments = remember(activeDocuments, detailSearchQuery) {
                    if (detailSearchQuery.isBlank()) activeDocuments
                    else activeDocuments.filter {
                        it.title.contains(detailSearchQuery, ignoreCase = true) ||
                        it.fileUri.contains(detailSearchQuery, ignoreCase = true)
                    }
                }
                // Documents matching in nested subfolders of this folder
                val nestedMatchingDocs = remember(subfolders, allDocuments, detailSearchQuery) {
                    if (detailSearchQuery.isBlank()) emptyList()
                    else {
                        val subIds = subfolders.map { it.id }
                        allDocuments.filter { doc ->
                            doc.categoryId in subIds && (
                                doc.title.contains(detailSearchQuery, ignoreCase = true) ||
                                doc.fileUri.contains(detailSearchQuery, ignoreCase = true)
                            )
                        }
                    }
                }

                AdaptiveScreenContainer(maxWidth = 960.dp) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 84.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                    // Fil d'Ariane interactif et retour au dossier principal si on est dans un sous-dossier
                    if (folderStack.size > 1) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    LazyRow(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        itemsIndexed(folderStack) { index, fId ->
                                            val fEntity = if (fId == mainFolder.id) mainFolder else categories.find { it.id == fId }
                                            val fName = fEntity?.let { getLocalizedCategoryName(it.name, isAr) } ?: (if (isAr) "مجلد" else "Dossier")
                                            val isCurrent = index == folderStack.size - 1

                                            if (index > 0) {
                                                Icon(
                                                    imageVector = if (isAr) Icons.AutoMirrored.Filled.ArrowBack else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.outline,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (isCurrent) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                modifier = Modifier.clickable(enabled = !isCurrent) {
                                                    while (folderStack.size > index + 1) {
                                                        folderStack.removeAt(folderStack.size - 1)
                                                    }
                                                }
                                            ) {
                                                Text(
                                                    text = fName,
                                                    fontSize = 11.5.sp,
                                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isCurrent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    TextButton(
                                        onClick = {
                                            while (folderStack.size > 1) {
                                                folderStack.removeAt(folderStack.size - 1)
                                            }
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Home,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(Modifier.width(3.dp))
                                        Text(
                                            text = if (isAr) "الرئيسي" else "Principal",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (subfolders.isNotEmpty() || activeDocuments.isNotEmpty() || nestedMatchingDocs.isNotEmpty()) {
                        item {
                            FineSearchBar(
                                value = detailSearchQuery,
                                onValueChange = { detailSearchQuery = it },
                                placeholder = if (isAr) "البحث عن مجلد أو محتواه..." else "Rechercher un dossier ou son contenu...",
                                testTag = "search_folder_detail_field",
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    if (subfolders.isEmpty() && activeDocuments.isEmpty() && nestedMatchingDocs.isEmpty()) {
                        // Fully Empty State
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp)
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(16.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.padding(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FolderOpen,
                                        contentDescription = "Dossier vide",
                                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                                        modifier = Modifier.size(56.dp)
                                    )
                                    Text(
                                        text = if (isAr) "هذا المجلد فارغ" else "Ce dossier est vide",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = if (isAr) "أنشئ مجلداً فرعياً جديداً أو استورد ملفات مباشرة من الأزرار أعلاه." else "Créez un nouveau sous-dossier ou importez des fichiers directement depuis les boutons en haut.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else if (filteredSubfolders.isEmpty() && filteredActiveDocuments.isEmpty() && nestedMatchingDocs.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "No results",
                                        tint = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = if (isAr) "لم يتم العثور على أي ملف أو مجلد فرعي لـ « $detailSearchQuery »." else "Aucun fichier ou sous-dossier trouvé pour « $detailSearchQuery ».",
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                    } else {
                        // Subfolders section
                        if (filteredSubfolders.isNotEmpty()) {
                            item {
                                Text(
                                    text = if (isAr) "المجلدات الفرعية (${filteredSubfolders.size})" else "Sous-dossiers (${filteredSubfolders.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                            items(filteredSubfolders, key = { it.id }) { sub ->
                                GlassmorphicCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { folderStack.add(sub.id) },
                                    shape = RoundedCornerShape(12.dp),
                                    elevation = 1.dp
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Folder,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = getLocalizedCategoryName(sub.name, isAr),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            val subDocsCount = allDocuments.count { it.categoryId == sub.id }
                                            val docsText = if (isAr) {
                                                if (subDocsCount <= 1) "$subDocsCount ملف" else "$subDocsCount ملفات"
                                            } else {
                                                if (subDocsCount <= 1) "$subDocsCount fichier" else "$subDocsCount fichiers"
                                            }
                                            Text(
                                                text = docsText,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                        // Bouton Renommer sous-dossier
                                        IconButton(
                                            onClick = { onRenameCategoryRequest(sub) },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Renommer le sous-dossier",
                                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        if (isProtectedCategory(sub)) {
                                            IconButton(
                                                onClick = {
                                                    Toast.makeText(context, if (isAr) "هذا المجلد التنظيمي محمي ضد الحذف." else "Ce dossier réglementaire est protégé contre la suppression.", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Lock,
                                                    contentDescription = "Dossier protégé",
                                                    tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        } else {
                                            IconButton(
                                                onClick = { onDeleteCategoryRequest(sub) },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DeleteOutline,
                                                    contentDescription = "Supprimer le sous-dossier",
                                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            contentDescription = "Ouvrir",
                                            tint = MaterialTheme.colorScheme.outlineVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Documents section
                        if (filteredActiveDocuments.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (isAr) "الملفات (${filteredActiveDocuments.size})" else "Fichiers (${filteredActiveDocuments.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                            items(filteredActiveDocuments, key = { it.id }) { document ->
                                val fileInfo = remember(document.fileUri) { getFileTypeInfo(document.fileUri) }
                                GlassmorphicCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { openFile(context, document.fileUri) },
                                    shape = RoundedCornerShape(12.dp),
                                    elevation = 1.dp
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(fileInfo.containerColor),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = fileInfo.icon,
                                                contentDescription = fileInfo.label,
                                                tint = fileInfo.color,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = document.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            val formattedDate = remember(document.createdAt) {
                                                try {
                                                    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                                                    sdf.format(Date(document.createdAt))
                                                } catch (e: Exception) {
                                                    ""
                                                }
                                            }
                                            Text(
                                                text = formattedDate,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                        // Bouton Renommer le document
                                        IconButton(
                                            onClick = { onRenameDocumentRequest(document) },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Renommer le document",
                                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        // Suppression verrouillée si document réglementaire protégé
                                        if (isProtectedRegulatoryDoc(document)) {
                                            IconButton(
                                                onClick = {
                                                    Toast.makeText(context, if (isAr) "هذا النص التشريعي الرسمي محمي ضد الحذف." else "Ce texte réglementaire officiel est protégé contre la suppression.", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Lock,
                                                    contentDescription = "Document réglementaire protégé",
                                                    tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        } else {
                                            IconButton(
                                                onClick = { onDeleteDocumentRequest(document) },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DeleteOutline,
                                                    contentDescription = "Supprimer le document",
                                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Documents found in subfolders when searching
                        if (detailSearchQuery.isNotBlank() && nestedMatchingDocs.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Documents trouvés dans les sous-dossiers (${nestedMatchingDocs.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                            items(nestedMatchingDocs, key = { it.id }) { document ->
                                val subCat = subfolders.find { it.id == document.categoryId }
                                val fileInfo = remember(document.fileUri) { getFileTypeInfo(document.fileUri) }
                                GlassmorphicCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { openFile(context, document.fileUri) },
                                    shape = RoundedCornerShape(12.dp),
                                    elevation = 1.dp
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(fileInfo.containerColor),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = fileInfo.icon,
                                                contentDescription = fileInfo.label,
                                                tint = fileInfo.color,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = document.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Folder,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Text(
                                                    text = subCat?.let { getLocalizedCategoryName(it.name, isAr) } ?: (if (isAr) "مجلد فرعي" else "Sous-dossier"),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                        IconButton(
                                            onClick = { folderStack.add(document.categoryId) },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.FolderOpen,
                                                contentDescription = "Aller au sous-dossier",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        // Bouton Renommer
                                        IconButton(
                                            onClick = { onRenameDocumentRequest(document) },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Renommer le document",
                                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        if (isProtectedRegulatoryDoc(document)) {
                                            IconButton(
                                                onClick = {
                                                    Toast.makeText(context, if (isAr) "هذا النص التشريعي الرسمي محمي ضد الحذف." else "Ce texte réglementaire officiel est protégé contre la suppression.", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Lock,
                                                    contentDescription = "Document officiel protégé",
                                                    tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        } else {
                                            IconButton(
                                                onClick = { onDeleteDocumentRequest(document) },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DeleteOutline,
                                                    contentDescription = "Supprimer le document",
                                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

    // --- ADD SUBFOLDER DIALOG ---
    if (showAddSubfolderDialog) {
        AlertDialog(
            onDismissRequest = { showAddSubfolderDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CreateNewFolder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isAr) "مجلد فرعي جديد" else "Nouveau sous-dossier", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    val localizedActiveName = getLocalizedCategoryName(activeFolder.name, isAr)
                    Text(
                        text = if (isAr) "إنشاء في : $localizedActiveName" else "Création dans : $localizedActiveName",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    OutlinedTextField(
                        value = newSubfolderName,
                        onValueChange = { newSubfolderName = it },
                        label = { Text(if (isAr) "اسم المجلد الفرعي" else "Nom du sous-dossier") },
                        placeholder = { Text(if (isAr) "مثال: فروض 2026، بحوث..." else "Ex: Devoirs 2026, Fiches TPs...") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("subfolder_name_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newSubfolderName.isNotBlank()) {
                            viewModel.addCategory(newSubfolderName.trim(), currentFolderId)
                            showAddSubfolderDialog = false
                        }
                    },
                    modifier = Modifier.testTag("confirm_create_subfolder_button")
                ) {
                    Text(if (isAr) "إنشاء" else "Créer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSubfolderDialog = false }) {
                    Text(if (isAr) "إلغاء" else "Annuler")
                }
            }
        )
    }

    // --- ADD FILE DOCUMENT DIALOG ---
    if (showAddFileDialog) {
        AlertDialog(
            onDismissRequest = { showAddFileDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.UploadFile,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isAr) "استيراد ملف" else "Importer un fichier", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            val targetFolderName = getLocalizedCategoryName(activeFolder.name, isAr)
                            Text(
                                text = if (isAr) "المجلد المستهدف : $targetFolderName" else "Dossier cible : $targetFolderName",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    OutlinedTextField(
                        value = newFileTitle,
                        onValueChange = { newFileTitle = it },
                        label = { Text(if (isAr) "عنوان الوثيقة" else "Titre du document") },
                        placeholder = { Text(if (isAr) "مثال: بطاقة درس الفصل 1..." else "Ex: Fiche cours Chapitre 1...") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_pdf_title_input")
                    )

                    // Clickable Drag & Drop upload style tactile card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (selectedFileName.isNotBlank()) MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                            )
                            .border(
                                width = 1.dp,
                                color = if (selectedFileName.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { filePickerLauncher.launch("*/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(16.dp)
                        ) {
                            if (selectedFileName.isNotBlank()) {
                                val fileInfo = remember(selectedFileName) { getFileTypeInfo(selectedFileName) }
                                Icon(
                                    imageVector = fileInfo.icon,
                                    contentDescription = "Attaché",
                                    tint = fileInfo.color,
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    text = selectedFileName,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = if (isAr) "اضغط لتغيير الملف" else "Cliquez pour changer le fichier",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = "Importer",
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    text = if (isAr) "اختيار ملف" else "Sélectionner un fichier",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isAr) "جميع الصيغ مقبولة (PDF، Word، Excel، صور، إلخ)" else "Tous formats acceptés (PDF, Word, Excel, Image, etc.)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val uri = selectedFileUri
                        if (newFileTitle.isNotBlank() && uri != null) {
                            val savedPath = copyUriToInternalStorage(context, uri, selectedFileName)
                            if (savedPath != null) {
                                viewModel.addDocument(currentFolderId, newFileTitle.trim(), savedPath)
                                showAddFileDialog = false
                            } else {
                                Toast.makeText(context, if (isAr) "فشل حفظ الملف." else "Échec de l'enregistrement du fichier.", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, if (isAr) "يرجى إدخال العنوان واختيار ملف." else "Veuillez remplir le titre et sélectionner un fichier.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.testTag("confirm_add_pdf_button")
                ) {
                    Text(if (isAr) "استيراد" else "Importer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddFileDialog = false }) {
                    Text(if (isAr) "إلغاء" else "Annuler")
                }
            }
        )
    }
}

@Composable
fun DocumentCard(
    document: DocumentEntity,
    onOpen: () -> Unit,
    onRename: () -> Unit = {},
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val isAr = Tr.isArabic()
    val formattedDate = remember(document.createdAt) {
        try {
            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            sdf.format(Date(document.createdAt))
        } catch (e: Exception) {
            ""
        }
    }
    val fileInfo = remember(document.fileUri) { getFileTypeInfo(document.fileUri) }

    GlassmorphicCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() },
        shape = RoundedCornerShape(14.dp),
        elevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Top: File Icon and delete option
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .shadow(2.dp, RoundedCornerShape(10.dp))
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(fileInfo.color, fileInfo.color.copy(alpha = 0.80f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = fileInfo.icon,
                        contentDescription = fileInfo.label,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onRename,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Renommer",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    if (isProtectedRegulatoryDoc(document)) {
                        IconButton(
                            onClick = {
                                Toast.makeText(context, if (isAr) "هذا النص التشريعي الرسمي محمي ضد الحذف." else "Ce texte réglementaire officiel est protégé contre la suppression.", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Protégé",
                                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Supprimer",
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Title & Creation Date
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = document.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Tap action label
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Ouvrir le fichier",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// Helper: copy Uri into app's internal files folder securely preserving extension
private fun copyUriToInternalStorage(context: Context, uri: Uri, originalName: String? = null): String? {
    return try {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        if (inputStream != null) {
            val dir = File(context.filesDir, "copied_documents")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            val ext = if (!originalName.isNullOrBlank() && originalName.contains(".")) {
                originalName.substringAfterLast(".", "").lowercase()
            } else {
                val mime = context.contentResolver.getType(uri)
                if (mime != null) {
                    MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) ?: "bin"
                } else {
                    "bin"
                }
            }
            val fileName = "doc_${System.currentTimeMillis()}.$ext"
            val file = File(dir, fileName)
            val outputStream = FileOutputStream(file)
            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            file.absolutePath
        } else {
            null
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

// Helper: Déterminer si un document est un texte réglementaire officiel protégé
fun isProtectedRegulatoryDoc(document: DocumentEntity): Boolean {
    val uri = document.fileUri.lowercase()
    val title = document.title.lowercase()
    return uri.contains("a2008004") || uri.contains("a2025004") ||
           uri.contains("decret_executif_25-54") || uri.contains("loi_08-04") ||
           title.contains("a2008004") || title.contains("a2025004") ||
           title.contains("25-54") || title.contains("08-04") ||
           title.contains("التوجيهي") || title.contains("الأساسي")
}

// Helper: Déterminer si une catégorie est le dossier Textes réglementaires protégé
fun isProtectedCategory(category: DocumentCategoryEntity): Boolean {
    val name = category.name.trim().lowercase()
    return name.contains("textes réglementaires") || name.contains("textes reglementaires") ||
           name == "reglementaire" || name.contains("النصوص التشريعية") || name.contains("النصوص التنظيمية")
}

// Helper: Traduction des dossiers vers l'arabe et vice-versa
fun getLocalizedCategoryName(name: String, isAr: Boolean): String {
    val trimmed = name.trim()
    if (isAr) {
        val lower = trimmed.lowercase()
        return when {
            lower == "progressions annuelles" || lower == "progression annuelle" || lower == "progressions" -> "التدرجات السنوية"
            lower == "fiches de cours" || lower == "fiche de cours" || lower == "fiches" -> "مذكرات الدروس"
            lower == "textes réglementaires" || lower == "textes reglementaires" || lower == "reglementaire" -> "النصوص التشريعية والتنظيمية"
            lower == "règlements" || lower == "reglements" || lower == "reglement" -> "النصوص التنظيمية"
            lower == "1as" || lower == "1 as" || lower == "1 as." -> "1 ثانوي"
            lower == "2as" || lower == "2 as" || lower == "2 as." -> "2 ثانوي"
            lower == "3as" || lower == "3 as" || lower == "3 as." -> "3 ثانوي"
            lower == "1am" || lower == "1 am" -> "1 متوسط"
            lower == "2am" || lower == "2 am" -> "2 متوسط"
            lower == "3am" || lower == "3 am" -> "3 متوسط"
            lower == "4am" || lower == "4 am" -> "4 متوسط"
            lower == "1ap" || lower == "1 ap" -> "1 ابتدائي"
            lower == "2ap" || lower == "2 ap" -> "2 ابتدائي"
            lower == "3ap" || lower == "3 ap" -> "3 ابتدائي"
            lower == "4ap" || lower == "4 ap" -> "4 ابتدائي"
            lower == "5ap" || lower == "5 ap" -> "5 ابتدائي"
            lower == "devoirs" || lower == "devoir" -> "فروض"
            lower == "examens" || lower == "examen" || lower == "compositions" || lower == "composition" -> "اختبارات"
            lower == "évaluations" || lower == "evaluations" || lower == "évaluation" || lower == "evaluation" -> "تقويمات"
            lower == "cahier journal" || lower == "cahier de texte" -> "دفتر النصوص"
            lower == "projets" || lower == "projet" -> "مشاريع"
            lower == "documents administratifs" || lower == "administratif" || lower == "administration" -> "وثائق إدارية"
            lower == "emploi du temps" || lower == "emplois du temps" -> "جدول التوقيت"
            lower == "listes des élèves" || lower == "listes des eleves" || lower == "liste des élèves" || lower == "liste des eleves" -> "قوائم التلاميذ"
            lower == "bulletins" || lower == "bulletin" -> "كشوف النقاط"
            lower == "rattrapage" || lower == "soutien" -> "استدراك"
            lower == "archives" || lower == "archive" -> "أرشيف"
            lower == "général" || lower == "general" -> "عام"
            else -> trimmed
        }
    } else {
        return when (trimmed) {
            "التدرجات السنوية" -> "Progressions annuelles"
            "مذكرات الدروس" -> "Fiches de cours"
            "النصوص التشريعية والتنظيمية", "النصوص التنظيمية" -> "Textes réglementaires"
            "1 ثانوي" -> "1AS"
            "2 ثانوي" -> "2AS"
            "3 ثانوي" -> "3AS"
            "فروض", "الفروض" -> "Devoirs"
            "اختبارات", "الاختبارات" -> "Examens"
            "تقويمات", "التقويمات" -> "Évaluations"
            "دفتر النصوص", "كراس اليومية" -> "Cahier journal"
            "مشاريع", "المشاريع" -> "Projets"
            "وثائق إدارية" -> "Documents administratifs"
            "جدول التوقيت" -> "Emploi du temps"
            "قوائم التلاميذ" -> "Listes des élèves"
            "كشوف النقاط" -> "Bulletins"
            "استدراك" -> "Rattrapage"
            "أرشيف" -> "Archives"
            "عام" -> "Général"
            else -> trimmed
        }
    }
}

// Helper: Open the local file via Intent using FileProvider and offer application chooser
private fun openFile(context: Context, filePath: String) {
    try {
        val file = File(filePath)
        if (file.exists()) {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val extension = file.extension.lowercase()
            val mimeType = if (extension.isNotEmpty()) {
                MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "*/*"
            } else {
                "*/*"
            }
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                if (context !is android.app.Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            // Lancer directement sans Intent.createChooser pour offrir le choix natif : Une seule fois ou Toujours
            context.startActivity(intent)
        } else {
            val isArDoc = Tr.isArabicLang()
            Toast.makeText(context, if (isArDoc) "الملف غير موجود. ربما تم نقله أو حذفه." else "Fichier introuvable. Il se peut qu'il ait été déplacé ou supprimé.", Toast.LENGTH_SHORT).show()
        }
    } catch (e: android.content.ActivityNotFoundException) {
        val isArDoc = Tr.isArabicLang()
        Toast.makeText(context, if (isArDoc) "لم يتم العثور على أي تطبيق مناسب لفتح هذه الوثيقة." else "Aucune application de lecture compatible trouvée pour ouvrir ce document.", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        val isArDoc = Tr.isArabicLang()
        Toast.makeText(context, if (isArDoc) "تعذر فتح الملف : ${e.localizedMessage}" else "Impossible d'ouvrir le fichier : ${e.localizedMessage}", Toast.LENGTH_LONG).show()
    }
}

// Helper: File type visualization info
data class FileTypeInfo(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color,
    val containerColor: Color,
    val label: String
)

fun getFileTypeInfo(filePath: String): FileTypeInfo {
    val ext = filePath.substringAfterLast('.', "").lowercase()
    return when (ext) {
        "pdf" -> FileTypeInfo(
            icon = Icons.Default.PictureAsPdf,
            color = Color(0xFFDC2626),
            containerColor = Color(0xFFFEE2E2),
            label = "PDF"
        )
        "doc", "docx", "odt", "rtf", "txt" -> FileTypeInfo(
            icon = Icons.Default.Description,
            color = Color(0xFF2563EB),
            containerColor = Color(0xFFDBEAFE),
            label = ext.uppercase()
        )
        "xls", "xlsx", "csv", "ods" -> FileTypeInfo(
            icon = Icons.Default.Description,
            color = Color(0xFF16A34A),
            containerColor = Color(0xFFDCFCE7),
            label = ext.uppercase()
        )
        "ppt", "pptx", "odp" -> FileTypeInfo(
            icon = Icons.Default.Description,
            color = Color(0xFFEA580C),
            containerColor = Color(0xFFFFEDD5),
            label = ext.uppercase()
        )
        "jpg", "jpeg", "png", "webp", "gif", "bmp", "svg" -> FileTypeInfo(
            icon = Icons.Default.Image,
            color = Color(0xFF9333EA),
            containerColor = Color(0xFFF3E8FF),
            label = ext.uppercase()
        )
        "mp3", "wav", "m4a", "aac", "ogg", "flac" -> FileTypeInfo(
            icon = Icons.Default.Audiotrack,
            color = Color(0xFFDB2777),
            containerColor = Color(0xFFFCE7F3),
            label = ext.uppercase()
        )
        "mp4", "mkv", "mov", "avi", "webm" -> FileTypeInfo(
            icon = Icons.Default.Videocam,
            color = Color(0xFF4F46E5),
            containerColor = Color(0xFFEEF2FF),
            label = ext.uppercase()
        )
        else -> FileTypeInfo(
            icon = Icons.Default.InsertDriveFile,
            color = Color(0xFF475569),
            containerColor = Color(0xFFF1F5F9),
            label = if (ext.isNotEmpty()) ext.uppercase() else "FICHIER"
        )
    }
}

// Helper: Get clean file name from Uri
private fun getFileNameFromUri(context: Context, uri: Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        try {
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    result = cursor.getString(index)
                }
            }
        } finally {
            cursor?.close()
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/')
        if (cut != null && cut != -1) {
            result = result?.substring(cut + 1)
        }
    }
    return result
}

/**
 * Recursive import of a complete folder tree (directories and files)
 */
suspend fun importDirectoryTree(
    context: Context,
    parentCategoryId: Int?,
    directoryDoc: DocumentFile,
    viewModel: AppViewModel
): Pair<Int, Int> {
    var folderCount = 0
    var fileCount = 0

    val catName = directoryDoc.name?.trim()?.takeIf { it.isNotBlank() } ?: "Dossier"
    val newCatId = viewModel.addCategoryAndGetId(catName, parentCategoryId)
    if (newCatId <= 0) return Pair(0, 0)
    folderCount++

    val children = directoryDoc.listFiles()
    for (child in children) {
        if (child.isDirectory) {
            val (subF, subDocs) = importDirectoryTree(context, newCatId, child, viewModel)
            folderCount += subF
            fileCount += subDocs
        } else if (child.isFile) {
            val uri = child.uri
            val name = child.name ?: "document"
            val title = if (name.contains('.')) name.substringBeforeLast('.') else name
            val savedPath = copyUriToInternalStorage(context, uri, name)
            if (savedPath != null) {
                viewModel.addDocumentDirect(newCatId, title, savedPath)
                fileCount++
            }
        }
    }
    return Pair(folderCount, fileCount)
}
