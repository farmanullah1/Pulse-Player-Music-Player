package com.pulse.player.net

import android.content.Context
import com.pulse.player.data.settings.SettingsStore

object NetworkGatewayFactory {
    fun create(context: Context, settings: SettingsStore): NetworkGateway =
        FullNetworkGateway(context, settings)
}
