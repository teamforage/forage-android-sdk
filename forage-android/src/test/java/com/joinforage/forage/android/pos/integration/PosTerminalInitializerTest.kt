package com.joinforage.forage.android.pos.integration

import com.joinforage.forage.android.core.services.EnvConfig
import com.joinforage.forage.android.core.services.ForageConfig
import com.joinforage.forage.android.core.services.generateTraceId
import com.joinforage.forage.android.core.services.telemetry.LogAttributes
import com.joinforage.forage.android.core.services.telemetry.Loggable
import com.joinforage.forage.android.core.services.telemetry.extractTenantIdFromToken
import com.joinforage.forage.android.pos.integration.base64.JavaBase64Util
import com.joinforage.forage.android.pos.integration.forageapi.getAccessToken
import com.joinforage.forage.android.pos.integration.forageapi.getSessionToken
import com.joinforage.forage.android.pos.integration.logger.InMemoryLogger
import com.joinforage.forage.android.pos.services.encryption.AesBlock
import com.joinforage.forage.android.pos.services.encryption.certificate.IRsaKeyManager
import com.joinforage.forage.android.pos.services.encryption.dukpt.DukptService
import com.joinforage.forage.android.pos.services.encryption.dukpt.IDukptService
import com.joinforage.forage.android.pos.services.encryption.dukpt.KsnComponent
import com.joinforage.forage.android.pos.services.encryption.dukpt.WorkingKey
import com.joinforage.forage.android.pos.services.encryption.storage.IKsnFileManager
import com.joinforage.forage.android.pos.services.encryption.storage.InMemoryKeyRegisters
import com.joinforage.forage.android.pos.services.encryption.storage.KeySerialNumber
import com.joinforage.forage.android.pos.services.init.IRosettaInitService
import com.joinforage.forage.android.pos.services.init.InitialPosState
import com.joinforage.forage.android.pos.services.init.PosTerminalInitializer
import com.joinforage.forage.android.pos.services.init.SignedCertificate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.bouncycastle.jcajce.provider.util.BadBlockException
import org.junit.Assert.assertThrows
import org.junit.BeforeClass
import org.junit.Test

class PosTerminalInitializerTest {

    companion object {
        private val merchantRef = "e6b746712a" // "c67e8569c1"
        private val posTerminalId = "HeadlessAndroidIntegrationTests"
        private val username = "o3vJFaHmO3eOGLxhREmwk7GHIAD4k7E9WTOwGeUP"
        private val password = "BrqSz3vDhb98nwW2wJ7OpZtx5eQYTKuJGhAD4BxSKKk0yvBNjBy6yVArn1wpFQJX618yo2oA4PUCyRWJj4SflMuhPGSGj4kaJXK158uMJvOdtT5CU4uVyeopfpx3ooDx"
        private val env = EnvConfig.Dev
        private val traceId = generateTraceId()
        private val keyRegisters = InMemoryKeyRegisters()

        private lateinit var forageConfig: ForageConfig
        private lateinit var accessToken: String

        @BeforeClass
        @JvmStatic
        fun setupClass() = runBlocking {
            println("The Trace ID for this test run is: $traceId")
            accessToken = getAccessToken(username, password, env)
            val sessionToken = getSessionToken(accessToken, merchantRef)
            forageConfig = ForageConfig(merchantRef, sessionToken)
        }

        private fun createBaseStubKsnManager(isInitialized: Boolean = false) = object : IKsnFileManager {
            override fun init(initialKeyId: String): Boolean = true
            override fun isInitialized(): Boolean = isInitialized
            override fun readBaseDerivationKeyId(): KsnComponent? = null
            override fun readDeviceDerivationId(): KsnComponent? = null
            override fun readDukptClientTxCount(): KsnComponent? = null
            override fun readAll(): KeySerialNumber = throw NotImplementedError()
            override fun updateKsn(nextKsnState: KeySerialNumber) {}
        }

        private fun createBaseStubRsaKeyManager() = object : IRsaKeyManager {
            override fun encrypt(str: String): ByteArray = ByteArray(0)
            override fun encrypt(data: ByteArray): ByteArray = ByteArray(0)
            override fun decrypt(encryptedData: ByteArray): ByteArray = ByteArray(16)
            override fun generateCSRBase64(): String = ""
        }

        private fun createSuccessfulRosetta() = object : IRosettaInitService {
            override suspend fun signCertificate(csr: String): SignedCertificate {
                return SignedCertificate("test-certificate")
            }
            override suspend fun initializePos(base64PublicKeyPEM: String): InitialPosState {
                return InitialPosState(
                    base64EncryptedIpek = "dGVzdGlwZWs=", // Base64 encoded "testipek"
                    checksum = "test-checksum",
                    checksumAlgorithm = "test-algo",
                    keySerialNumber = "0123456789abcdef"
                )
            }
        }
    }

