package com.joinforage.forage.android.ecom.services

import android.content.Context
import com.joinforage.forage.android.core.services.ForageConfig
import com.joinforage.forage.android.core.services.forageapi.engine.IHttpEngine
import com.joinforage.forage.android.core.services.forageapi.payment.IPaymentService
import com.joinforage.forage.android.core.services.forageapi.payment.PaymentService
import com.joinforage.forage.android.core.services.telemetry.LogLogger
import com.joinforage.forage.android.core.services.telemetry.UserAction
import com.joinforage.forage.android.core.services.vault.ISecurePinCollector
import com.joinforage.forage.android.core.services.vault.errors.IErrorStrategy
import com.joinforage.forage.android.core.services.vault.metrics.IMetricsRecorder
import com.joinforage.forage.android.core.services.vault.metrics.VaultMetricsRecorder
import com.joinforage.forage.android.core.services.vault.requests.ISubmitRequestBuilder
import com.joinforage.forage.android.core.services.vault.submission.PinSubmission
import com.joinforage.forage.android.ecom.services.forageapi.engine.EcomOkHttpEngine
import com.joinforage.forage.android.ecom.services.forageapi.paymentmethod.IPaymentMethodService
import com.joinforage.forage.android.ecom.services.forageapi.paymentmethod.PaymentMethodService
import com.joinforage.forage.android.ecom.services.telemetry.EcomDatadogLoggerFactory
import com.joinforage.forage.android.ecom.services.vault.submission.EcomBalanceCheckSubmission
import com.joinforage.forage.android.ecom.services.vault.submission.EcomCapturePaymentSubmission
import com.joinforage.forage.android.ecom.services.vault.submission.EcomDeferCapturePaymentSubmission
import dagger.Binds
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import dagger.Provides
import dagger.assisted.AssistedFactory
import javax.inject.Named

@Component(
    modules = [
        ForageSDKApiHttpEngineModule::class,
        ForageSDKVaultHttpEngineModule::class,
        ForageSDKLoggerModule::class,
        ForageSDKBindingModule::class
    ]
)
internal abstract class ForageSDKComponent {
    @Component.Builder
    interface Builder {
        @BindsInstance fun forageConfig(forageConfig: ForageConfig): Builder

        @BindsInstance fun customerId(@Named("customerId") customerId: String?): Builder

        @BindsInstance fun context(context: Context?): Builder

        @BindsInstance fun securePinCollector(securePinCollector: ISecurePinCollector?): Builder

        @BindsInstance
        @Named("vault_override")
        fun vaultHttpEngineOverride(vaultHttpEngineOverride: IHttpEngine?): Builder

        fun build(): ForageSDKComponent
    }

    abstract fun inject(forageSDK: ForageSDK)
}

@AssistedFactory
internal interface EcomBalanceCheckSubmissionFactory {
    fun build(paymentMethodRef: String): EcomBalanceCheckSubmission
}

@AssistedFactory
internal interface EcomCapturePaymentSubmissionFactory {
    fun build(paymentRef: String): EcomCapturePaymentSubmission
}

@AssistedFactory
internal interface EcomDeferCapturePaymentSubmissionFactory {
    fun build(paymentRef: String): EcomDeferCapturePaymentSubmission
}

@AssistedFactory
internal interface PinSubmissionFactory {
    fun build(
        errorStrategy: IErrorStrategy,
        requestBuilder: ISubmitRequestBuilder,
        userAction: UserAction
    ): PinSubmission
}

@Module
internal class ForageSDKApiHttpEngineModule {
    @Provides
    @Named("api")
    internal fun provideHttpEngine(): IHttpEngine {
        return EcomOkHttpEngine()
    }
}

@Module
internal class ForageSDKVaultHttpEngineModule {
    @Provides
    @Named("vault")
    internal fun provideVaultHttpEngine(): IHttpEngine {
        return EcomOkHttpEngine()
    }
}

@Module
internal class ForageSDKLoggerModule {
    companion object {
        var singletonInstance: LogLogger? = null
    }

    @Provides
    // @Singleton // Mysterious compilation errors
    internal fun provideLogLogger(loggerFactory: EcomDatadogLoggerFactory): LogLogger {
        synchronized(this) {
            if (singletonInstance == null) {
                singletonInstance = loggerFactory.makeLogger()
            }
            return singletonInstance!!
        }
    }
}

@Module
internal abstract class ForageSDKBindingModule {
    @Binds
    internal abstract fun providePaymentMethodService(paymentMethodService: PaymentMethodService): IPaymentMethodService

    @Binds
    internal abstract fun providePaymentService(paymentService: PaymentService): IPaymentService

    @Binds
    internal abstract fun provideMetricsRecorder(metricsRecorder: VaultMetricsRecorder): IMetricsRecorder
}
