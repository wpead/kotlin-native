package org.jetbrains.kotlin.native.interop.gen

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
}

/**
 * Mangler which mangles symbols similar to compiler's one.
 */
class KotlinLikeInteropMangler() : InteropMangler {
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
        get() = TODO("not implemented")
    override val ConstantDef.uniqueSymbolName: String
        get() = TODO("not implemented")
    override val WrappedMacroDef.uniqueSymbolName: String
        get() = TODO("not implemented")
    override val GlobalDecl.uniqueSymbolName: String
        get() = TODO("not implemented")
}

