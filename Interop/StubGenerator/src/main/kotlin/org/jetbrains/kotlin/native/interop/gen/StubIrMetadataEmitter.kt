package org.jetbrains.kotlin.native.interop.gen

import kotlinx.metadata.*
import org.jetbrains.kotlin.backend.konan.serialization.KonanStringTable
import org.jetbrains.kotlin.metadata.serialization.Interner
import org.jetbrains.kotlin.metadata.serialization.MutableTypeTable
import org.jetbrains.kotlin.native.interop.gen.metadata.annotations
import org.jetbrains.kotlin.native.interop.gen.metadata.getterAnnotations
import org.jetbrains.kotlin.native.interop.gen.metadata.setterAnnotations
import org.jetbrains.kotlin.serialization.StringTableImpl
import org.jetbrains.kotlin.utils.addIfNotNull

/**
 * Emits [kotlinx.metadata] which can be easily translated to protobuf.
 */
class StubIrMetadataEmitter(
        private val builderResult: StubIrBuilderResult,
        private val typeTable: MutableTypeTable,
        private val stringTable: StringTableImpl
) {
    fun emit(): KmPackage =
            mapper.visitSimpleStubContainer(builderResult.stubs, null)

    private val _typeParameterInterner = Interner<TypeParameterStub>()

    private val typeParameterInterner: Interner<TypeParameterStub>
            get() = _typeParameterInterner

    private val mapper = object : StubIrVisitor<StubContainer?, Any> {
        override fun visitClass(element: ClassStub, data: StubContainer?): KmClass {
            return KmClass().apply {
                name = when (element) {
                    is ClassStub.Simple -> element.classifier.fqName.serializedForm()
                    is ClassStub.Companion -> TODO()
                    is ClassStub.Enum -> element.classifier.fqName.serializedForm()
                }

            }
        }

        override fun visitTypealias(element: TypealiasStub, data: StubContainer?) {
        }

        override fun visitFunction(element: FunctionStub, data: StubContainer?): KmFunction =
            KmFunction(
                    flagsOf(*element.flags),
                    element.name
            ).apply {
                returnType = element.returnType.map()
                valueParameters += element.parameters.map { mapValueParameter(it) }
                typeParameters += element.typeParameters.map { mapTypeParameter(it) }
                annotations += element.annotations.map { mapAnnotation(it) }
            }

        override fun visitProperty(element: PropertyStub, data: StubContainer?): KmProperty {



            return KmProperty(
                    name = element.name,
                    flags = flagsOf(*element.flags),
                    getterFlags = flagsOf(*element.getterFlags),
                    setterFlags = flagsOf(*element.setterFlags)
            ).apply {
                returnType = element.type.map()
                getterAnnotations += element.getterAnnotations
                setterAnnotations += element.setterAnnotations
                setterParameter = element.setterParameter
            }
        }

        override fun visitConstructor(constructorStub: ConstructorStub, data: StubContainer?) {

        }

        override fun visitPropertyAccessor(propertyAccessor: PropertyAccessor, data: StubContainer?) {

        }

        override fun visitSimpleStubContainer(simpleStubContainer: SimpleStubContainer, data: StubContainer?): KmPackage {
            simpleStubContainer.classes.forEach {
                it.accept(this, simpleStubContainer)
            }
            val functions = simpleStubContainer.functions.map {
                it.accept(this, simpleStubContainer) as KmFunction
            }
//            simpleStubContainer.properties.forEach {
//                it.accept(this, simpleStubContainer)
//            }
//            simpleStubContainer.typealiases.forEach {
//                it.accept(this, simpleStubContainer)
//            }
//            simpleStubContainer.simpleContainers.forEach {
//                it.accept(this, simpleStubContainer)
//            }
            return KmPackage().apply {
                this.functions += functions
            }
        }

        private fun StubType.map(): KmType = when (this) {
            is WrapperStubType -> {
                KmType(flags = flagsOf(*flags)).apply {
                    val fqName = this@map.kotlinType.classifier.fqName.serializedForm()
                    classifier = KmClassifier.Class(fqName)
                }
            }
            is ClassifierStubType -> {
                KmType(flags = flagsOf(*flags)).apply {
                    val fqName = this@map.classifier.fqName.serializedForm()
                    classifier = KmClassifier.Class(fqName)
                    arguments += this@map.typeArguments.map { mapTypeArgument(it) }
                }
            }
            is TypeParameterStubType -> {
                KmType(flags = flagsOf(*flags)).apply {
                    classifier = KmClassifier.TypeParameter(id = typeParameterInterner.intern(this@map.source))
                }
            }
            is NestedStubType -> {
                KmType(flags = flagsOf(*flags)).apply {
                    classifier = KmClassifier.Class(this@map.fqName.serializedForm())
                }
            }
            is AbbreviationStubType -> {
                KmType(flags = flagsOf(*flags)).apply {
                    abbreviatedType = this@map.abbreviatedType.map()
                    classifier = KmClassifier.Class(this@map.classifierStubType.classifier.fqName.serializedForm())
                    arguments += this@map.classifierStubType.typeArguments.map { mapTypeArgument(it) }
                }
            }
        }

        private fun String.serializedForm() = replace('.', '/')

        private val FunctionStub.flags: Array<Flag>
            get() = listOfNotNull(
                    Flag.Common.IS_PUBLIC,
                    Flag.Function.IS_EXTERNAL,
                    Flag.HAS_ANNOTATIONS
            ).toTypedArray()

        private val PropertyStub.flags: Array<Flag>
            get() = when (kind) {
                is PropertyStub.Kind.Val -> arrayOf(
                        Flag.Property.HAS_GETTER
                )
                is PropertyStub.Kind.Var -> arrayOf(
                        Flag.Property.IS_VAR,
                        Flag.Property.HAS_GETTER,
                        Flag.Property.HAS_SETTER
                )
                is PropertyStub.Kind.Constant -> arrayOf(
                        Flag.Property.IS_CONST
                )
                PropertyStub.Kind.LateinitVar -> arrayOf(
                        Flag.Property.IS_LATEINIT
                )
            }


        private val PropertyStub.getterFlags: Array<Flag>
            get() = when (kind) {
                is PropertyStub.Kind.Val -> arrayOf(
                        Flag.PropertyAccessor.IS_EXTERNAL
                )
                is PropertyStub.Kind.Var -> arrayOf(
                        Flag.PropertyAccessor.IS_EXTERNAL
                )
                is PropertyStub.Kind.Constant -> TODO()
                PropertyStub.Kind.LateinitVar -> TODO()
            }

        private val PropertyStub.setterFlags: Array<Flag>
            get() = when (kind) {
                is PropertyStub.Kind.Val -> arrayOf(
                        Flag.PropertyAccessor.IS_EXTERNAL
                )
                is PropertyStub.Kind.Var -> arrayOf(
                        Flag.PropertyAccessor.IS_EXTERNAL
                )
                is PropertyStub.Kind.Constant -> TODO()
                PropertyStub.Kind.LateinitVar -> TODO()
            }

        private val PropertyStub.getterAnnotations: List<KmAnnotation>
            get() = when (kind) {
                is PropertyStub.Kind.Val -> TODO()
                is PropertyStub.Kind.Var -> TODO()
                is PropertyStub.Kind.Constant -> TODO()
                PropertyStub.Kind.LateinitVar -> TODO()
            }

        private val PropertyStub.setterAnnotations: List<KmAnnotation>
            get() = when (kind) {
                is PropertyStub.Kind.Val -> TODO()
                is PropertyStub.Kind.Var -> TODO()
                is PropertyStub.Kind.Constant -> TODO()
                PropertyStub.Kind.LateinitVar -> TODO()
            }

        private val PropertyStub.setterParameter: KmValueParameter?
            get() = when (kind) {
                is PropertyStub.Kind.Val -> null
                is PropertyStub.Kind.Var -> null
                is PropertyStub.Kind.Constant -> null
                PropertyStub.Kind.LateinitVar -> null
            }

        private val StubType.flags: Array<Flag>
            get() = listOfNotNull(
                    if (nullable) Flag.Type.IS_NULLABLE else null
            ).toTypedArray()

        private fun mapValueParameter(parameter: FunctionParameterStub): KmValueParameter =
                KmValueParameter(
                        flags = 0,
                        name = parameter.name
                ).apply {
                    parameter.type.map().let {
                        if (parameter.isVararg) {
                            varargElementType = it
                        } else {
                            type = it
                        }
                    }

                }

        private fun mapTypeArgument(typeArgumentStub: TypeArgument): KmTypeProjection = when (typeArgumentStub) {
            is TypeArgumentStub -> KmTypeProjection(KmVariance.INVARIANT, typeArgumentStub.type.map()).apply {

            }
            is TypeArgumentStub.StarProjection -> KmTypeProjection.STAR
            else -> error("Unexpected type projection: $typeArgumentStub")
        }


        private fun mapTypeParameter(typeParameterStub: TypeParameterStub): KmTypeParameter =
                KmTypeParameter(
                        flags = 0,
                        name = typeParameterStub.name,
                        id = typeParameterId(typeParameterStub),
                        variance = KmVariance.INVARIANT
                ).apply {
                    upperBounds.addIfNotNull(typeParameterStub.upperBound?.map())
                }

        private fun mapAnnotation(annotationStub: AnnotationStub): KmAnnotation = when (annotationStub) {
            AnnotationStub.ObjC.ConsumesReceiver -> TODO()
            AnnotationStub.ObjC.ReturnsRetained -> TODO()
            is AnnotationStub.ObjC.Method -> TODO()
            is AnnotationStub.ObjC.Factory -> TODO()
            AnnotationStub.ObjC.Consumed -> TODO()
            is AnnotationStub.ObjC.Constructor -> TODO()
            is AnnotationStub.ObjC.ExternalClass -> TODO()
            AnnotationStub.CCall.CString -> TODO()
            AnnotationStub.CCall.WCString -> TODO()
            is AnnotationStub.CCall.Symbol ->
                KmAnnotation(
                        "kotlinx/cinterop/internal/CCall",
                        mapOf(
                              "id" to KmAnnotationArgument.StringValue(annotationStub.symbolName)
                        )
                )
            is AnnotationStub.CCall.GetMemberAt -> TODO()
            is AnnotationStub.CCall.SetMemberAt -> TODO()
            is AnnotationStub.CStruct -> TODO()
            is AnnotationStub.CNaturalStruct -> TODO()
            is AnnotationStub.CLength -> TODO()
            is AnnotationStub.Deprecated -> TODO()
            is AnnotationStub.ReadBits -> TODO()
            is AnnotationStub.WriteBits -> TODO()
        }
    }

    private fun typeParameterId(typeParameter: TypeParameterStub): Int =
        typeParameterInterner.intern(typeParameter)
}