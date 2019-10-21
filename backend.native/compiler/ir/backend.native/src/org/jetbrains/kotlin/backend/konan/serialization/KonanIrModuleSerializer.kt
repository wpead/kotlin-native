package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.backend.common.LoggingContext
import org.jetbrains.kotlin.backend.common.serialization.*
import org.jetbrains.kotlin.backend.konan.RuntimeNames
import org.jetbrains.kotlin.backend.konan.llvm.KonanMangler
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsGlobalDeclarationTable
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsIrFileSerializer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.resolve.descriptorUtil.module

class KonanIrModuleSerializer(
    logger: LoggingContext,
    irBuiltIns: IrBuiltIns,
    private val descriptorTable: DescriptorTable
) : IrModuleSerializer<KonanIrFileSerializer>(logger) {


    private val globalDeclarationTable = KonanGlobalDeclarationTable(irBuiltIns)

    override fun createSerializerForFile(file: IrFile): KonanIrFileSerializer {
        val declarationTable = object : DeclarationTable(descriptorTable, globalDeclarationTable, 0), DescriptorUniqIdAware by DeserializedDescriptorUniqIdAware {
            override fun tryComputeSpecial(declaration: IrDeclaration): UniqId? {
                return if (declaration.descriptor.module.isFromMetadataBasedLibrary()) {
                    UniqId(declaration.descriptor.getUniqId() ?: error("No uniq id found for ${declaration.descriptor}"))
                } else {
                    null
                }
            }
        }
        return KonanIrFileSerializer(logger, declarationTable)
    }

}