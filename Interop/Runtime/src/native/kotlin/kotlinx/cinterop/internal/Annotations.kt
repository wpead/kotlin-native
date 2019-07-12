package kotlinx.cinterop.internal

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class CStruct(val spelling: String)

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
public annotation class CCall(val id: String) {
    @Target(AnnotationTarget.VALUE_PARAMETER)
    @Retention(AnnotationRetention.BINARY)
    annotation class CString

    @Target(AnnotationTarget.VALUE_PARAMETER)
    @Retention(AnnotationRetention.BINARY)
    annotation class WCString

    @Target(AnnotationTarget.FUNCTION)
    @Retention(AnnotationRetention.BINARY)
    annotation class ReturnsRetained

    @Target(AnnotationTarget.FUNCTION)
    @Retention(AnnotationRetention.BINARY)
    annotation class ConsumesReceiver

    @Target(AnnotationTarget.VALUE_PARAMETER)
    @Retention(AnnotationRetention.BINARY)
    annotation class Consumed

    @Target(AnnotationTarget.PROPERTY_GETTER)
    @Retention(AnnotationRetention.BINARY)
    annotation class ReadBits(val offset: Long, val size: Int, val signed: Boolean)

    @Target(AnnotationTarget.PROPERTY_SETTER)
    @Retention(AnnotationRetention.BINARY)
    annotation class WriteBits(val offset: Long, val size: Int)

    @Target(AnnotationTarget.PROPERTY_GETTER)
    @Retention(AnnotationRetention.BINARY)
    annotation class GetMemberAt(val offset: Long, val typeHolder: String, val isPassedByValue: Boolean)

    @Target(AnnotationTarget.PROPERTY_SETTER)
    @Retention(AnnotationRetention.BINARY)
    annotation class SetMemberAt(val offset: Long, val typeHolder: String)
}
