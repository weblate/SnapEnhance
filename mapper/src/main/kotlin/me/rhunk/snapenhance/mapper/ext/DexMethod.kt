package me.rhunk.snapenhance.mapper.ext

import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.MethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.instruction.WideLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction21c
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction22c
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import kotlin.collections.orEmpty

fun MethodImplementation.findConstString(
    string: String,
    contains: Boolean = false,
    startsWith: Boolean = false,
    ignoreCase: Boolean = false
): Boolean = instructions.filterIsInstance<Instruction21c>().any {
     (it.reference as? StringReference)?.string?.let { str ->
        if (contains && str.contains(string, ignoreCase = ignoreCase)) return@any true
        if (startsWith && str.startsWith(string, ignoreCase = ignoreCase)) return@any true
        str == string
    } == true
}

fun MethodImplementation.getAllConstStrings(): List<String> = instructions.filterIsInstance<Instruction21c>().mapNotNull {
    it.reference as? StringReference
}.map {
    it.string
}

fun MethodImplementation.searchNextFieldReference(constString: String, contains: Boolean = false): FieldReference? = this.instructions.let {
    var found = false
    for (instruction in it) {
        if (instruction is Instruction21c && instruction.reference is StringReference) {
            val str = (instruction.reference as StringReference).string
            if (if (contains) str.contains(constString) else str == constString) {
                found = true
            }
        }

        if (!found) continue

        if (instruction is Instruction22c &&
            instruction.reference is FieldReference
        ) {
            return@let (instruction.reference as FieldReference)
        }
    }
    null
}

fun MethodImplementation.findSetViewIds(): List<Int> {
    val instructions = instructions.toList()

    return instructions.mapIndexedNotNull { index, instruction ->
        val ref = (instruction as? ReferenceInstruction)?.reference as? MethodReference ?: return@mapIndexedNotNull null

        if (ref.name != "setId" || ref.parameterTypes != listOf("I") || ref.returnType != "V") {
            return@mapIndexedNotNull null
        }

        val register = when (instruction) {
            is FiveRegisterInstruction -> {
                if (instruction.registerCount < 2) return@mapIndexedNotNull null
                instruction.registerD
            }

            is RegisterRangeInstruction -> {
                if (instruction.registerCount < 2) return@mapIndexedNotNull null
                instruction.startRegister + 1
            }

            else -> return@mapIndexedNotNull null
        }

        for (i in index - 1 downTo 0) {
            val previous = instructions[i]
            if (previous !is OneRegisterInstruction || previous.registerA != register) continue
            return@mapIndexedNotNull (previous as? NarrowLiteralInstruction)?.narrowLiteral
        }
        null
    }.distinct()
}

fun Method.findSetViewIds(): List<Int> = implementation?.findSetViewIds().orEmpty()

fun Method.referencedMethods(): List<MethodReference> {
    return implementation?.instructions?.asSequence()?.mapNotNull { instruction ->
        (instruction as? ReferenceInstruction)?.reference as? MethodReference
    }?.toList().orEmpty()
}

fun Method.referencedFields(): List<FieldReference> {
    return implementation?.instructions?.asSequence()?.mapNotNull { instruction ->
        (instruction as? ReferenceInstruction)?.reference as? FieldReference
    }?.toList().orEmpty()
}
