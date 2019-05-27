package org.jetbrains.kotlin.backend.konan.llvm

import llvm.LLVMModuleRef
import llvm.LLVMValueRef
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFile

internal class FileModuleGenerator(
        val irFile: IrFile,
        val context: Context,
        val llvmModule: LLVMModuleRef,
        val llvmDeclarations: LlvmDeclarations,
        val staticData: StaticData,
        val llvm: Llvm,
        intrinsicGeneratorEnvironment : IntrinsicGeneratorEnvironment
) {
    val fileInitializers = mutableListOf<IrField>()
    val objects = mutableSetOf<LLVMValueRef>()
    val sharedObjects = mutableSetOf<LLVMValueRef>()

    val codegen = CodeGenerator(context, this)

    val intrinsicGenerator = IntrinsicGenerator(intrinsicGeneratorEnvironment)

}