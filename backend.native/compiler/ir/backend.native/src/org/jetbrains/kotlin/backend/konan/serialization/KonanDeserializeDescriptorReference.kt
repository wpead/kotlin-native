package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.backend.common.serialization.*
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedClassDescriptor

class KonanDescriptorReferenceDeserializer(
    currentModule: ModuleDescriptor,
    mangler: KotlinMangler,
    builtIns: IrBuiltIns,
    private val resolvedForwardDeclarations: MutableMap<UniqId, UniqId>
): DescriptorReferenceDeserializer(currentModule, mangler, builtIns),
   DescriptorUniqIdAware by DeserializedDescriptorUniqIdAware {

    override fun platformSpecificHandler(packageFqName: FqName, name: String, protoIndex: Long?): DeclarationDescriptor? =
            tryHandleForwardDeclaration(packageFqName, name, protoIndex)

    private fun tryHandleForwardDeclaration(packageFqName: FqName, name: String, protoIndex: Long?): DeclarationDescriptor? {
        if (packageFqName.asString().isSpecialPackage()) {
            val descriptor = currentModule.findClassAcrossModuleDependencies(ClassId(packageFqName, FqName(name), false))!!
            if (!(descriptor.fqNameUnsafe.asString().isSpecialPackage())) {
                if (descriptor is DeserializedClassDescriptor) {
                    val uniqId = UniqId(descriptor.getUniqId()!!)
                    val newKey = uniqId
                    val oldKey = UniqId(protoIndex!!)

                    resolvedForwardDeclarations[oldKey] = newKey
                } else {
                    /* ??? */
                }
            }
            return descriptor
        } else {
            return null
        }
    }

    private fun String.isSpecialPackage() =
            startsWith("cnames.") || startsWith("objcnames.")
}
