package org.cog.hymnchtv.notebook.model;

import static com.google.common.truth.Truth.assertThat;

import org.cog.hymnchtv.MainActivity;
import org.cog.hymnchtv.utils.HymnNoValidate;
import org.junit.Test;

public class HymnTypesConsistencyTest {
    @Test
    public void hymnTypesMirrorMainActivity() {
        assertThat(HymnTypes.DB).isEqualTo(MainActivity.HYMN_DB);
        assertThat(HymnTypes.BB).isEqualTo(MainActivity.HYMN_BB);
        assertThat(HymnTypes.ER).isEqualTo(MainActivity.HYMN_ER);
        assertThat(HymnTypes.XB).isEqualTo(MainActivity.HYMN_XB);
        assertThat(HymnTypes.XG).isEqualTo(MainActivity.HYMN_XG);
        assertThat(HymnTypes.YB).isEqualTo(MainActivity.HYMN_YB);
    }

    @Test
    public void numberingConstantsMirrorHymnNoValidate() {
        assertThat(HymnNumbering.DB_NO_MAX).isEqualTo(HymnNoValidate.HYMN_DB_NO_MAX);
        assertThat(HymnNumbering.DB_NO_TMAX).isEqualTo(HymnNoValidate.HYMN_DB_NO_TMAX);
        assertThat(HymnNumbering.BB_NO_MAX).isEqualTo(HymnNoValidate.HYMN_BB_NO_MAX);
        assertThat(HymnNumbering.BB_DUMMY).isEqualTo(HymnNoValidate.HYMN_BB_DUMMY);
        assertThat(HymnNumbering.ER_NO_MAX).isEqualTo(HymnNoValidate.HYMN_ER_NO_MAX);
        assertThat(HymnNumbering.XB_NO_MAX).isEqualTo(HymnNoValidate.HYMN_XB_NO_MAX);
        assertThat(HymnNumbering.XG_NO_MAX).isEqualTo(HymnNoValidate.HYMN_XG_NO_MAX);
        assertThat(HymnNumbering.YB_NO_TMAX).isEqualTo(HymnNoValidate.HYMN_YB_NO_TMAX);
    }
}
