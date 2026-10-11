package dev.breda.tap

enum class DeviceReadiness { UNSUPPORTED, DISABLED, READY }
fun deviceReadiness(hasNfc:Boolean,enabled:Boolean):DeviceReadiness=when { !hasNfc->DeviceReadiness.UNSUPPORTED;!enabled->DeviceReadiness.DISABLED;else->DeviceReadiness.READY }
