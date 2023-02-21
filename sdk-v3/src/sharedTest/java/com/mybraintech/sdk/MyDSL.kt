@file:Suppress("unused")

package com.mybraintech.sdk

@DslMarker @Target(AnnotationTarget.CLASS)
annotation class MyDSL

@MyDSL
object Gwen
@MyDSL
object Arranger
@MyDSL
object Actor
@MyDSL
object Asserter

inline fun gwen(block : Gwen.() -> Unit) = block(Gwen)
inline fun Gwen.given(block : Arranger.() -> Unit) = block(Arranger)
inline fun Gwen.whenever(block : Actor.() -> Unit) = block(Actor)
inline fun Gwen.then(block : Asserter.() -> Unit) = block(Asserter)
