package io.mailguru.whois

import java.lang.reflect.Modifier
import kotlin.reflect.full.functions
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

class TestUtil private constructor() {
    companion object {
        fun getResourceAsText(path: String): String? = object {}.javaClass.getResource(path)?.readText()
    }
}

fun Any.mockPrivateFields(vararg mocks: Any): Any {
    mocks.forEach { mock ->
        javaClass.declaredFields
            .filter { it.modifiers.and(Modifier.PRIVATE) > 0 || it.modifiers.and(Modifier.PROTECTED) > 0 }
            .firstOrNull { it.type == mock.javaClass }
            ?.also { it.isAccessible = true }
            ?.set(this, mock)
    }

    return this
}

fun Any.mockFieldByName(name: String, mock: Any): Any {
    javaClass.declaredFields
        .firstOrNull { it.name == name }
        ?.also { it.isAccessible = true }
        ?.set(this, mock)

    return this
}

inline fun <reified T : Any, R> T.getPrivateProperty(name: String): R? = T::class
    .memberProperties
    .firstOrNull { it.name == name }
    ?.apply { isAccessible = true }
    ?.get(this) as? R

inline fun <reified T : Any, R> T.callPrivateMethod(name: String, vararg args: Any?): R? = T::class
    .functions
    .firstOrNull { it.name == name }
    ?.apply { isAccessible = true }
    ?.call(this, *args) as? R
