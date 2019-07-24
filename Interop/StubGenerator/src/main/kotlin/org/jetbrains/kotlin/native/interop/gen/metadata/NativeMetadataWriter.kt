package org.jetbrains.kotlin.native.interop.gen.metadata

import kotlinx.metadata.impl.PackageWriter
import org.jetbrains.kotlin.konan.CURRENT
import org.jetbrains.kotlin.konan.KonanVersion
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.library.KonanLibraryLayout
import org.jetbrains.kotlin.konan.library.impl.KonanLibraryWriterImpl
import org.jetbrains.kotlin.konan.properties.Properties
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.library.KonanLibraryVersioning
import org.jetbrains.kotlin.library.KotlinAbiVersion
import org.jetbrains.kotlin.library.SerializedMetadata
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.konan.KonanProtoBuf
import org.jetbrains.kotlin.serialization.StringTableImpl

/**
 * Idea:
 * StubIR -> kotlinx.metadata -> protobuf -> klib
 */

class NativePackageWriter(private val stringTable: StringTableImpl = StringTableImpl()) : PackageWriter(stringTable) {
    fun write(): SerializedMetadata {
        val libraryProto = KonanProtoBuf.LinkDataLibrary.newBuilder()
        libraryProto.moduleName = "<hello>"
        val fragment = buildFragment(t.build())
        val fragmentName = "new_interop"
        libraryProto.addPackageFragmentName("new_interop")
        return SerializedMetadata(libraryProto.build().toByteArray(), listOf(listOf(fragment.toByteArray())), listOf(fragmentName))
    }

    private fun buildFragment(
            packageProto: ProtoBuf.Package
    ): KonanProtoBuf.LinkDataPackageFragment {
        val (stringTableProto, nameTableProto) = stringTable.buildProto()

        return KonanProtoBuf.LinkDataPackageFragment.newBuilder()
                .setFqName("new_interop")
                .setClasses(KonanProtoBuf.LinkDataClasses.newBuilder().build())
                .setPackage(packageProto)
                .setStringTable(stringTableProto)
                .setNameTable(nameTableProto)
                .setIsEmpty(false)
                .build()
    }
}

fun buildInteropKlib(
        outputDir: String,
        metadata: SerializedMetadata,
        manifest: Properties,
        moduleName: String,
        target: KonanTarget
) {
    val version = KonanLibraryVersioning(
            "META_INTEROP",
            abiVersion = KotlinAbiVersion.CURRENT,
            compilerVersion = KonanVersion.CURRENT
    )
    val klibFile = File(outputDir, moduleName)
    KonanLibraryWriterImpl(klibFile, moduleName, version, target).apply {
        addMetadata(metadata)
        addManifestAddend(manifest)
        commit()
    }
}

