package org.cog.hymnchtv.reading

import android.content.SharedPreferences

/** Map-backed SharedPreferences for JVM tests; a wrong-type read throws ClassCastException like Android does. */
class FakeSharedPreferences(initial: Map<String, Any?> = emptyMap()) : SharedPreferences {
    val values: MutableMap<String, Any?> = initial.toMutableMap()

    override fun getAll(): MutableMap<String, *> = values.toMutableMap()
    override fun getString(key: String, defValue: String?): String? = if (key in values) values[key] as String? else defValue

    @Suppress("UNCHECKED_CAST")
    override fun getStringSet(key: String, defValues: MutableSet<String>?): MutableSet<String>? =
        if (key in values) values[key] as MutableSet<String>? else defValues

    override fun getInt(key: String, defValue: Int): Int = if (key in values) values[key] as Int else defValue
    override fun getLong(key: String, defValue: Long): Long = if (key in values) values[key] as Long else defValue
    override fun getFloat(key: String, defValue: Float): Float = if (key in values) values[key] as Float else defValue
    override fun getBoolean(key: String, defValue: Boolean): Boolean = if (key in values) values[key] as Boolean else defValue
    override fun contains(key: String): Boolean = key in values
    override fun edit(): SharedPreferences.Editor = Editor()
    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit
    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit

    private inner class Editor : SharedPreferences.Editor {
        private val pending = mutableMapOf<String, Any?>()
        private val removed = mutableSetOf<String>()
        private var clearAll = false

        private fun put(key: String, value: Any?): SharedPreferences.Editor {
            pending[key] = value
            return this
        }

        override fun putString(key: String, value: String?) = put(key, value)
        override fun putStringSet(key: String, values: MutableSet<String>?) = put(key, values)
        override fun putInt(key: String, value: Int) = put(key, value)
        override fun putLong(key: String, value: Long) = put(key, value)
        override fun putFloat(key: String, value: Float) = put(key, value)
        override fun putBoolean(key: String, value: Boolean) = put(key, value)

        override fun remove(key: String): SharedPreferences.Editor {
            removed += key
            return this
        }

        override fun clear(): SharedPreferences.Editor {
            clearAll = true
            return this
        }

        override fun commit(): Boolean {
            if (clearAll) values.clear()
            removed.forEach { values.remove(it) }
            values.putAll(pending)
            return true
        }

        override fun apply() {
            commit()
        }
    }
}
