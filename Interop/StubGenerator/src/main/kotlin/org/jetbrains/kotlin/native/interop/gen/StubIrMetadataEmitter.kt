/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin.native.interop.gen

import kotlinx.metadata.*
import kotlinx.metadata.klib.annotations
import kotlinx.metadata.klib.getterAnnotations
import kotlinx.metadata.klib.setterAnnotations
import org.jetbrains.kotlin.utils.addIfNotNull

class StubIrMetadataEmitter(
        private val stubIrBuilderResult: StubIrBuilderResult
) {
    fun emit(): KmPackage = stubIrBuilderResult.stubs.accept(packageProducer, null)

    private val packageProducer = object : SimpleStubIrVisitor<Nothing?, KmPackage>() {
        override fun visitSimpleStubContainer(simpleStubContainer: SimpleStubContainer, data: Nothing?): KmPackage {
            return KmPackage().apply {
                typeAliases += simpleStubContainer.typealiases.map { it.accept(mapper, simpleStubContainer) as KmTypeAlias }
                functions += simpleStubContainer.functions.map { it.accept(mapper, simpleStubContainer) as KmFunction }
                properties += simpleStubContainer.properties.map { it.accept(mapper, simpleStubContainer) as KmProperty }
            }
        }
    }

    private val mapper = object : StubIrVisitor<StubContainer?, Any> {
        override fun visitClass(element: ClassStub, data: StubContainer?): Any {
            TODO("not implemented")
        }

        override fun visitTypealias(element: TypealiasStub, data: StubContainer?): Any =
                KmTypeAlias(element.flags, element.alias.topLevelName).also { km ->
                    km.underlyingType = element.aliasee.map()
                    km.expandedType = element.aliasee.expandedType.map()
                }

        override fun visitFunction(element: FunctionStub, data: StubContainer?): Any =
                KmFunction(element.flags, element.name).also { km ->
                    km.returnType = element.returnType.map()
                    km.valueParameters += element.parameters.map { it.map() }
                    km.typeParameters += element.typeParameters.map { it.map() }
                    km.annotations += element.annotations.map { it.map() }
                }

        override fun visitProperty(element: PropertyStub, data: StubContainer?): Any =
                KmProperty(element.flags, element.name, element.getterFlags, element.setterFlags).also { km ->
                    km.returnType = element.type.map()
                    if (element.kind is PropertyStub.Kind.Var) {
                        val setter = element.kind.setter
                        km.setterAnnotations += setter.annotations.map { it.map() }
                        val setterParameter = setter.parameters.single()
                        km.setterParameter = KmValueParameter(setterParameter.flags, setterParameter.name).also { km ->
                            km.type = setterParameter.type.map()
                        }
                    }
                    km.getterAnnotations += when (element.kind) {
                        is PropertyStub.Kind.Val -> element.kind.getter.annotations.map { it.map() }
                        is PropertyStub.Kind.Var -> element.kind.getter.annotations.map { it.map() }
                        is PropertyStub.Kind.Constant -> emptyList()
                    }
                    if (element.kind is PropertyStub.Kind.Constant) {

                    }
                }

        override fun visitConstructor(constructorStub: ConstructorStub, data: StubContainer?): Any {
            TODO("not implemented")
        }

        override fun visitPropertyAccessor(propertyAccessor: PropertyAccessor, data: StubContainer?): Any {
            TODO("not implemented")
        }

        override fun visitSimpleStubContainer(simpleStubContainer: SimpleStubContainer, data: StubContainer?): Any {
            TODO("not implemented")
        }
    }

    private fun StubType.map(): KmType = when (this) {
        is ClassifierStubType -> KmType(flags).also { km ->
            km.arguments += typeArguments.map { it.map() }
            if (isTypealias) {
                km.abbreviatedType = abbreviatedType
                km.classifier = expandedType.map().classifier
            } else {
                km.classifier = KmClassifier.Class(classifier.fqNameSerialized)
            }

        }
        is FunctionalType -> KmType(flags).also { km ->
            km.classifier = KmClassifier.Class(classifier.fqNameSerialized)
        }
        is TypeParameterType -> KmType(flags).also { km ->
            km.classifier = KmClassifier.TypeParameter(id)
        }
    }

    private fun FunctionParameterStub.map(): KmValueParameter =
            KmValueParameter(flags, name).also { km ->
                type.map().let {
                    if (isVararg) {
                        km.varargElementType = it
                    } else {
                        km.type = it
                    }
                }
            }

    private fun TypeParameterStub.map(): KmTypeParameter =
            KmTypeParameter(flagsOf(), name, id, KmVariance.INVARIANT).also { km ->
                km.upperBounds.addIfNotNull(upperBound?.map())
            }

    private fun TypeArgument.map(): KmTypeProjection = when (this) {
        TypeArgument.StarProjection -> KmTypeProjection.STAR
        is TypeArgumentStub -> KmTypeProjection(variance.map(), type.map())
        else -> error("Unexpected TypeArgument: $this")
    }

    private fun TypeArgument.Variance.map(): KmVariance = when (this) {
        TypeArgument.Variance.INVARIANT -> KmVariance.INVARIANT
        TypeArgument.Variance.IN -> KmVariance.IN
        TypeArgument.Variance.OUT -> KmVariance.OUT
    }

    private fun AnnotationStub.map(): KmAnnotation {
        val args = when (this) {
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
                    mapOf("id" to KmAnnotationArgument.StringValue(symbolName))
            is AnnotationStub.CStruct -> TODO()
            is AnnotationStub.CNaturalStruct -> TODO()
            is AnnotationStub.CLength -> TODO()
            is AnnotationStub.Deprecated -> TODO()
        }
        return KmAnnotation(classifier.fqNameSerialized, args)
    }

    private val FunctionStub.flags: Flags
        get() = arrayOf(
                Flag.Common.IS_PUBLIC,
                Flag.Function.IS_EXTERNAL,
                Flag.HAS_ANNOTATIONS
        ).let { flagsOf(*it) }

    private val PropertyStub.flags: Flags
        get() = listOfNotNull(
                Flag.IS_PUBLIC,
                Flag.Property.IS_DECLARATION,
                Flag.IS_FINAL,
                when (kind) {
                    is PropertyStub.Kind.Val -> null
                    is PropertyStub.Kind.Var -> Flag.Property.IS_VAR
                    is PropertyStub.Kind.Constant -> Flag.Property.IS_CONST
                },
                when (kind) {
                    is PropertyStub.Kind.Constant -> null
                    is PropertyStub.Kind.Val,
                    is PropertyStub.Kind.Var -> Flag.Property.HAS_GETTER
                },
                when (kind) {
                    is PropertyStub.Kind.Constant -> null
                    is PropertyStub.Kind.Val -> null
                    is PropertyStub.Kind.Var -> Flag.Property.HAS_SETTER
                }
        ).let { flagsOf(*it.toTypedArray()) }

    private val PropertyStub.getterFlags: Flags
        get() = listOfNotNull(
                Flag.HAS_ANNOTATIONS,
                Flag.IS_PUBLIC,
                Flag.IS_FINAL,
                Flag.PropertyAccessor.IS_EXTERNAL
        ).let { flagsOf(*it.toTypedArray()) }

    private val PropertyStub.setterFlags: Flags
        get() = listOfNotNull(
                Flag.HAS_ANNOTATIONS,
                Flag.IS_PUBLIC,
                Flag.IS_FINAL,
                Flag.PropertyAccessor.IS_EXTERNAL
        ).let { flagsOf(*it.toTypedArray()) }

    private val StubType.flags: Flags
        get() = listOfNotNull(
                if (nullable) Flag.Type.IS_NULLABLE else null
        ).let { flagsOf(*it.toTypedArray()) }

    private val TypealiasStub.flags: Flags
        get() = listOfNotNull(
                Flag.IS_PUBLIC
        ).let { flagsOf(*it.toTypedArray()) }

    private val FunctionParameterStub.flags: Flags
        get() = flagsOf()

    private val TypeParameterType.id: Int
        get() = TODO()

    private val TypeParameterStub.id: Int
        get() = TODO()

    private val ClassifierStubType.abbreviatedType: KmType
        get() = KmType(flags).also { km ->
            km.classifier = KmClassifier.TypeAlias(classifier.fqNameSerialized)
        }
}

private val Classifier.fqNameSerialized: String
        get() = fqName.replace('.', '/')