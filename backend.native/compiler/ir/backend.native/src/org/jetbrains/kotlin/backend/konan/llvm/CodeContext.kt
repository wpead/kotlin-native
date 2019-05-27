package org.jetbrains.kotlin.backend.konan.llvm

import llvm.DIScopeOpaqueRef
import llvm.LLVMBasicBlockRef
import llvm.LLVMValueRef
import org.jetbrains.kotlin.ir.declarations.IrSymbolOwner
import org.jetbrains.kotlin.ir.declarations.IrValueDeclaration
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrBreak
import org.jetbrains.kotlin.ir.expressions.IrContinue

/**
 * Defines how to generate context-dependent operations.
 */
internal interface CodeContext {

    /**
     * Generates `return` [value] operation.
     *
     * @param value may be null iff target type is `Unit`.
     */
    fun genReturn(target: IrSymbolOwner, value: LLVMValueRef?)

    fun genBreak(destination: IrBreak)

    fun genContinue(destination: IrContinue)

    val exceptionHandler: ExceptionHandler

    fun genThrow(exception: LLVMValueRef)

    /**
     * Declares the variable.
     * @return index of declared variable.
     */
    fun genDeclareVariable(variable: IrVariable, value: LLVMValueRef?, variableLocation: VariableDebugLocation?): Int

    /**
     * @return index of variable declared before, or -1 if no such variable has been declared yet.
     */
    fun getDeclaredVariable(variable: IrVariable): Int

    /**
     * Generates the code to obtain a value available in this context.
     *
     * @return the requested value
     */
    fun genGetValue(value: IrValueDeclaration): LLVMValueRef

    /**
     * Returns owning function scope.
     *
     * @return the requested value
     */
    fun functionScope(): CodeContext?

    /**
     * Returns owning file scope.
     *
     * @return the requested value if in the file scope or null.
     */
    fun fileScope(): CodeContext?

    /**
     * Returns owning class scope [ClassScope].
     *
     * @returns the requested value if in the class scope or null.
     */
    fun classScope(): CodeContext?

    fun addResumePoint(bbLabel: LLVMBasicBlockRef): Int

    /**
     * Returns owning returnable block scope [ReturnableBlockScope].
     *
     * @returns the requested value if in the returnableBlockScope scope or null.
     */
    fun returnableBlockScope(): CodeContext?

    /**
     * Returns location information for given source location [LocationInfo].
     */
    fun location(line:Int, column: Int): LocationInfo?

    /**
     * Returns [DIScopeOpaqueRef] instance for corresponding scope.
     */
    fun scope(): DIScopeOpaqueRef?
}

/**
 * Fake [CodeContext] that doesn't support any operation.
 *
 * During function code generation [FunctionScope] should be set up.
 */
internal object TopLevelCodeContext : CodeContext {
    private fun unsupported(any: Any? = null): Nothing = throw UnsupportedOperationException(any?.toString() ?: "")

    override fun genReturn(target: IrSymbolOwner, value: LLVMValueRef?) = unsupported(target)

    override fun genBreak(destination: IrBreak) = unsupported()

    override fun genContinue(destination: IrContinue) = unsupported()

    override val exceptionHandler get() = unsupported()

    override fun genThrow(exception: LLVMValueRef) = unsupported()

    override fun genDeclareVariable(variable: IrVariable, value: LLVMValueRef?, variableLocation: VariableDebugLocation?) = unsupported(variable)

    override fun getDeclaredVariable(variable: IrVariable) = -1

    override fun genGetValue(value: IrValueDeclaration) = unsupported(value)

    override fun functionScope(): CodeContext? = null

    override fun fileScope(): CodeContext? = null

    override fun classScope(): CodeContext? = null

    override fun addResumePoint(bbLabel: LLVMBasicBlockRef) = unsupported(bbLabel)

    override fun returnableBlockScope(): CodeContext? = null

    override fun location(line: Int, column: Int): LocationInfo? = unsupported()

    override fun scope(): DIScopeOpaqueRef? = unsupported()
}

/**
 * The [CodeContext] which can define some operations and delegate other ones to [outerContext]
 */
internal abstract class InnerScope(val outerContext: CodeContext) : CodeContext by outerContext