    // Helper function to create initializer with common parameters
    private fun createInitializer(
        ksnManager: IKsnFileManager,
        rosetta: IRosettaInitService,
        rsaKeyManager: IRsaKeyManager = createBaseStubRsaKeyManager(),
        logger: InMemoryLogger = InMemoryLogger(
            LogAttributes(
                forageConfig,
                traceId,
                posTerminalId
            )
        ),
        dukptServiceFactory: (KeySerialNumber) -> IDukptService = {
                ksn ->
            DukptService(ksn, keyRegisters)
        }
    ) = PosTerminalInitializer(
        ksnManager,
        logger,
        rosetta,
        JavaBase64Util(),
        rsaKeyManager,
        dukptServiceFactory
    )

    // Helper function to verify error logs
    private fun verifyErrorLogs(logger: InMemoryLogger, exception: Exception) {
        val attrs = LogAttributes(
            forageConfig,
            traceId,
            posTerminalId,
            tenantId = extractTenantIdFromToken(
                JavaBase64Util(),
                forageConfig
            )
        ).toMap()
        val expectedLogs = listOf(
            Loggable.Info("", "[START] POS Initialization", attrs),
            Loggable.Error("", "[END] Failed to initialize the ForageTerminalSDK", exception, attrs)
        )
        assertThat(logger.logs).isEqualTo(expectedLogs)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testCompletesFullInit() = runTest {
        val stubKsnManager = createBaseStubKsnManager()
        val logger = InMemoryLogger(
            LogAttributes(
                forageConfig,
                traceId,
                posTerminalId
            )
        )

        val initializer = createInitializer(ksnManager = stubKsnManager, rosetta = createSuccessfulRosetta(), logger = logger)
        val result = initializer.safeInit()

        assertThat(result).isEqualTo(true)

        val attrs = LogAttributes(
            forageConfig,
            traceId,
            posTerminalId,
            tenantId = extractTenantIdFromToken(JavaBase64Util(), forageConfig)
        ).toMap()
        val expectedLogs = listOf(
            Loggable.Info("", "[START] POS Initialization", attrs),
            Loggable.Info("", "[END] ForageTerminalSDK is now initialized", attrs)
        )
        assertThat(logger.logs).isEqualTo(expectedLogs)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testExitEarlyIfAlreadyInitialized() = runTest {
        val stubKsnManager = createBaseStubKsnManager(isInitialized = true)
        val logger = InMemoryLogger(
            LogAttributes(
                forageConfig,
                traceId,
                posTerminalId
            )
        )

        val initializer = createInitializer(ksnManager = stubKsnManager, rosetta = createSuccessfulRosetta(), logger = logger)
        val result = initializer.safeInit()

        assertThat(result).isEqualTo(false)

        val attrs = LogAttributes(
            forageConfig,
            traceId,
            posTerminalId,
            tenantId = extractTenantIdFromToken(JavaBase64Util(), forageConfig)
        ).toMap()
        val expectedLogs = listOf(
            Loggable.Info("", "[START] POS Initialization", attrs),
            Loggable.Info("", "[END] Already initialized; skipping.", attrs)
        )
        assertThat(logger.logs).isEqualTo(expectedLogs)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testFailedCsrGenerationRequest() = runTest {
        val exception = Exception("some error")
        val failingRosetta = object : IRosettaInitService {
            override suspend fun signCertificate(csr: String): SignedCertificate {
                throw exception
            }
            override suspend fun initializePos(base64PublicKeyPEM: String): InitialPosState {
                throw exception
            }
        }

        val logger = InMemoryLogger(
            LogAttributes(
                forageConfig,
                traceId,
                posTerminalId
            )
        )
        val initializer = createInitializer(
            createBaseStubKsnManager(),
            failingRosetta,
            logger = logger
        )

        assertThrows(PosTerminalInitializer.ForageInitException::class.java) {
            runBlocking { initializer.safeInit() }
        }

        verifyErrorLogs(logger, exception)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testFailedPosInitializationRequest() = runTest {
        // Create a test exception and failing Rosetta that succeeds for CSR but fails for POS init
        val exception = Exception("pos initialization error")
        val failingRosetta = object : IRosettaInitService {
            override suspend fun signCertificate(csr: String): SignedCertificate {
                return SignedCertificate("test-certificate")
            }
            override suspend fun initializePos(base64PublicKeyPEM: String): InitialPosState {
                throw exception
            }
        }

        val logger = InMemoryLogger(
            LogAttributes(
                forageConfig,
                traceId,
                posTerminalId
            )
        )
        val initializer = createInitializer(
            ksnManager = createBaseStubKsnManager(),
            rosetta = failingRosetta,
            logger = logger
        )

        // Call safeInit and verify it throws the expected exception
        assertThrows(PosTerminalInitializer.ForageInitException::class.java) {
            runBlocking { initializer.safeInit() }
        }

        verifyErrorLogs(logger, exception)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testKsnInitFails() = runTest {
        val exception = Exception("ksn init error")

        // Create KsnFileManager that fails on init
        val failingKsnManager = object : IKsnFileManager by createBaseStubKsnManager() {
            override fun init(initialKeyId: String): Boolean {
                throw exception
            }
        }

        val logger = InMemoryLogger(
            LogAttributes(
                forageConfig,
                traceId,
                posTerminalId
            )
        )
        val initializer = createInitializer(
            ksnManager = failingKsnManager,
            rosetta = createSuccessfulRosetta(),
            logger = logger
        )

        assertThrows(PosTerminalInitializer.ForageInitException::class.java) {
            runBlocking { initializer.safeInit() }
        }

        verifyErrorLogs(logger, exception)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testFailToDecryptIpek() = runTest {
        val exception = BadBlockException("unable to decrypt block", Exception("test"))

        val stubRsaKeyManager = object : IRsaKeyManager {
            override fun encrypt(str: String): ByteArray = ByteArray(0)
            override fun encrypt(data: ByteArray): ByteArray = ByteArray(0)
            override fun decrypt(encryptedData: ByteArray): ByteArray {
                throw exception
            }
            override fun generateCSRBase64(): String = ""
        }

        val logger = InMemoryLogger(
            LogAttributes(
                forageConfig,
                traceId,
                posTerminalId
            )
        )
        val initializer = createInitializer(
            ksnManager = createBaseStubKsnManager(),
            rosetta = createSuccessfulRosetta(),
            logger = logger,
            rsaKeyManager = stubRsaKeyManager
        )

        assertThrows(PosTerminalInitializer.ForageInitException::class.java) {
            runBlocking { initializer.safeInit() }
        }

        verifyErrorLogs(logger, exception)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testFailToLoadDukptKey() = runTest {
        val exception = Exception("failed to load DUKPT key")

        val failingDukptServiceFactory: (KeySerialNumber) -> IDukptService = { ksn ->
            object : IDukptService {
                override fun loadKey(initialDerivationKeyMaterial: AesBlock): KeySerialNumber {
                    throw exception
                }
                override fun generateWorkingKey(): Pair<WorkingKey, KeySerialNumber> {
                    throw NotImplementedError()
                }
            }
        }

        val logger = InMemoryLogger(
            LogAttributes(
                forageConfig,
                traceId,
                posTerminalId
            )
        )
        val initializer = createInitializer(
            ksnManager = createBaseStubKsnManager(),
            rosetta = createSuccessfulRosetta(),
            logger = logger,
            dukptServiceFactory = failingDukptServiceFactory
        )

        assertThrows(PosTerminalInitializer.ForageInitException::class.java) {
            runBlocking { initializer.safeInit() }
        }

        verifyErrorLogs(logger, exception)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testFailToUpdateKsn() = runTest {
        val exception = Exception("failed to update KSN")

        // Create stub KsnFileManager that throws on updateKsn
        val failingKsnManager = object : IKsnFileManager by createBaseStubKsnManager() {
            override fun updateKsn(nextKsnState: KeySerialNumber) {
                throw exception
            }
        }

        val logger = InMemoryLogger(
            LogAttributes(
                forageConfig,
                traceId,
                posTerminalId
            )
        )
        val initializer = createInitializer(
            ksnManager = failingKsnManager,
            rosetta = createSuccessfulRosetta(),
            logger = logger
        )

        assertThrows(PosTerminalInitializer.ForageInitException::class.java) {
            runBlocking { initializer.safeInit() }
        }

        verifyErrorLogs(logger, exception)
    }
}
