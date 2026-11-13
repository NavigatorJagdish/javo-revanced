package app.javo.patcher

import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableClassDef
import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableMethod.Companion.toMutable
import app.revanced.patcher.patch.BytecodePatchContext
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MethodImplementationBuilder
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.builder.instruction.*
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableTypeReference
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableFieldReference
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

/** Javo's small, version-checked extension to the inspected upstream stream patch.
 * A SABR-only response can advertise formats without usable DASH URLs. Returning
 * zero lets upstream's existing rejection/close/client-fallback path handle it.
 * This does not promise that a URL is accepted by the remote video server.
 */
object JavoPlaybackPatch {
    const val REQUEST = "Lapp/revanced/extension/shared/spoof/requests/StreamingDataRequest;"
    const val STREAM = "Lapp/revanced/extension/shared/innertube/PlayerResponseOuterClass\$StreamingData;"
    const val FORMAT = "Lapp/revanced/extension/shared/innertube/PlayerResponseOuterClass\$Format;"
    const val GUARD = "javoPlayableAdaptiveFormats"

    fun apply(classes: BytecodePatchContext.ClassDefs) {
        // Check the extension ABI, rather than assuming every future bundle has it.
        for ((type, name, parameters, result) in listOf(
            arrayOf(STREAM, "getAdaptiveFormatsCount", "", "I"),
            arrayOf(STREAM, "getAdaptiveFormats", "I", FORMAT),
            arrayOf(FORMAT, "getUrl", "", "Ljava/lang/String;"),
            arrayOf(FORMAT, "getSignatureCipher", "", "Ljava/lang/String;")
        )) {
            check(classes[type]?.methods?.any {
                it.name == name && it.parameterTypes.joinToString("") == parameters && it.returnType == result
            } == true) { "Unsupported stream extension API: $name. Use a reviewed compatible patch bundle." }
        }
        val original = checkNotNull(classes[REQUEST]) { "The selected stream patch has no supported extension." }
        patchStreamingDataClass(classes.getOrReplaceMutable(original))
    }

    internal fun patchStreamingDataClass(target: MutableClassDef) {
        check(target.type == REQUEST)
        check(target.methods.none { it.name == GUARD }) { "Javo stream guard already exists." }
        val method = target.methods.singleOrNull {
            it.name == "buildPlayerResponseBuffer" && it.returnType == "[B" &&
                it.parameterTypes.map { p -> p.toString() } == listOf(
                    "Lapp/revanced/extension/shared/spoof/ClientType;", "Ljava/net/HttpURLConnection;")
        } ?: error("Unsupported stream response parser. Use a reviewed compatible patch bundle.")
        val code = checkNotNull(method.implementation).instructions
        val matches = code.indices.filter { index ->
            val ref = (code[index] as? ReferenceInstruction)?.reference as? MethodReference
            code[index].opcode == Opcode.INVOKE_VIRTUAL && ref?.definingClass == STREAM &&
                ref.name == "getAdaptiveFormatsCount" && ref.parameterTypes.isEmpty() && ref.returnType == "I"
        }
        check(matches.size == 1) { "Cannot safely locate the stream fallback check in this bundle." }
        val index = matches.single()
        val call = code[index] as FiveRegisterInstruction
        check(call.registerCount == 1 && code.getOrNull(index + 1)?.opcode == Opcode.MOVE_RESULT &&
            code.getOrNull(index + 2)?.opcode == Opcode.IF_NEZ) { "Unsupported stream fallback control flow." }

        val body = MethodImplementationBuilder(5)
        fun invoke(op: Opcode, owner: String, name: String, params: List<String>, result: String, vararg registers: Int) {
            body.addInstruction(BuilderInstruction35c(op, registers.size,
                registers.getOrElse(0) { 0 }, registers.getOrElse(1) { 0 }, 0, 0, 0,
                ImmutableMethodReference(owner, name, params, result)))
        }
        // v4 is p0. Keep the original parser's registers, try/finally and branches intact.
        invoke(Opcode.INVOKE_VIRTUAL, STREAM, "getAdaptiveFormatsCount", emptyList(), "I", 4)
        body.addInstruction(BuilderInstruction11x(Opcode.MOVE_RESULT, 0))
        body.addInstruction(BuilderInstruction11n(Opcode.CONST_4, 1, 0))
        val loop = body.addLabel("next_format")
        // Forward labels are placed after appending the complete instruction sequence.
        val unavailable = body.getLabel("unavailable")
        val usable = body.getLabel("usable")
        body.addInstruction(BuilderInstruction22t(Opcode.IF_GE, 1, 0, unavailable))
        invoke(Opcode.INVOKE_VIRTUAL, STREAM, "getAdaptiveFormats", listOf("I"), FORMAT, 4, 1)
        body.addInstruction(BuilderInstruction11x(Opcode.MOVE_RESULT_OBJECT, 2))
        invoke(Opcode.INVOKE_VIRTUAL, FORMAT, "getUrl", emptyList(), "Ljava/lang/String;", 2)
        body.addInstruction(BuilderInstruction11x(Opcode.MOVE_RESULT_OBJECT, 3))
        invoke(Opcode.INVOKE_VIRTUAL, "Ljava/lang/String;", "isEmpty", emptyList(), "Z", 3)
        body.addInstruction(BuilderInstruction11x(Opcode.MOVE_RESULT, 3))
        body.addInstruction(BuilderInstruction21t(Opcode.IF_EQZ, 3, usable))
        invoke(Opcode.INVOKE_VIRTUAL, FORMAT, "getSignatureCipher", emptyList(), "Ljava/lang/String;", 2)
        body.addInstruction(BuilderInstruction11x(Opcode.MOVE_RESULT_OBJECT, 3))
        invoke(Opcode.INVOKE_VIRTUAL, "Ljava/lang/String;", "isEmpty", emptyList(), "Z", 3)
        body.addInstruction(BuilderInstruction11x(Opcode.MOVE_RESULT, 3))
        body.addInstruction(BuilderInstruction21t(Opcode.IF_EQZ, 3, usable))
        body.addInstruction(BuilderInstruction22b(Opcode.ADD_INT_LIT8, 1, 1, 1))
        body.addInstruction(BuilderInstruction10t(Opcode.GOTO, loop))
        body.addLabel("unavailable")
        body.addInstruction(BuilderInstruction11n(Opcode.CONST_4, 0, 0))
        body.addLabel("usable")
        body.addInstruction(BuilderInstruction11x(Opcode.RETURN, 0))
        val helper = ImmutableMethod(REQUEST, GUARD,
            listOf(ImmutableMethodParameter(STREAM, null, null)), "I",
            AccessFlags.PUBLIC.value or AccessFlags.STATIC.value, null, null, body.methodImplementation).toMutable()
        target.methods.add(helper)
        method.implementation!!.replaceInstruction(index, BuilderInstruction35c(Opcode.INVOKE_STATIC,
            1, call.registerC, 0, 0, 0, 0, ImmutableMethodReference(REQUEST, GUARD, listOf(STREAM), "I")))
        patchClientOrderPublication(target)
    }

