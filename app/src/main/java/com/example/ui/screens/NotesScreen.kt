package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.util.AdaptiveScreenContainer
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.NoteEntity
import com.example.ui.AppViewModel
import com.example.ui.components.FineSearchBar
import com.example.ui.components.GlassmorphicCard
import com.example.util.Tr
import com.example.util.tr
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NotesScreen(viewModel: AppViewModel) {
    val isAr = Tr.isArabic()
    val notes by viewModel.allNotes.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    var showAddEditDialog by remember { mutableStateOf(false) }
    var selectedNoteForEdit by remember { mutableStateOf<NoteEntity?>(null) }
    var noteToDelete by remember { mutableStateOf<NoteEntity?>(null) }

    val filteredNotes = notes.filter {
        it.title.contains(searchQuery, ignoreCase = true) ||
                it.content.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    selectedNoteForEdit = null
                    showAddEditDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_note_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = if (isAr) "إضافة ملاحظة" else "Ajouter une note")
            }
        }
    ) { innerPadding ->
        AdaptiveScreenContainer(maxWidth = 960.dp) {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Adaptive(minSize = 175.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .testTag("notes_screen_grid"),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalItemSpacing = 12.dp
            ) {
            // Header
            item(span = StaggeredGridItemSpan.FullLine) {
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                    Text(
                        text = if (isAr) "الملاحظات والمذكرات" else "Notes & Mémos",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (isAr) "تدوين الملاحظات السريعة لحصصك ودروسك" else "Prenez des notes rapides pour vos cours",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            // Fine Search Bar
            item(span = StaggeredGridItemSpan.FullLine) {
                FineSearchBar(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = if (isAr) "البحث في الملاحظات..." else "Rechercher dans vos notes...",
                    testTag = "search_notes_field",
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                )
            }

            // Staggered Grid of Notes (Modern Bento Look)
            if (filteredNotes.isEmpty()) {
                item(span = StaggeredGridItemSpan.FullLine) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.EditNote,
                                contentDescription = if (isAr) "لا توجد ملاحظات" else "Aucune note",
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(64.dp)
                            )
                            Text(
                                text = if (searchQuery.isNotEmpty()) (if (isAr) "لم يتم العثور على أي نتيجة." else "Aucun résultat trouvé.") else (if (isAr) "لا توجد أي ملاحظات متاحة." else "Aucune note disponible."),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.outline,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = if (searchQuery.isNotEmpty()) (if (isAr) "جرب كلمات مفتاحية أخرى" else "Essayez d'autres mots-clés") else (if (isAr) "أنشئ ملاحظتك الأولى باستخدام زر +" else "Créez votre première note avec le bouton +"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            } else {
                items(filteredNotes, key = { it.id }) { note ->
                    NoteCard(
                        note = note,
                        onClick = {
                            selectedNoteForEdit = note
                            showAddEditDialog = true
                        },
                        onDelete = { noteToDelete = note }
                    )
                }
            }
        }
        }
    }

    // --- Add/Edit Dialog ---
    if (showAddEditDialog) {
        var title by remember { mutableStateOf(selectedNoteForEdit?.title ?: "") }
        var content by remember { mutableStateOf(selectedNoteForEdit?.content ?: "") }

        AlertDialog(
            onDismissRequest = { showAddEditDialog = false },
            title = {
                Text(
                    text = if (selectedNoteForEdit != null) (if (isAr) "تعديل الملاحظة" else "Modifier la Note") else (if (isAr) "ملاحظة جديدة" else "Nouvelle Note"),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text(if (isAr) "العنوان (اختياري)" else "Titre (Optionnel)") },
                        placeholder = { Text(if (isAr) "مثال: فرض الرياضيات" else "Ex: Devoir de mathématiques") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("note_title_input")
                    )

                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text(if (isAr) "المحتوى" else "Contenu") },
                        placeholder = { Text(if (isAr) "مثال: تذكر تصحيح التمرين 4..." else "Ex: Penser à corriger l'exercice 4...") },
                        minLines = 5,
                        maxLines = 10,
                        modifier = Modifier.fillMaxWidth().testTag("note_content_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (content.isNotBlank()) {
                            val targetNote = selectedNoteForEdit
                            if (targetNote != null) {
                                viewModel.updateNote(targetNote.copy(title = title, content = content))
                            } else {
                                viewModel.addNote(title, content)
                            }
                            showAddEditDialog = false
                        }
                    },
                    enabled = content.isNotBlank()
                ) {
                    Text(if (selectedNoteForEdit != null) (if (isAr) "حفظ" else "Enregistrer") else (if (isAr) "إضافة" else "Ajouter"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddEditDialog = false }) {
                    Text(if (isAr) "إلغاء" else "Annuler")
                }
            }
        )
    }

    if (noteToDelete != null) {
        val note = noteToDelete!!
        AlertDialog(
            onDismissRequest = { noteToDelete = null },
            title = { Text(if (isAr) "تأكيد الحذف" else "Confirmer la suppression") },
            text = { Text(if (isAr) "هل أنت متأكد من رغبتك في حذف هذه الملاحظة (${note.title.ifBlank { "بدون عنوان" }}) ؟ هذا الإجراء لا يمكن التراجع عنه." else "Voulez-vous vraiment supprimer cette note (${note.title.ifBlank { "Sans titre" }}) ? Cette action est irréversible.") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        viewModel.deleteNote(note.id)
                        noteToDelete = null
                    }
                ) {
                    Text(if (isAr) "حذف" else "Supprimer")
                }
            },
            dismissButton = {
                TextButton(onClick = { noteToDelete = null }) {
                    Text(if (isAr) "إلغاء" else "Annuler")
                }
            }
        )
    }
}

@Composable
fun NoteCard(
    note: NoteEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val isAr = Tr.isArabic()
    val dateString = remember(note.createdAt) {
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        sdf.format(Date(note.createdAt))
    }

    val isLight = MaterialTheme.colorScheme.background == com.example.ui.theme.OffWhite || MaterialTheme.colorScheme.background == Color(0xFFF8FAFC)
    GlassmorphicCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("note_card_${note.id}"),
        shape = RoundedCornerShape(16.dp),
        elevation = 2.dp,
        borderWidth = 1.1.dp,
        glassAlpha = if (isLight) 0.88f else 0.82f,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                if (note.title.isNotBlank()) {
                    Text(
                        text = note.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text(
                        text = if (isAr) "ملاحظة سريعة" else "Note rapide",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.weight(1f),
                        maxLines = 1
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = if (isAr) "حذف" else "Supprimer",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Text(
                text = note.content,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 6,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateString,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.outline
                )
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = if (isAr) "تعديل" else "Edit",
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}
