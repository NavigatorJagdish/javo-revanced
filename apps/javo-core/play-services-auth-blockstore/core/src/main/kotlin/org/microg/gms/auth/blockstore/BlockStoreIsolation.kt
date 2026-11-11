/* SPDX-License-Identifier: Apache-2.0 */
package org.microg.gms.auth.blockstore

internal fun ownsBlockStoreKey(packageName: String, storedKey: String): Boolean =
    storedKey.startsWith("$packageName:")
