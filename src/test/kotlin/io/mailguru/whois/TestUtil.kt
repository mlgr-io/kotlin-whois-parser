package io.mailguru.whois

import kotlin.reflect.full.functions
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

object TestUtil {
    fun getResourceAsText(path: String): String? = object {}.javaClass.getResource(path)?.readText()
}

inline fun <reified T : Any, reified R> T.getPrivateProperty(name: String): R? = T::class
    .memberProperties
    .firstOrNull { it.name == name }
    ?.apply { isAccessible = true }
    ?.get(this) as? R

inline fun <reified T : Any, reified R> T.callPrivateMethod(name: String, vararg args: Any?): R? = T::class
    .functions
    .firstOrNull { it.name == name }
    ?.apply { isAccessible = true }
    ?.call(this, *args) as? R
