package org.jetbrains.kotlin.native.interop.gen.metadata

import kotlinx.metadata.impl.PackageWriter
import org.jetbrains.kotlin.backend.konan.serialization.KonanStringTable
import org.jetbrains.kotlin.konan.CURRENT
import org.jetbrains.kotlin.konan.KonanVersion
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.library.defaultResolver
import org.jetbrains.kotlin.konan.library.impl.KonanLibraryWriterImpl
import org.jetbrains.kotlin.konan.properties.Properties
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.library.KonanLibraryVersioning
import org.jetbrains.kotlin.library.KotlinAbiVersion
import org.jetbrains.kotlin.library.SerializedMetadata
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.konan.KonanProtoBuf
import org.jetbrains.kotlin.metadata.serialization.MutableTypeTable
import org.jetbrains.kotlin.metadata.serialization.MutableVersionRequirementTable
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.native.interop.gen.StubIrContext
import org.jetbrains.kotlin.serialization.StringTableImpl
import org.jetbrains.kotlin.serialization.konan.KonanMetadataVersion
import org.jetbrains.kotlin.serialization.konan.KonanSerializerExtension
import org.jetbrains.kotlin.serialization.konan.SourceFileMap

/**
 * Idea:
 * StubIR -> kotlinx.metadata -> protobuf -> klib
 */

class NativePackageWriter(
        private val context: StubIrContext,
        private val stringTable: StringTableImpl = KonanStringTable()
) : PackageWriter(stringTable) {

    val sourceFileMap = SourceFileMap()

    private val versionRequirementTable: MutableVersionRequirementTable? = MutableVersionRequirementTable()

    val serializerExtension = KonanSerializerExtension(
            releaseCoroutines = true,
            metadataVersion = KonanMetadataVersion(14),
            sourceFileMap = sourceFileMap,
            descriptorToIndex = { it.hashCode().toLong() }
    )

    fun write(): SerializedMetadata {
        val libraryProto = KonanProtoBuf.LinkDataLibrary.newBuilder()
        libraryProto.moduleName = "<hello>"

        // empty root
        val root = ""
        val rootFragments = listOf(buildFragment(null, "").toByteArray())
        libraryProto.addPackageFragmentName(root)

        val packageName = if (context.configuration.pkgName.isEmpty()) "lib" else context.createPackageName(context.configuration.pkgName)

        MutableTypeTable().serialize()?.let { t.typeTable = it }

        val versionRequirementTableProto = versionRequirementTable?.serialize()
        if (versionRequirementTableProto != null) {
            t.versionRequirementTable = versionRequirementTableProto
        }

        buildPackageProto(packageName, t)
        val packageFragments = listOf(buildFragment(t.build(), packageName).toByteArray())
        libraryProto.addPackageFragmentName(packageName)

        val packages = listOf(rootFragments, packageFragments)
        val packageNames = listOf(root, packageName)

        val libraryProtoBytes = libraryProto.build().toByteArray()
        return SerializedMetadata(libraryProtoBytes, packages, packageNames)
    }

    private fun buildFragment(
            packageProto: ProtoBuf.Package?,
            fqName: String
    ): KonanProtoBuf.LinkDataPackageFragment {
        val (stringTableProto, nameTableProto) = stringTable.buildProto()
        val classesProto = KonanProtoBuf.LinkDataClasses.newBuilder().build()
        return KonanProtoBuf.LinkDataPackageFragment.newBuilder()
                .setFqName(fqName)
                .setClasses(classesProto)
                .setPackage(packageProto ?: ProtoBuf.Package.newBuilder().build())
                .setStringTable(stringTableProto)
                .setNameTable(nameTableProto)
                .setIsEmpty(packageProto == null)
                .build()
    }

    private fun buildPackageProto(packageFqName: String, proto: ProtoBuf.Package.Builder) {
        serializerExtension.serializePackage(FqName(packageFqName), proto)
    }
}

fun produceInteropKLib(
        outputFilePath: String,
        metadata: SerializedMetadata,
        manifest: Properties,
        moduleName: String,
        target: KonanTarget,
        bitcodeFilePath: String
) {
    val version = KonanLibraryVersioning(
            null,
            abiVersion = KotlinAbiVersion.CURRENT,
            compilerVersion = KonanVersion.CURRENT
    )
    val klibFile = File(outputFilePath)

    val repositories: List<String> = listOf("stdlib")
    val resolver = defaultResolver(repositories, target)
    val defaultLinks = resolver.defaultLinks(noStdLib = false, noDefaultLibs = true)
    println("Link deps: ${defaultLinks.joinToString { it.libraryName }}")
    KonanLibraryWriterImpl(klibFile, moduleName, version, target).apply {
        addLinkDependencies(defaultLinks)
        addMetadata(metadata)
        addManifestAddend(manifest)
        addNativeBitcode(bitcodeFilePath)
        commit()
    }
}

