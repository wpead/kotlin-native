package org.jetbrains.kotlin.native.interop.gen.metadata

import kotlinx.metadata.*
import kotlinx.metadata.impl.ReadContext
import kotlinx.metadata.impl.WriteContext
import kotlinx.metadata.impl.extensions.*
import org.jetbrains.kotlin.metadata.ProtoBuf

// It looks like that MetadataExtensions can be separated into several interfaces: read, write, create extensions
class NativeMetadataExtensions : MetadataExtensions {
    override fun readClassExtensions(v: KmClassVisitor, proto: ProtoBuf.Class, c: ReadContext) {

    }

    override fun readPackageExtensions(v: KmPackageVisitor, proto: ProtoBuf.Package, c: ReadContext) {

    }

    override fun readFunctionExtensions(v: KmFunctionVisitor, proto: ProtoBuf.Function, c: ReadContext) {

    }

    override fun readPropertyExtensions(v: KmPropertyVisitor, proto: ProtoBuf.Property, c: ReadContext) {

    }

    override fun readConstructorExtensions(v: KmConstructorVisitor, proto: ProtoBuf.Constructor, c: ReadContext) {

    }

    override fun readTypeParameterExtensions(v: KmTypeParameterVisitor, proto: ProtoBuf.TypeParameter, c: ReadContext) {

    }

    override fun readTypeExtensions(v: KmTypeVisitor, proto: ProtoBuf.Type, c: ReadContext) {

    }

    override fun writeClassExtensions(type: KmExtensionType, proto: ProtoBuf.Class.Builder, c: WriteContext): KmClassExtensionVisitor? {
        return object : NativeClassExtensionVisitor() {

        }
    }

    override fun writePackageExtensions(type: KmExtensionType, proto: ProtoBuf.Package.Builder, c: WriteContext): KmPackageExtensionVisitor? {
        return object : NativePackageExtensionVisitor() {

        }
    }

    override fun writeFunctionExtensions(type: KmExtensionType, proto: ProtoBuf.Function.Builder, c: WriteContext): KmFunctionExtensionVisitor? {
        return object : NativeFunctionExtensionVisitor() {

        }
    }

    override fun writePropertyExtensions(type: KmExtensionType, proto: ProtoBuf.Property.Builder, c: WriteContext): KmPropertyExtensionVisitor? {
        return object : NativePropertyExtensionVisitor() {

        }
    }

    override fun writeConstructorExtensions(type: KmExtensionType, proto: ProtoBuf.Constructor.Builder, c: WriteContext): KmConstructorExtensionVisitor? {
        return object : NativeConstructorExtensionVisitor() {

        }
    }

    override fun writeTypeParameterExtensions(type: KmExtensionType, proto: ProtoBuf.TypeParameter.Builder, c: WriteContext): KmTypeParameterExtensionVisitor? {
        return object : NativeTypeParameterExtensionVisitor() {

        }
    }

    override fun writeTypeExtensions(type: KmExtensionType, proto: ProtoBuf.Type.Builder, c: WriteContext): KmTypeExtensionVisitor? {
        return object : NativeTypeExtensionVisitor() {

        }
    }

    override fun createClassExtension(): KmClassExtension =
            NativeClassExtension()

    override fun createPackageExtension(): KmPackageExtension =
            NativePackageExtension()

    override fun createFunctionExtension(): KmFunctionExtension =
            NativeFunctionExtension()

    override fun createPropertyExtension(): KmPropertyExtension =
            NativePropertyExtension()

    override fun createConstructorExtension(): KmConstructorExtension =
            NativeConstructorExtension()

    override fun createTypeParameterExtension(): KmTypeParameterExtension =
            NativeTypeParameterExtension()

    override fun createTypeExtension(): KmTypeExtension =
            NativeTypeExtension()

}

open class NativeClassExtensionVisitor : KmClassExtensionVisitor {
    override val type: KmExtensionType
        get() = TYPE

    companion object {
        val TYPE: KmExtensionType = KmExtensionType(NativeClassExtensionVisitor::class)
    }
}

class NativeClassExtension() : NativeClassExtensionVisitor(), KmClassExtension  {
    override fun accept(visitor: KmClassExtensionVisitor) {

    }
}

open class NativePackageExtensionVisitor : KmPackageExtensionVisitor {
    override val type: KmExtensionType
        get() = TYPE

    companion object {
        val TYPE: KmExtensionType = KmExtensionType(NativePackageExtensionVisitor::class)
    }
}

class NativePackageExtension() : NativePackageExtensionVisitor(), KmPackageExtension  {
    override fun accept(visitor: KmPackageExtensionVisitor) {

    }
}

open class NativeFunctionExtensionVisitor : KmFunctionExtensionVisitor {
    override val type: KmExtensionType
        get() = TYPE

    companion object {
        val TYPE: KmExtensionType = KmExtensionType(NativeFunctionExtensionVisitor::class)
    }
}

class NativeFunctionExtension() : NativeFunctionExtensionVisitor(), KmFunctionExtension  {
    override fun accept(visitor: KmFunctionExtensionVisitor) {

    }
}

open class NativePropertyExtensionVisitor : KmPropertyExtensionVisitor {
    override val type: KmExtensionType
        get() = TYPE

    companion object {
        val TYPE: KmExtensionType = KmExtensionType(NativePropertyExtensionVisitor::class)
    }
}

class NativePropertyExtension() : NativePropertyExtensionVisitor(), KmPropertyExtension  {
    override fun accept(visitor: KmPropertyExtensionVisitor) {

    }
}

open class NativeConstructorExtensionVisitor : KmConstructorExtensionVisitor {
    override val type: KmExtensionType
        get() = TYPE

    companion object {
        val TYPE: KmExtensionType = KmExtensionType(NativeConstructorExtensionVisitor::class)
    }
}

class NativeConstructorExtension() : NativeConstructorExtensionVisitor(), KmConstructorExtension  {
    override fun accept(visitor: KmConstructorExtensionVisitor) {

    }
}

open class NativeTypeParameterExtensionVisitor : KmTypeParameterExtensionVisitor {
    override val type: KmExtensionType
        get() = TYPE

    companion object {
        val TYPE: KmExtensionType = KmExtensionType(NativeTypeParameterExtensionVisitor::class)
    }
}

class NativeTypeParameterExtension() : NativeTypeParameterExtensionVisitor(), KmTypeParameterExtension  {
    override fun accept(visitor: KmTypeParameterExtensionVisitor) {

    }
}

open class NativeTypeExtensionVisitor : KmTypeExtensionVisitor {
    override val type: KmExtensionType
        get() = TYPE

    companion object {
        val TYPE: KmExtensionType = KmExtensionType(NativeTypeExtensionVisitor::class)
    }
}

class NativeTypeExtension() : NativeTypeExtensionVisitor(), KmTypeExtension  {
    override fun accept(visitor: KmTypeExtensionVisitor) {

    }
}