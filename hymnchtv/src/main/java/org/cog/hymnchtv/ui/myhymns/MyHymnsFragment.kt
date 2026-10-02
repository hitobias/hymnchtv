package org.cog.hymnchtv.ui.myhymns

import androidx.fragment.app.Fragment
import org.cog.hymnchtv.R

/**
 * "My hymns" tab: an empty slot (contract C-1). It deliberately knows nothing about the notebook: once sub-project D-1 is merged,
 * its integration task puts its home fragment into R.id.myHymnsContainer.
 */
class MyHymnsFragment : Fragment(R.layout.fragment_myhymns)