    private fun patchClientOrderPublication(target: MutableClassDef) {
        val client = "Lapp/revanced/extension/shared/spoof/ClientType;"
        val array = "[$client"
        check(target.fields.any { it.name == "clientOrderToUse" && it.type == array &&
            AccessFlags.VOLATILE.isSet(it.accessFlags) && AccessFlags.STATIC.isSet(it.accessFlags) }) {
            "Unsupported stream client-order field."
        }
        val setter = target.methods.singleOrNull { it.name == "setClientOrderToUse" &&
            it.parameterTypes.map { p -> p.toString() } == listOf("Ljava/util/List;", client) &&
            it.returnType == "V" && AccessFlags.STATIC.isSet(it.accessFlags) }
            ?: error("Unsupported stream client-order setter.")
        val body = MethodImplementationBuilder(6)
        fun call(op: Opcode, owner: String, name: String, params: List<String>, result: String, vararg r: Int) {
            body.addInstruction(BuilderInstruction35c(op, r.size, r.getOrElse(0) { 0 },
                r.getOrElse(1) { 0 }, 0, 0, 0, ImmutableMethodReference(owner, name, params, result)))
        }
        val list = "Ljava/util/ArrayList;"
        val obj = "Ljava/lang/Object;"
        // p0/v4 = available clients; p1/v5 = preference. Build privately in v0.
        call(Opcode.INVOKE_STATIC, "Ljava/util/Objects;", "requireNonNull", listOf(obj), obj, 5)
        body.addInstruction(BuilderInstruction21c(Opcode.NEW_INSTANCE, 0, ImmutableTypeReference(list)))
        call(Opcode.INVOKE_DIRECT, list, "<init>", emptyList(), "V", 0)
        call(Opcode.INVOKE_VIRTUAL, list, "add", listOf(obj), "Z", 0, 5)
        call(Opcode.INVOKE_INTERFACE, "Ljava/util/List;", "iterator", emptyList(), "Ljava/util/Iterator;", 4)
        body.addInstruction(BuilderInstruction11x(Opcode.MOVE_RESULT_OBJECT, 4))
        val next = body.addLabel("next")
        val publish = body.getLabel("publish")
        call(Opcode.INVOKE_INTERFACE, "Ljava/util/Iterator;", "hasNext", emptyList(), "Z", 4)
        body.addInstruction(BuilderInstruction11x(Opcode.MOVE_RESULT, 1))
        body.addInstruction(BuilderInstruction21t(Opcode.IF_EQZ, 1, publish))
        call(Opcode.INVOKE_INTERFACE, "Ljava/util/Iterator;", "next", emptyList(), obj, 4)
        body.addInstruction(BuilderInstruction11x(Opcode.MOVE_RESULT_OBJECT, 1))
        body.addInstruction(BuilderInstruction21c(Opcode.CHECK_CAST, 1, ImmutableTypeReference(client)))
        body.addInstruction(BuilderInstruction22t(Opcode.IF_EQ, 1, 5, next))
        call(Opcode.INVOKE_STATIC, "Ljava/util/Objects;", "requireNonNull", listOf(obj), obj, 1)
        call(Opcode.INVOKE_VIRTUAL, list, "add", listOf(obj), "Z", 0, 1)
        body.addInstruction(BuilderInstruction10t(Opcode.GOTO, next))
        body.addLabel("publish")
        body.addInstruction(BuilderInstruction11n(Opcode.CONST_4, 1, 0))
        body.addInstruction(BuilderInstruction22c(Opcode.NEW_ARRAY, 1, 1, ImmutableTypeReference(array)))
        call(Opcode.INVOKE_VIRTUAL, list, "toArray", listOf("[$obj"), "[$obj", 0, 1)
        body.addInstruction(BuilderInstruction11x(Opcode.MOVE_RESULT_OBJECT, 0))
        body.addInstruction(BuilderInstruction21c(Opcode.CHECK_CAST, 0, ImmutableTypeReference(array)))
        // The only volatile write happens after every slot has been filled.
        body.addInstruction(BuilderInstruction21c(Opcode.SPUT_OBJECT, 0,
            ImmutableFieldReference(REQUEST, "clientOrderToUse", array)))
        body.addInstruction(BuilderInstruction10x(Opcode.RETURN_VOID))
        setter.setImplementation(MutableMethodImplementation(body.methodImplementation))
    }
}
