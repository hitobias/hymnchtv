package org.cog.hymnchtv.notebook.repo.room

import org.cog.hymnchtv.notebook.model.IdGenerator
import org.cog.hymnchtv.notebook.model.NotebookValidation

/** A blank id means "new row": generate one. Any other id must be a canonical lowercase UUID. */
internal fun resolveId(id: String, ids: IdGenerator): String = NotebookValidation.uuid(id.ifBlank { ids.newId() })

internal fun optionalUuid(id: String?): String? = id?.let(NotebookValidation::uuid)
