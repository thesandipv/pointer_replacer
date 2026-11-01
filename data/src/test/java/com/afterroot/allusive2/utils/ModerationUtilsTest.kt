/*
 * Copyright (C) 2020-2026 Sandip Vaghela
 * SPDX-License-Identifier: Apache-2.0
 */
package com.afterroot.allusive2.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModerationUtilsTest {

  @Test
  fun testContainsExplicitContent() {
    assertTrue(ModerationUtils.containsExplicitContent("This is some porn content"))
    assertTrue(ModerationUtils.containsExplicitContent("Clean title", "nsfw description"))
    assertFalse(ModerationUtils.containsExplicitContent("Cool Pointer", "A blue cursor pointer"))
  }

  @Test
  fun testIsValidPointerText() {
    assertTrue(ModerationUtils.isValidPointerText("Red Pointer", "Smooth gaming mouse pointer"))
    assertFalse(ModerationUtils.isValidPointerText("", "Description"))
    assertFalse(ModerationUtils.isValidPointerText("A", "Too short name"))
    assertFalse(ModerationUtils.isValidPointerText("Free xxx Pointer", "Adult content"))
  }

  @Test
  fun testHammingDistance() {
    assertEquals(0, ImageHashUtils.hammingDistance("0000000000000000", "0000000000000000"))
    assertEquals(1, ImageHashUtils.hammingDistance("0000000000000000", "0000000000000001"))
    assertEquals(4, ImageHashUtils.hammingDistance("0000000000000000", "000000000000000f"))
  }

  @Test
  fun testSha256() {
    val sample = "Pointer Replacer".toByteArray()
    val hash = ImageHashUtils.calculateSHA256(sample)
    assertEquals(64, hash.length)
  }
}
