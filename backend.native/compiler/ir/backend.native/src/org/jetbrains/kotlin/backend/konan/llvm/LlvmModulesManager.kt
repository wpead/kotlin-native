package org.jetbrains.kotlin.backend.konan.llvm

import llvm.LLVMModuleCreateWithName
import llvm.LLVMModuleRef
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.ir.declarations.IrFile

internal class LlvmModuleComposer(val context: Context) {

    fun initialize() {
        llvmModule = LLVMModuleCreateWithName("out")!! // TODO: dispose
    }

    private val fileToModule = mutableMapOf<IrFile, LlvmModule>()

    var llvmModule: LLVMModuleRef? = null
        set(module) {
            if (field != null) {
                throw Error("Another LLVMModule in the context.")
            }
            field = module!!

            context.llvm = Llvm(context, module)
            context.globalLlvm = context.llvm.globalLlvm
            context.debugInfo = DebugInfo(context)
        }

    fun getLlvmModuleForFile(irFile: IrFile): LLVMModuleRef {
        return llvmModule!!
    }

    fun getGlobalLlvmModule(): LLVMModuleRef =
            llvmModule!!

    fun getCStubsModule(): LLVMModuleRef = llvmModule!!

    fun getModules(): Set<LLVMModuleRef> = setOf(llvmModule!!)


}