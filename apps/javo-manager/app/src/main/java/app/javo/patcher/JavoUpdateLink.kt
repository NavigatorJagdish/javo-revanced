package app.javo.patcher

import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableClassDef
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction31c
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableStringReference

/** Change the actionable repair destination, preserving checks and upstream credits. */
object JavoUpdateLink {
    const val TYPE = "Lapp/revanced/extension/shared/checks/Check;"
    const val DESTINATION = "https://navigatorjagdish.github.io/javo-revanced/docs/installation/"

    fun apply(target: MutableClassDef) {
        check(target.type == TYPE)
        val method = target.methods.singleOrNull { it.name == "<clinit>" }
            ?: error("Unsupported update/help initializer in this patch bundle.")
        val code = checkNotNull(method.implementation).instructions
        val matches = code.indices.filter { index ->
            ((code[index] as? ReferenceInstruction)?.reference as? StringReference)?.string == "https://revanced.app"
        }
        check(matches.size == 1) { "Cannot safely configure the Javo update/help link in this bundle." }
        val index = matches.single()
        val register = (code[index] as? OneRegisterInstruction)?.registerA
            ?: error("Unsupported update/help constant instruction.")
        val parse = (code.getOrNull(index + 1) as? ReferenceInstruction)?.reference as? MethodReference
        check(code[index + 1].opcode == Opcode.INVOKE_STATIC && parse?.definingClass == "Landroid/net/Uri;" &&
            parse.name == "parse" && parse.parameterTypes.map { it.toString() } == listOf("Ljava/lang/String;")) {
            "Unsupported update/help URL initialization."
        }
        method.implementation!!.replaceInstruction(index,
            BuilderInstruction31c(Opcode.CONST_STRING_JUMBO, register, ImmutableStringReference(DESTINATION)))
    }
}
