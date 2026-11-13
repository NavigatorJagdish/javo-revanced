package app.javo.patcher

import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableClassDef.Companion.toMutable
import com.android.tools.smali.dexlib2.DexFileFactory
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.immutable.ImmutableDexFile
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import java.io.File

/** Transform the exact pinned extension with production code, for Android runtime tests. */
fun main(args: Array<String>) {
    val original = DexFileFactory.loadDexFile(File(args[0]), Opcodes.getDefault())
    val target = original.classes.single { it.type == JavoPlaybackPatch.REQUEST }.toMutable()
    JavoPlaybackPatch.patchStreamingDataClass(target)
    val parser = target.methods.single { it.name == "buildPlayerResponseBuffer" }
    val calls = parser.implementation!!.instructions.mapNotNull {
        (it as? ReferenceInstruction)?.reference as? MethodReference
    }
    check(calls.count { it.name == JavoPlaybackPatch.GUARD } == 1)
    check(calls.none { it.name == "getAdaptiveFormatsCount" })
    check(runCatching { JavoPlaybackPatch.patchStreamingDataClass(target) }.isFailure)
    val unsupported = original.classes.single { it.type == JavoPlaybackPatch.REQUEST }.toMutable()
    unsupported.methods.removeAll { it.name == "buildPlayerResponseBuffer" }
    check(runCatching { JavoPlaybackPatch.patchStreamingDataClass(unsupported) }.isFailure)
    check(unsupported.methods.none { it.name == JavoPlaybackPatch.GUARD })
    val help = original.classes.single { it.type == JavoUpdateLink.TYPE }.toMutable()
    JavoUpdateLink.apply(help)
    check(runCatching { JavoUpdateLink.apply(help) }.isFailure)
    DexFileFactory.writeDexFile(args[1], ImmutableDexFile(original.opcodes,
        original.classes.filterNot { it.type == target.type || it.type == help.type } + target + help))
    println("Production stream transformation passed on the pinned extension; runtime tests still required.")
}
