package org.cog.hymnchtv

/**
 * Marks the instrumented tests of the quick suite: core user flows that are cheap and stable.
 * Run with `-e annotation org.cog.hymnchtv.QuickTest` (see docs/testing.md).
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class QuickTest
