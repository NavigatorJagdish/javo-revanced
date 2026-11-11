package org.microg.gms.auth.blockstore

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockStoreIsolationTest {
    @Test fun acceptsOnlyKeysForTheExactOwningPackage() {
        assertTrue(ownsBlockStoreKey("app.javo.example", "app.javo.example:token"))
        assertTrue(ownsBlockStoreKey("app.javo.example", "app.javo.example:session:nested"))
    }

    @Test fun rejectsPackagePrefixAndSiblingAttacks() {
        assertFalse(ownsBlockStoreKey("app.javo", "app.javo.example:token"))
        assertFalse(ownsBlockStoreKey("app.javo.example", "app.javo.exampleevil:token"))
        assertFalse(ownsBlockStoreKey("app.javo.example", "app.javo.other:token"))
        assertFalse(ownsBlockStoreKey("app.javo.example", "app.javo.example"))
    }
}
