/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin.backend.konan

import llvm.LLVMLinkModules2
import llvm.LLVMModuleRef
import llvm.LLVMWriteBitcodeToFile
import org.jetbrains.kotlin.backend.konan.library.impl.buildLibrary
import org.jetbrains.kotlin.backend.konan.llvm.*
import org.jetbrains.kotlin.konan.CURRENT
import org.jetbrains.kotlin.konan.KonanAbiVersion
import org.jetbrains.kotlin.konan.KonanVersion
import org.jetbrains.kotlin.konan.file.isBitcode
import org.jetbrains.kotlin.konan.library.KonanLibraryVersioning
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.Family

val CompilerOutputKind.isNativeBinary: Boolean get() = when (this) {
    CompilerOutputKind.PROGRAM, CompilerOutputKind.DYNAMIC,
    CompilerOutputKind.STATIC, CompilerOutputKind.FRAMEWORK -> true
    CompilerOutputKind.LIBRARY, CompilerOutputKind.BITCODE -> false
}

internal fun produceCStubs(context: Context, llvmModule: LLVMModuleRef) {
    context.cStubsManager.compile(context.config.clang, context.messageCollector, context.inVerbosePhase)?.let {
        parseAndLinkBitcodeFile(llvmModule, it.absolutePath)
    }
}

private fun linkAllDependecies(context: Context, llvmModule: LLVMModuleRef, generatedBitcodeFiles: List<String>) {

    val nativeLibraries = context.config.nativeLibraries + context.config.defaultNativeLibraries
    val bitcodeLibraries = context.globalLlvm.bitcodeToLink.map { it.bitcodePaths }.flatten().filter { it.isBitcode }
    val additionalBitcodeFilesToLink = context.globalLlvm.additionalProducedBitcodeFiles
    val bitcodeFiles = (nativeLibraries + generatedBitcodeFiles + additionalBitcodeFilesToLink + bitcodeLibraries).toSet()

    bitcodeFiles.forEach {
        parseAndLinkBitcodeFile(llvmModule, it)
    }
}

private fun shouldOptimizeWithLlvmApi(context: Context) =
        (context.config.target.family != Family.ZEPHYR && context.config.target.family != Family.WASM)

private fun shoudRunClosedWorldCleanUp(context: Context) =
        // GlobalDCE will kill coverage-related globals.
        !context.coverage.enabled

private fun runLlvmPipeline(context: Context, llvmModule: LLVMModuleRef) = when {
    shouldOptimizeWithLlvmApi(context) -> runLlvmOptimizationPipeline(context, llvmModule)
    shoudRunClosedWorldCleanUp(context) -> runClosedWorldCleanup(context, llvmModule)
    else -> {}
}

internal fun produceOutput(context: Context) {

    val config = context.config.configuration
    val tempFiles = context.config.tempFiles
    val produce = config.get(KonanConfigKeys.PRODUCE)

    when (produce) {
        CompilerOutputKind.STATIC,
        CompilerOutputKind.DYNAMIC,
        CompilerOutputKind.FRAMEWORK,
        CompilerOutputKind.PROGRAM -> {
            val llvmModule = context.composer.getModules().first()
            val output = tempFiles.nativeBinaryFileName
            context.bitcodeFileName = output
            val generatedBitcodeFiles =
                if (produce == CompilerOutputKind.DYNAMIC || produce == CompilerOutputKind.STATIC) {
                    produceCAdapterBitcode(
                        context.config.clang,
                        tempFiles.cAdapterCppName,
                        tempFiles.cAdapterBitcodeName)
                    listOf(tempFiles.cAdapterBitcodeName)
                } else emptyList()
            if (produce == CompilerOutputKind.FRAMEWORK && context.config.produceStaticFramework) {
                embedAppleLinkerOptionsToBitcode(context)
            }
            linkAllDependecies(context, llvmModule, generatedBitcodeFiles)
            runLlvmPipeline(context, llvmModule)
            LLVMWriteBitcodeToFile(llvmModule, output)
        }
        CompilerOutputKind.LIBRARY -> {
            val output = context.config.outputFiles.outputName
            val libraryName = context.config.moduleId
            val neededLibraries = context.librariesWithDependencies
            val abiVersion = KonanAbiVersion.CURRENT
            val compilerVersion = KonanVersion.CURRENT
            val libraryVersion = config.get(KonanConfigKeys.LIBRARY_VERSION)
            val versions = KonanLibraryVersioning(abiVersion = abiVersion, libraryVersion = libraryVersion, compilerVersion = compilerVersion)
            val target = context.config.target
            val nopack = config.getBoolean(KonanConfigKeys.NOPACK)
            val manifestProperties = context.config.manifestProperties

            val library = buildLibrary(
                context.config.nativeLibraries,
                context.config.includeBinaries,
                neededLibraries,
                context.serializedLinkData!!,
                versions,
                target,
                output,
                libraryName,
                null,
                nopack,
                manifestProperties,
                context.dataFlowGraph)

            context.bitcodeFileName = library.mainBitcodeFileName
        }
        CompilerOutputKind.BITCODE -> {
            val output = context.config.outputFile
            context.bitcodeFileName = output
            val llvmModule = context.composer.getModules().first()
            LLVMWriteBitcodeToFile(llvmModule, output)
        }
    }
}

private fun parseAndLinkBitcodeFile(llvmModule: LLVMModuleRef, path: String) {
    val parsedModule = parseBitcodeFile(path)
    val failed = LLVMLinkModules2(llvmModule, parsedModule)
    if (failed != 0) {
        throw Error("failed to link $path") // TODO: retrieve error message from LLVM.
    }
}

private fun embedAppleLinkerOptionsToBitcode(context: Context) {

    val config: KonanConfig = context.config

    fun findEmbeddableOptions(options: List<String>): List<List<String>> {
        val result = mutableListOf<List<String>>()
        val iterator = options.iterator()
        loop@while (iterator.hasNext()) {
            val option = iterator.next()
            result += when {
                option.startsWith("-l") -> listOf(option)
                option == "-framework" && iterator.hasNext() -> listOf(option, iterator.next())
                else -> break@loop // Ignore the rest.
            }
        }
        return result
    }

    val optionsToEmbed = findEmbeddableOptions(config.platform.configurables.linkerKonanFlags) +
            context.globalLlvm.nativeDependenciesToLink.flatMap { findEmbeddableOptions(it.linkerOpts) }

    embedLlvmLinkOptions(context.composer.getGlobalLlvmModule(), optionsToEmbed)
}