package org.jetbrains.kotlin.backend.konan.llvm

import llvm.LLVMModuleCreateWithName
import llvm.LLVMModuleRef
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.name

internal class LlvmModuleComposer(val context: Context) {

    fun initialize() {
        llvmModule = LLVMModuleCreateWithName("out")!!
    }

    private val fileToModule = mutableMapOf<IrFile, LLVMModuleRef>()

    private var llvmModule: LLVMModuleRef? = null
        set(module) {
            if (field != null) {
                throw Error("Another LLVMModule in the context.")
            }
            field = module!!
            context.debugInfo = DebugInfo(context)
        }


    fun getLlvmModuleForFile(irFile: IrFile): LLVMModuleRef =
            fileToModule.getOrPut(irFile) {
                val moduleName = "${irFile.name}_${irFile.hashCode()}"
                LLVMModuleCreateWithName(moduleName)!!
            }

    fun getGlobalLlvmModule(): LLVMModuleRef =
            llvmModule!!

    fun getCStubsModule(): LLVMModuleRef = llvmModule!!

    fun getModules(): Set<LLVMModuleRef> = setOf(*fileToModule.values.toTypedArray())


}