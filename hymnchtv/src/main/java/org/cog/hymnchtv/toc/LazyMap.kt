package org.cog.hymnchtv.toc

import java.util.Collections

/**
 * Read-only map whose contents are produced once, thread-safely, on first access. The loaded map is wrapped
 * unmodifiable so Java callers cannot mutate it through clear(), entrySet() or Entry.setValue().
 */
internal class LazyMap<K, V>(loader: () -> Map<K, V>) : AbstractMap<K, V>() {
    private val delegate: Map<K, V> by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { Collections.unmodifiableMap(loader()) }

    override val entries: Set<Map.Entry<K, V>> get() = delegate.entries
    override val size: Int get() = delegate.size
    override fun get(key: K): V? = delegate[key]
    override fun containsKey(key: K): Boolean = delegate.containsKey(key)
}
