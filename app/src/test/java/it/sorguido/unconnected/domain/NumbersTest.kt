package it.sorguido.unconnected.domain

import org.junit.Assert.*
import org.junit.Test

class NumbersTest {
    @Test fun normalizesRegionalAndInternationalInput() {
        assertEquals("+12025550101", Numbers.normalize("(202) 555-0101", "US"))
        assertEquals("+12025550101", Numbers.normalize("+1 (202) 555-0101", "IT"))
        assertEquals("+12025550101", Numbers.normalize("0012025550101", "IT"))
    }

    @Test fun rejectsUnsafeLexicalFormsAndInvalidNumbers() {
        listOf("", "112", "911", "+1", "private", "+1-800-FLOWERS", "+12025550101 ext. 5",
            "tel:+12025550101", "+12025550101;phone-context=+1", "+12025550101#", "+12025550101*",
            "++12025550101", "+1 ((202)) 555-0101", "+1 (202 555-0101", "+12025550101-", "１２０２５５５０１０１", "1".repeat(81)).forEach {
            assertNull("Accepted $it", Numbers.normalize(it, "US"))
        }
    }

    @Test fun requiresAnExplicitValidRegion() {
        assertTrue(Numbers.isSupportedRegion("IT"))
        assertFalse(Numbers.isSupportedRegion("ZZ"))
        assertFalse(Numbers.isSupportedRegion("it"))
        assertNull(Numbers.normalize("+12025550101", "ZZ"))
    }

    @Test fun canonicalStorageRequiresExactE164() {
        assertTrue(Numbers.isCanonical("+12025550101", "IT"))
        assertFalse(Numbers.isCanonical("+1 202 555 0101", "IT"))
        assertFalse(Numbers.isCanonical("0012025550101", "IT"))
    }
}
