package com.mybraintech.sdk.core

@RequiresOptIn(level = RequiresOptIn.Level.WARNING, message = "This API is experimental. It can be incompatibly changed in the future.")
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class LabStreamingLayer

@RequiresOptIn(level = RequiresOptIn.Level.WARNING, message = "This API is reserved for Test Bench application and should not be used outside.")
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class TestBench

@RequiresOptIn(level = RequiresOptIn.Level.WARNING, message = "This API is reserved for ResearchStudy application.")
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class ResearchStudy
