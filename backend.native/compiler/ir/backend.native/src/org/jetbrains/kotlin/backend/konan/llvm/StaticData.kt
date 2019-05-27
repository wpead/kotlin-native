/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.llvm

import llvm.*
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.llvm.StaticData.Global
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.util.file

/**
 * Provides utilities to create static data.
 */
internal class StaticData(
        val irFile: IrFile,
        override val context: Context,
        override val llvm: Llvm,
        override val llvmDeclarations: LlvmDeclarations
): LlvmDeclarationsAware {

    override val llvmModule: LLVMModuleRef = llvm.llvmModule

    override fun isExternal(declaration: IrDeclaration): Boolean =
            declaration.file != irFile

    /**
     * Represents the LLVM global variable.
     */
    class Global private constructor(val llvmTargetData: LLVMTargetDataRef, val llvmGlobal: LLVMValueRef) {
        companion object {

            private fun createLlvmGlobal(module: LLVMModuleRef,
                                         type: LLVMTypeRef,
                                         name: String,
                                         isExported: Boolean
            ): LLVMValueRef {

                if (isExported && LLVMGetNamedGlobal(module, name) != null) {
                    throw IllegalArgumentException("Global '$name' already exists")
                }

                // Globals created with this API are *not* thread local.
                val llvmGlobal = LLVMAddGlobal(module, type, name)!!

                if (!isExported) {
                    LLVMSetLinkage(llvmGlobal, LLVMLinkage.LLVMInternalLinkage)
                }

                return llvmGlobal
            }

            fun create(contextUtils: ContextUtils, type: LLVMTypeRef, name: String, isExported: Boolean): Global {

                val isUnnamed = (name == "") // LLVM will select the unique index and represent the global as `@idx`.
                if (isUnnamed && isExported) {
                    throw IllegalArgumentException("unnamed global can't be exported")
                }

                val llvmGlobal = createLlvmGlobal(contextUtils.llvmModule, type, name, isExported)
                return Global(contextUtils.llvmTargetData, llvmGlobal)
            }
        }

        val type get() = getGlobalType(this.llvmGlobal)

        fun setInitializer(value: ConstValue) {
            LLVMSetInitializer(llvmGlobal, value.llvm)
        }

        fun setZeroInitializer() {
            LLVMSetInitializer(llvmGlobal, LLVMConstNull(this.type)!!)
        }

        fun setConstant(value: Boolean) {
            LLVMSetGlobalConstant(llvmGlobal, if (value) 1 else 0)
        }

        fun setLinkage(value: LLVMLinkage) {
            LLVMSetLinkage(llvmGlobal, value)
        }

        fun setAlignment(value: Int) {
            LLVMSetAlignment(llvmGlobal, value)
        }

        fun setSection(name: String) {
            LLVMSetSection(llvmGlobal, name)
        }

        val pointer = Pointer.to(this)
    }

    /**
     * Represents the pointer to static data.
     * It can be a pointer to either a global or any its element.
     *
     * TODO: this class is probably should be implemented more optimally
     */
    class Pointer private constructor(val global: Global,
                                      private val delegate: ConstPointer,
                                      private val offsetInGlobal: Long) : ConstPointer by delegate {

        companion object {
            fun to(global: Global) = Pointer(global, constPointer(global.llvmGlobal), 0L)
        }

        private fun getElementOffset(index: Int): Long {
            val llvmTargetData = global.llvmTargetData
            val type = LLVMGetElementType(delegate.llvmType)
            return when (LLVMGetTypeKind(type)) {
                LLVMTypeKind.LLVMStructTypeKind -> LLVMOffsetOfElement(llvmTargetData, type, index)
                LLVMTypeKind.LLVMArrayTypeKind -> LLVMABISizeOfType(llvmTargetData, LLVMGetElementType(type)) * index
                else -> TODO()
            }
        }

        override fun getElementPtr(index: Int): Pointer {
            return Pointer(global, delegate.getElementPtr(index), offsetInGlobal + this.getElementOffset(index))
        }
    }

    /**
     * Creates array-typed global with given name and value.
     */
    fun placeGlobalArray(name: String, elemType: LLVMTypeRef?, elements: List<ConstValue>, isExported: Boolean = false): Global {
        val initializer = ConstArray(elemType, elements)
        val global = placeGlobal(name, initializer, isExported)

        return global
    }

    private val stringLiterals = mutableMapOf<String, ConstPointer>()
    private val cStringLiterals = mutableMapOf<String, ConstPointer>()

    fun cStringLiteral(value: String) =
            cStringLiterals.getOrPut(value) { placeCStringLiteral(value) }

    private fun createKotlinStringLiteral(value: String): ConstPointer {
        val name = "kstr:" + value.globalHashBase64
        val elements = value.toCharArray().map(::Char16)

        val objRef = createConstKotlinArray(context.ir.symbols.string.owner, elements)

        val res = createAlias(name, objRef)
        LLVMSetLinkage(res.llvm, LLVMLinkage.LLVMWeakAnyLinkage)

        return res
    }

    fun kotlinStringLiteral(value: String) =
        stringLiterals.getOrPut(value) { createKotlinStringLiteral(value) }
}

/**
 * Creates static instance of `konan.ImmutableByteArray` with given values of elements.
 *
 * @param args data for constant creation.
 */
internal fun StaticData.createImmutableBlob(value: IrConst<String>): LLVMValueRef {
    val args = value.value.map { Int8(it.toByte()).llvm }
    return createConstKotlinArray(context.ir.symbols.immutableBlob.owner, args)
}

/**
 * Creates [Global] with given type and name.
 *
 * It is external until explicitly initialized with [Global.setInitializer].
 */
internal fun ContextUtils.createGlobal(type: LLVMTypeRef, name: String, isExported: Boolean = false): Global {
    return Global.create(this, type, name, isExported)
}

/**
 * Creates [Global] with given name and value.
 */
internal fun ContextUtils.placeGlobal(name: String, initializer: ConstValue, isExported: Boolean = false): Global {
    val global = createGlobal(initializer.llvmType, name, isExported)
    global.setInitializer(initializer)
    return global
}