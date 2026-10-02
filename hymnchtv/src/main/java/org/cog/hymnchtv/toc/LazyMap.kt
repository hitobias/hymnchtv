package org.cog.hymnchtv.toc

/** Read-only map whose contents are produced once, thread-safely, on first access. */
internal class LazyMap<K, V>(loader: () -> Map<K, V>) : AbstractMap<K, V>() {
    private val delegate: Map<K, V> by lazy(LazyThreadSafetyMode.SYNCHRONIZED, loader)

    override val entries: Set<Map.Entry<K, V>> get() = delegate.entries
    override val size: Int get() = delegate.size
    override fun get(key: K): V? = delegate[key]
    override fun containsKey(key: K): Boolean = delegate.containsKey(key)
}
