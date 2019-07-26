package org.jetbrains.kotlin.native.interop.gen.metadata

import kotlinx.metadata.*
import kotlinx.metadata.impl.ReadContext
import kotlinx.metadata.impl.WriteContext
import kotlinx.metadata.impl.extensions.*
import kotlinx.metadata.impl.writeAnnotation
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.konan.KonanProtoBuf

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
        if (type != NativePackageExtensionVisitor.TYPE) return null
        return object : NativePackageExtensionVisitor() {
            override fun visitPackageFqName(fqName: String) {
                // TODO: use [org.jetbrains.kotlin.serialization.StringTableImpl.getPackageFqNameIndex]
                proto.setExtension(KonanProtoBuf.packageFqName, c[fqName])
            }
        }
    }

    override fun writeFunctionExtensions(type: KmExtensionType, proto: ProtoBuf.Function.Builder, c: WriteContext): KmFunctionExtensionVisitor? {
        if (type != NativeFunctionExtensionVisitor.TYPE) return null
        return object : NativeFunctionExtensionVisitor() {
            override fun visitAnnotation(annotation: KmAnnotation) {
                proto.addExtension(KonanProtoBuf.functionAnnotation, annotation.writeAnnotation(c.strings).build())
            }
        }
    }

    override fun writePropertyExtensions(type: KmExtensionType, proto: ProtoBuf.Property.Builder, c: WriteContext): KmPropertyExtensionVisitor? {
        return object : NativePropertyExtensionVisitor() {
            override fun visitAnnotation(annotation: KmAnnotation) {
                proto.addExtension(KonanProtoBuf.propertyAnnotation, annotation.writeAnnotation(c.strings).build())
            }

            override fun visitGetterAnnotation(annotation: KmAnnotation) {
                proto.addExtension(KonanProtoBuf.propertyGetterAnnotation, annotation.writeAnnotation(c.strings).build())
            }

            override fun visitSetterAnnotation(annotation: KmAnnotation) {
                proto.addExtension(KonanProtoBuf.propertySetterAnnotation, annotation.writeAnnotation(c.strings).build())
            }
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





abstract class NativePackageExtensionVisitor : KmPackageExtensionVisitor {
    override val type: KmExtensionType
        get() = TYPE

    companion object {
        val TYPE: KmExtensionType = KmExtensionType(NativePackageExtensionVisitor::class)
    }

    abstract fun visitPackageFqName(fqName: String)
}

class NativePackageExtension : NativePackageExtensionVisitor(), KmPackageExtension {

    var packageFqName: String? = null

    override fun accept(visitor: KmPackageExtensionVisitor) {

    }

    override fun visitPackageFqName(fqName: String) {
        packageFqName = fqName
    }
}

val KmPackage.nativeExtensions: NativePackageExtension
    get() = visitExtensions(NativePackageExtensionVisitor.TYPE) as NativePackageExtension

var KmPackage.packageFqName: String?
    get() = nativeExtensions.packageFqName
    set(value) { nativeExtensions.packageFqName = value }




abstract class NativeFunctionExtensionVisitor : KmFunctionExtensionVisitor {
    override val type: KmExtensionType
        get() = TYPE

    companion object {
        val TYPE: KmExtensionType = KmExtensionType(NativeFunctionExtensionVisitor::class)
    }

    abstract fun visitAnnotation(annotation: KmAnnotation)

}

class NativeFunctionExtension() : NativeFunctionExtensionVisitor(), KmFunctionExtension {

    val annotations: MutableList<KmAnnotation> = mutableListOf()
    var returnType: Int = 0

    override fun visitAnnotation(annotation: KmAnnotation) {
        annotations += annotation
    }

    override fun accept(visitor: KmFunctionExtensionVisitor) {
        require(visitor is NativeFunctionExtensionVisitor)
        annotations.forEach(visitor::visitAnnotation)

    }
}

val KmFunction.nativeExtensions: NativeFunctionExtension
    get() = visitExtensions(NativeFunctionExtensionVisitor.TYPE) as NativeFunctionExtension

val KmFunction.annotations: MutableList<KmAnnotation>
    get() = nativeExtensions.annotations




abstract class NativePropertyExtensionVisitor : KmPropertyExtensionVisitor {
    override val type: KmExtensionType
        get() = TYPE

    companion object {
        val TYPE: KmExtensionType = KmExtensionType(NativePropertyExtensionVisitor::class)
    }

    abstract fun visitAnnotation(annotation: KmAnnotation)

    abstract fun visitGetterAnnotation(annotation: KmAnnotation)

    abstract fun visitSetterAnnotation(annotation: KmAnnotation)
}

class NativePropertyExtension() : NativePropertyExtensionVisitor(), KmPropertyExtension {

    val annotations: MutableList<KmAnnotation> = mutableListOf()
    val getterAnnotations: MutableList<KmAnnotation> = mutableListOf()
    val setterAnnotations: MutableList<KmAnnotation> = mutableListOf()

    override fun accept(visitor: KmPropertyExtensionVisitor) {
        require(visitor is NativePropertyExtensionVisitor)
        annotations.forEach(visitor::visitAnnotation)
    }

    override fun visitAnnotation(annotation: KmAnnotation) {
        annotations += annotation
    }

    override fun visitGetterAnnotation(annotation: KmAnnotation) {
        getterAnnotations += annotation
    }

    override fun visitSetterAnnotation(annotation: KmAnnotation) {
        setterAnnotations += annotation
    }
}

val KmProperty.nativeExtensions: NativePropertyExtension
    get() = visitExtensions(NativePropertyExtensionVisitor.TYPE) as NativePropertyExtension

val KmProperty.annotations: MutableList<KmAnnotation>
    get() = nativeExtensions.annotations

val KmProperty.getterAnnotations: MutableList<KmAnnotation>
    get() = nativeExtensions.getterAnnotations

val KmProperty.setterAnnotations: MutableList<KmAnnotation>
    get() = nativeExtensions.setterAnnotations

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

class NativeTypeExtension() : NativeTypeExtensionVisitor(), KmTypeExtension {

    override fun accept(visitor: KmTypeExtensionVisitor) {

    }
}