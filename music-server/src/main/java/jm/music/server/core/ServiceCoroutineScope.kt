package jm.music.server.core

import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.CoroutineContext

abstract class ServiceCoroutineScope internal constructor() : CoroutineScope

internal class ServiceCoroutineScopeImpl(
    override val coroutineContext: CoroutineContext
) : ServiceCoroutineScope()
