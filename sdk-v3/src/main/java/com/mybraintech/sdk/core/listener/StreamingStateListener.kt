package com.mybraintech.sdk.core.listener

import com.mybraintech.sdk.core.model.StreamingState

interface StreamingStateListener {
    fun onStreamingStateFetched(streamingState: StreamingState)
    fun onStreamingStateError(error: String)
}