package dev.azure.desktop.domain.auth

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VerifyAndStorePatUseCaseTest {
    @Test
    fun returnsFailureWhenOrganizationIsBlank() =
        runBlocking {
            val verifier = RecordingVerifier(Result.success(Unit))
            val storage = RecordingStorage()
            val useCase = VerifyAndStorePatUseCase(verifier, storage)

            val result = useCase("   ", "pat")

            assertTrue(result.isFailure)
            assertEquals("Enter an Azure DevOps organization.", result.exceptionOrNull()?.message)
            assertFalse(verifier.wasCalled)
        }

    @Test
    fun returnsFailureWhenPatIsBlank() =
        runBlocking {
            val verifier = RecordingVerifier(Result.success(Unit))
            val storage = RecordingStorage()
            val useCase = VerifyAndStorePatUseCase(verifier, storage)

            val result = useCase("org", "   ")

            assertTrue(result.isFailure)
            assertEquals("Enter a personal access token.", result.exceptionOrNull()?.message)
            assertFalse(verifier.wasCalled)
        }

    @Test
    fun verifiesAndStoresTrimmedValues() =
        runBlocking {
            val verifier = RecordingVerifier(Result.success(Unit))
            val storage = RecordingStorage()
            val useCase = VerifyAndStorePatUseCase(verifier, storage)

            val result = useCase(" org ", " pat ")

            assertTrue(result.isSuccess)
            assertEquals("org", verifier.lastOrganization)
            assertEquals("pat", verifier.lastPat)
            assertEquals("pat", storage.savedPat)
            assertEquals("org", storage.savedOrganization)
        }

    @Test
    fun propagatesVerifierFailureWithoutSaving() =
        runBlocking {
            val verifier = RecordingVerifier(Result.failure(IllegalStateException("invalid")))
            val storage = RecordingStorage()
            val useCase = VerifyAndStorePatUseCase(verifier, storage)

            val result = useCase("org", "pat")

            assertTrue(result.isFailure)
            assertEquals("invalid", result.exceptionOrNull()?.message)
            assertEquals(null, storage.savedPat)
            assertEquals(null, storage.savedOrganization)
        }
}

private class RecordingVerifier(
    private val result: Result<Unit>,
) : PatVerifier {
    var wasCalled = false
    var lastOrganization: String? = null
    var lastPat: String? = null

    override suspend fun verify(organization: String, pat: String): Result<Unit> {
        wasCalled = true
        lastOrganization = organization
        lastPat = pat
        return result
    }
}

private class RecordingStorage : PatStorage {
    var savedPat: String? = null
    var savedOrganization: String? = null

    override fun savePat(pat: String): Result<Unit> {
        savedPat = pat
        return Result.success(Unit)
    }

    override fun loadPat(): String? = savedPat

    override fun saveOrganization(name: String): Result<Unit> {
        savedOrganization = name
        return Result.success(Unit)
    }

    override fun loadOrganization(): String? = savedOrganization

    override fun clearCredentials() {
        savedPat = null
        savedOrganization = null
    }

    override fun clearPatOnly() {
        savedPat = null
    }
}
