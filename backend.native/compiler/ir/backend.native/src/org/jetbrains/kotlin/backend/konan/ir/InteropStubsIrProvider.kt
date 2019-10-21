package org.jetbrains.kotlin.backend.konan.ir

import org.jetbrains.kotlin.backend.konan.descriptors.findPackage
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.konan.DeserializedKlibModuleOrigin
import org.jetbrains.kotlin.descriptors.konan.klibModuleOrigin
import org.jetbrains.kotlin.descriptors.konan.kotlinLibrary
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrFileSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.IrProvider
import org.jetbrains.kotlin.ir.util.NaiveSourceBasedFileEntryImpl
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.TypeTranslator
import org.jetbrains.kotlin.library.BaseKotlinLibrary
import org.jetbrains.kotlin.resolve.descriptorUtil.module

// TODO: Should be moved out from this file
private fun BaseKotlinLibrary.isMetadataBasedLibrary() =
        manifestProperties["noir"] == "true"


internal class InteropStubsIrProvider(
        private val symbolTable: SymbolTable,
        private val typeTranslator: TypeTranslator
) : IrProvider {

    private val interopFakeFiles = mutableMapOf<PackageFragmentDescriptor, IrFile>()

    private val PackageFragmentDescriptor.fakeFile: IrFile
        get() = interopFakeFiles.getOrPut(this) {
            val symbol = IrFileSymbolImpl(this)
            IrFileImpl(NaiveSourceBasedFileEntryImpl("Pseudo-file for $fqName"), symbol)
        }

    override fun getDeclaration(symbol: IrSymbol): IrDeclaration? =
            if (symbol.descriptor.module.hasNoIrFiles()) {
                provideIrDeclaration(symbol)
            } else {
                null
            }

    private fun ModuleDescriptor.isFromMetadataBasedLibrary() =
            if (klibModuleOrigin !is DeserializedKlibModuleOrigin) false
            else kotlinLibrary.isMetadataBasedLibrary()

    private fun ModuleDescriptor.hasNoIrFiles() =
            isFromMetadataBasedLibrary()

    private fun provideIrDeclaration(symbol: IrSymbol): IrDeclaration = when (symbol) {
        is IrSimpleFunctionSymbol -> provideIrFunction(symbol)
        else -> error("Unsupported interop declaration: symbol=$symbol, descriptor=${symbol.descriptor}")
    }

    private fun provideIrFunction(symbol: IrSimpleFunctionSymbol): IrFunction =
            symbolTable.declareSimpleFunction(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                    IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB,
                    symbol.descriptor) { symbol -> IrFunctionImpl(
                        UNDEFINED_OFFSET,
                        UNDEFINED_OFFSET,
                        IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB,
                        symbol,
                        typeTranslator.translateType(symbol.descriptor.returnType!!))
            }.also { function ->
                function.annotations += symbol.descriptor.annotations.mapNotNull(typeTranslator.constantValueGenerator::generateAnnotationConstructorCall)
                function.parent = symbol.descriptor.findPackage().fakeFile.also { it.declarations += function }
            }
}