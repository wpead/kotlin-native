/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.backend.common.LoggingContext
import org.jetbrains.kotlin.backend.common.descriptors.WrappedDeclarationDescriptor
import org.jetbrains.kotlin.backend.common.serialization.*
import org.jetbrains.kotlin.backend.konan.descriptors.konanLibrary
import org.jetbrains.kotlin.backend.konan.llvm.KonanMangler
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.konan.DeserializedKlibModuleOrigin
import org.jetbrains.kotlin.descriptors.konan.klibModuleOrigin
import org.jetbrains.kotlin.descriptors.konan.kotlinLibrary
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.impl.IrClassImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrFileSymbolImpl
import org.jetbrains.kotlin.ir.util.NaiveSourceBasedFileEntryImpl
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.library.BaseKotlinLibrary
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.module

// TODO: Should be moved out from this file
private fun BaseKotlinLibrary.isMetadataBasedLibrary() =
        manifestProperties["noir"] == "true"

internal fun ModuleDescriptor.isFromMetadataBasedLibrary() =
        if (klibModuleOrigin !is DeserializedKlibModuleOrigin) false
        else kotlinLibrary.isMetadataBasedLibrary()

class KonanIrLinker(
        currentModule: ModuleDescriptor,
        logger: LoggingContext,
        builtIns: IrBuiltIns,
        symbolTable: SymbolTable,
        private val forwardModuleDescriptor: ModuleDescriptor?,
        exportedDependencies: List<ModuleDescriptor>
) : KotlinIrLinker(logger, builtIns, symbolTable, exportedDependencies, 0L),
    DescriptorUniqIdAware by DeserializedDescriptorUniqIdAware {

    private fun ModuleDescriptor.hasNoIrFiles() =
            isForwardDeclarationModule || isFromMetadataBasedLibrary()

    private val forwardDeclarations = mutableSetOf<IrSymbol>()

    override val descriptorReferenceDeserializer =
            KonanDescriptorReferenceDeserializer(currentModule, KonanMangler, builtIns, resolvedForwardDeclarations)

    override fun reader(moduleDescriptor: ModuleDescriptor, fileIndex: Int, uniqId: UniqId) =
            moduleDescriptor.konanLibrary!!.irDeclaration(uniqId.index, fileIndex)

    override fun readSymbol(moduleDescriptor: ModuleDescriptor, fileIndex: Int, symbolIndex: Int) =
            moduleDescriptor.konanLibrary!!.symbol(symbolIndex, fileIndex)

    override fun readType(moduleDescriptor: ModuleDescriptor, fileIndex: Int, typeIndex: Int) =
            moduleDescriptor.konanLibrary!!.type(typeIndex, fileIndex)

    override fun readString(moduleDescriptor: ModuleDescriptor, fileIndex: Int, stringIndex: Int) =
            moduleDescriptor.konanLibrary!!.string(stringIndex, fileIndex)

    override fun readBody(moduleDescriptor: ModuleDescriptor, fileIndex: Int, bodyIndex: Int) =
            moduleDescriptor.konanLibrary!!.body(bodyIndex, fileIndex)

    override fun readFile(moduleDescriptor: ModuleDescriptor, fileIndex: Int) =
            moduleDescriptor.konanLibrary!!.file(fileIndex)

    override fun readFileCount(moduleDescriptor: ModuleDescriptor) =
            moduleDescriptor.run { if (this.hasNoIrFiles()) 0 else konanLibrary!!.fileCount() }

    private val ModuleDescriptor.userName get() = konanLibrary!!.libraryFile.absolutePath

    override fun checkAccessibility(declarationDescriptor: DeclarationDescriptor) = true

    override fun handleNoModuleDeserializerFound(key: UniqId): DeserializationState<*> {
        return globalDeserializationState
    }

    override fun DeclarationDescriptor.hasNoDeserializedForm(): Boolean =
            module.hasNoIrFiles()

    override fun declareForwardDeclarations() {
        if (forwardModuleDescriptor == null) return

        val packageFragments = forwardDeclarations.map { it.descriptor.findPackage() }.distinct()

        // We don't bother making a real IR module here, as we have no need in it any later.
        // All we need is just to declare forward declarations in the symbol table
        // In case you need a full fledged module, turn the forEach into a map and collect
        // produced files into an IrModuleFragment.

        packageFragments.forEach { packageFragment ->
            val symbol = IrFileSymbolImpl(packageFragment)
            val file = IrFileImpl(NaiveSourceBasedFileEntryImpl("forward declarations pseudo-file"), symbol)
            val symbols = forwardDeclarations
                    .filter { !it.isBound }
                    .filter { it.descriptor.findPackage() == packageFragment }
            val declarations = symbols.map {

                val classDescriptor = it.descriptor as ClassDescriptor
                val declaration = symbolTable.declareClass(
                        UNDEFINED_OFFSET, UNDEFINED_OFFSET, irrelevantOrigin,
                        classDescriptor,
                        classDescriptor.modality
                ) { symbol: IrClassSymbol -> IrClassImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, irrelevantOrigin, symbol) }
                        .also {
                            it.parent = file
                        }
                declaration

            }
            file.declarations.addAll(declarations)
        }
    }

    override fun handleDeserializedSymbol(symbol: IrSymbol) {
        if (symbol.descriptor is ClassDescriptor &&
                symbol.descriptor !is WrappedDeclarationDescriptor<*> &&
                symbol.descriptor.module.isForwardDeclarationModule
        ) {
            forwardDeclarations.add(symbol)
        }
    }

    val modules: Map<String, IrModuleFragment> get() = mutableMapOf<String, IrModuleFragment>().apply {
        deserializersForModules.filter { !it.key.isForwardDeclarationModule }.forEach {
            this.put(it.key.konanLibrary!!.libraryName, it.value.module)
        }
    }
}
