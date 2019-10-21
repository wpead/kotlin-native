package org.jetbrains.kotlin.native.interop.gen

import org.jetbrains.kotlin.backend.common.serialization.cityHash64
import org.jetbrains.kotlin.native.interop.indexer.*

/**
 * Similar to [KotlinMangler] from compiler.
 * This is needed for generation of [UniqId] which is required for
 * linkage process.
 */
interface InteropMangler {
    val StructDecl.uniqueSymbolName: String
    val EnumDef.uniqueSymbolName: String
    val ObjCClass.uniqueSymbolName: String
    val ObjCProtocol.uniqueSymbolName: String
    val ObjCCategory.uniqueSymbolName: String
    val TypedefDef.uniqueSymbolName: String
    val FunctionDecl.uniqueSymbolName: String
    val ConstantDef.uniqueSymbolName: String
    val WrappedMacroDef.uniqueSymbolName: String
    val GlobalDecl.uniqueSymbolName: String

    val String.hashMangle: Long
}

/**
 * Mangler which mangles symbols similar to compiler's one.
 *
 * It's simpler because:
 *  1. All declarations are public.
 */
class KotlinLikeInteropMangler() : InteropMangler {

    companion object {
        private const val PUBLIC_MASK = 1L shl 63
    }

    override val StructDecl.uniqueSymbolName: String
        get() = TODO("not implemented")
    override val EnumDef.uniqueSymbolName: String
        get() = TODO("not implemented")
    override val ObjCClass.uniqueSymbolName: String
        get() = TODO("not implemented")
    override val ObjCProtocol.uniqueSymbolName: String
        get() = TODO("not implemented")
    override val ObjCCategory.uniqueSymbolName: String
        get() = TODO("not implemented")
    override val TypedefDef.uniqueSymbolName: String
        get() = TODO("not implemented")
    override val FunctionDecl.uniqueSymbolName: String
        get() {

            return "kfun:#$functionName"
        }
    override val ConstantDef.uniqueSymbolName: String
        get() = TODO("not implemented")
    override val WrappedMacroDef.uniqueSymbolName: String
        get() = TODO("not implemented")
    override val GlobalDecl.uniqueSymbolName: String
        get() = TODO("not implemented")

    override val String.hashMangle: Long
        // (1L shl 63) denotes public declaration
        get() = cityHash64() or (PUBLIC_MASK)

    private val FunctionDecl.functionName: String
        get() {
            return "$name$signature"
        }

    private val FunctionDecl.signature: String
        get() {

            val signatureSuffix = when {
//                returnType.isInlined -> "ValueType"
//                !returnType.isVoid -> acyclicTypeMangler(this.returnType)
                else -> ""
            }

            return "($argsPart)$signatureSuffix"
        }

    private val FunctionDecl.argsPart: String
        get() = this.parameters.map { parameter ->

            }.joinToString(";")

    private fun acyclicTypeMangler(type: Type): String =  when (type) {
        VoidType -> ""
        else -> ""
    }
}

//private val Type.isVoid

