package org.example.utilities

import org.example.list.LinkedList
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * JVM unit tests for utilities module using JUnit4.
 */
class StringAndSplitUtilsTest {

    @Test
    fun split_shouldSplitBySpace_andIgnoreEmpty() {
        val list = SplitUtils.split("a  bb   c")
        assertEquals(3, list.size())
        assertEquals("a", list.get(0))
        assertEquals("bb", list.get(1))
        assertEquals("c", list.get(2))
    }

    @Test
    fun join_shouldJoinWithSpaces() {
        val list = LinkedList()
        list.add("one")
        list.add("two")
        list.add("three")

        val joined = StringUtils.join(list)
        assertEquals("one two three", joined)
    }
}
