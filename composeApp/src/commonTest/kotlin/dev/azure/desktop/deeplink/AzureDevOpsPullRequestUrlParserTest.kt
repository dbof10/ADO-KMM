package dev.azure.desktop.deeplink

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AzureDevOpsPullRequestUrlParserTest {
    @Test
    fun parseStandardPullRequestUrl() {
        val link =
            parseAzureDevOpsPullRequestUrl(
                "https://dev.azure.com/fabrikam/fiber/_git/core/pullrequest/42",
            )
        assertEquals(
            AzureDevOpsPullRequestLink("fabrikam", "fiber", "core", 42),
            link,
        )
    }

    @Test
    fun parseToleratesPullRequestsPluralAndSurroundingNoise() {
        val link =
            parseAzureDevOpsPullRequestUrl(
                """see https://dev.azure.com/org/proj/_git/repo/pullrequests/7?a=1#frag""",
            )
        assertEquals(
            AzureDevOpsPullRequestLink("org", "proj", "repo", 7),
            link,
        )
    }

    @Test
    fun parseDecodesPercentEncodedSegments() {
        val link =
            parseAzureDevOpsPullRequestUrl(
                "https://dev.azure.com/my%20org/my%20project/_git/my%20repo/pullrequest/3",
            )
        assertEquals(
            AzureDevOpsPullRequestLink("my org", "my project", "my repo", 3),
            link,
        )
    }

    @Test
    fun parseRejectsNonDevAzureHost() {
        assertNull(parseAzureDevOpsPullRequestUrl("https://example.com/a/b/_git/c/pullrequest/1"))
    }

    @Test
    fun parseRejectsInvalidId() {
        assertNull(parseAzureDevOpsPullRequestUrl("https://dev.azure.com/o/p/_git/r/pullrequest/0"))
        assertNull(parseAzureDevOpsPullRequestUrl("https://dev.azure.com/o/p/_git/r/pullrequest/x"))
    }

    @Test
    fun webUrlEncodesSegmentsAndTrims() {
        val url =
            azureDevOpsPullRequestWebUrl(
                organization = " my org ",
                projectName = "p",
                repositoryName = "r",
                pullRequestId = 9,
            )
        assertEquals("https://dev.azure.com/my%20org/p/_git/r/pullrequest/9", url)
    }

    @Test
    fun encodeUrlPathSegmentEncodesNonAscii() {
        assertEquals("%C3%A9", encodeUrlPathSegment("é"))
    }

    @Test
    fun percentDecodeHandlesPlusAndInvalidEscape() {
        assertEquals("a b", percentDecode("a+b"))
        assertEquals("a%zzb", percentDecode("a%zzb"))
    }
}
