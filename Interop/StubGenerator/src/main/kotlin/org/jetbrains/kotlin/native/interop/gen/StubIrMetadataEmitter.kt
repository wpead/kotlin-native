package org.jetbrains.kotlin.native.interop.gen

import kotlinx.metadata.*
import org.jetbrains.kotlin.native.interop.indexer.preambleLines
import org.jetbrains.kotlin.utils.addIfNotNull
import java.io.File


private class StubtoKm<S, M> {
    private val map = mutableMapOf<S, M>()

    fun getOrPut(stub: S, valueProducer: () -> M) = map.getOrPut(stub, valueProducer)
}

/**
 * Emits [kotlinx.metadata] which can be easily translated to protobuf.
 */
class StubIrMetadataEmitter(
        private val builderResult: StubIrBuilderResult
) {
    fun emit(): KmPackage =
            mapper.visitSimpleStubContainer(builderResult.stubs, null)


    private val mapper = object : StubIrVisitor<StubContainer?, Any> {
        override fun visitClass(element: ClassStub, data: StubContainer?) {
        }

        override fun visitTypealias(element: TypealiasStub, data: StubContainer?) {

        }

        override fun visitFunction(element: FunctionStub, data: StubContainer?): KmFunction =
            KmFunction(
                    flagsOf(*element.getFlags()),
                    element.name
            ).apply {
                returnType = element.returnType.map()
                valueParameters += element.parameters.map { mapValueParameter(it) }
                typeParameters += element.typeParameters.map { mapTypeParameter(it) }
            }

        override fun visitProperty(element: PropertyStub, data: StubContainer?) {

        }

        override fun visitConstructor(constructorStub: ConstructorStub, data: StubContainer?) {

        }

        override fun visitPropertyAccessor(propertyAccessor: PropertyAccessor, data: StubContainer?) {

        }

        override fun visitSimpleStubContainer(simpleStubContainer: SimpleStubContainer, data: StubContainer?): KmPackage {
//            simpleStubContainer.classes.forEach {
//                it.accept(this, simpleStubContainer)
//            }
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
                KmType(flags = flagsOf(*getFlags())).apply {
                    classifier = KmClassifier.Class(this@map.kotlinType.classifier.fqName)
                }
            }
            is ClassifierStubType -> {
                KmType(flags = flagsOf(*getFlags())).apply {
                    classifier = KmClassifier.Class(this@map.classifier.fqName)
                    arguments += this@map.typeArguments.map { mapTypeArgument(it) }
                }
            }
            is RuntimeStubType -> {
                KmType(flags = flagsOf(*getFlags())).apply {
                    classifier = KmClassifier.Class(this@map.fqName)
                }
            }
            is TypeParameterStubType -> {
                KmType(flags = flagsOf(*getFlags())).apply {
                    classifier = KmClassifier.TypeParameter(id = 0)
                }
            }
            is NestedStubType -> {
                KmType(flags = flagsOf(*getFlags())).apply {
                    classifier = KmClassifier.Class(this@map.fqName)
                }
            }
        }

        private fun FunctionStub.getFlags(): Array<Flag> = listOfNotNull(
                Flag.Common.IS_PUBLIC,
                Flag.Function.IS_EXTERNAL,
                Flag.HAS_ANNOTATIONS
        ).toTypedArray()

        private fun StubType.getFlags(): Array<Flag> = listOfNotNull(
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

        private fun mapTypeArgument(typeArgumentStub: TypeArgumentStub): KmTypeProjection =
                KmTypeProjection(KmVariance.INVARIANT, typeArgumentStub.type.map())

        private fun mapTypeParameter(typeParameterStub: TypeParameterStub): KmTypeParameter =
                KmTypeParameter(
                        flags = 0,
                        name = typeParameterStub.name,
                        id = 0,
                        variance = KmVariance.INVARIANT
                ).apply {
                    upperBounds.addIfNotNull(typeParameterStub.upperBound?.map())
                }
    }
}