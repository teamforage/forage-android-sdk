package com.joinforage.forage.android.ecom.services

import android.content.Context
import com.joinforage.forage.android.core.logger.InMemoryLogger
import com.joinforage.forage.android.core.services.ForageConfig
import com.joinforage.forage.android.core.services.forageapi.engine.IHttpEngine
import com.joinforage.forage.android.core.services.forageapi.payment.IPaymentService
import com.joinforage.forage.android.core.services.generateTraceId
import com.joinforage.forage.android.core.services.telemetry.LogAttributes
import com.joinforage.forage.android.core.services.telemetry.LogLogger
import com.joinforage.forage.android.core.services.vault.ISecurePinCollector
import com.joinforage.forage.android.ecom.services.forageapi.paymentmethod.IPaymentMethodService
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import dagger.Provides
import javax.inject.Named

@Component(
    modules = [
        ForageSDKApiHttpEngineModule::class,
        ForageSDKVaultHttpEngineTestModule::class,
        ForageSDKLoggerTestModule::class,
        ForageSDKBindingModule::class
    ]
)
internal abstract class ForageSDKTestComponent : ForageSDKComponent() {

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun forageConfig(forageConfig: ForageConfig): Builder

        @BindsInstance
        fun customerId(@Named("customerId") customerId: String?): Builder

        @BindsInstance
        fun context(context: Context?): Builder

        @BindsInstance
        fun securePinCollector(securePinCollector: ISecurePinCollector?): Builder

        @BindsInstance
        fun vaultHttpEngineOverride(@Named("vault_override") vaultHttpEngineOverride: IHttpEngine?): Builder

        fun build(): ForageSDKTestComponent
    }

    abstract fun getPinSubmissionFactory(): PinSubmissionFactory
    abstract fun getLogger(): LogLogger
    abstract fun getPaymentMethodService(): IPaymentMethodService
    abstract fun getPaymentService(): IPaymentService
}

@Module
internal class ForageSDKLoggerTestModule {
    companion object {
        var singletonInstance: LogLogger? = null
    }

    @Provides
    // @Singleton // Mysterious compilation errors
    internal fun provideLogLogger(forageConfig: ForageConfig): LogLogger {
        synchronized(this) {
            if (singletonInstance == null) {
                singletonInstance = InMemoryLogger(LogAttributes(forageConfig, generateTraceId()))
            }
            return singletonInstance!!
        }
    }
}

@Module
internal class ForageSDKVaultHttpEngineTestModule {
    @Provides
    @Named("vault")
    internal fun provideVaultHttpEngine(
        @Named("api") apiHttpEngine: IHttpEngine,
        @Named("vault_override") vaultHttpEngineOverride: IHttpEngine?
    ): IHttpEngine {
        return vaultHttpEngineOverride ?: apiHttpEngine
    }
}
