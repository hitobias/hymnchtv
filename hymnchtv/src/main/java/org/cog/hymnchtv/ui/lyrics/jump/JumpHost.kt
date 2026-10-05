package org.cog.hymnchtv.ui.lyrics.jump

import org.cog.hymnchtv.hymn.HymnRef
import org.cog.hymnchtv.nav.JumpState

/** What the jump panel (and the jump tests) need from the lyrics page; ContentHandler implements it. */
interface JumpHost {
    /** The return stack and the next slot of this lyrics session. */
    val jumpState: JumpState

    /** The hymn on screen. */
    fun currentRef(): HymnRef

    /** Opens [target] and pushes the hymn on screen (with its reading position) onto the return stack. */
    fun onJump(target: HymnRef)

    /** Returns to the [recentIndex]-th newest stack entry (0 = what the back key does), restoring its reading position. */
    fun onReturnTo(recentIndex: Int)

    /** Queues [target] for the next button (null clears the slot); the page stays where it is. */
    fun onSetNext(target: HymnRef?)

    /** True while the panel is open: the lyrics toolbars do not fade away under it. */
    fun setChromeHeld(held: Boolean)
}
