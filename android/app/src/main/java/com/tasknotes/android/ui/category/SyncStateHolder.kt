package com.tasknotes.android.ui.category

import com.tasknotes.android.model.Note

/** Tracks that a note is in edit mode and what its server timestamp was when loaded. */
data class SyncStateHolder(val loadedUpdatedAt: String)

/** Holds all data needed to present the conflict resolution UI. */
data class NoteConflictState(
    val noteId:       Long,
    val serverNote:   Note,
    val localTitle:   String,
    val localContent: String?
)
