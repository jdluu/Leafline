package com.jdluu.leafline.library.rescan

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FolderRescanViewModelTest {

    @Test
    fun `rescan all exposes the latest summary and totals`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val runner = FakeRunner(
                summary = RescanSummary(
                    folderResults = listOf(
                        FolderRescanResult.Success(RescanCounts(added = 2, skipped = 1))
                    ),
                    totals = RescanCounts(added = 2, skipped = 1)
                )
            )
            val viewModel = FolderRescanViewModel(runner, ioDispatcher = dispatcher)

            viewModel.rescanAll(listOf("content://a"))
            testScheduler.advanceUntilIdle()

            assertEquals(
                FolderRescanUiState.Done(
                    RescanSummary(
                        folderResults = listOf(
                            FolderRescanResult.Success(RescanCounts(added = 2, skipped = 1))
                        ),
                        totals = RescanCounts(added = 2, skipped = 1)
                    )
                ),
                viewModel.state.value
            )
            assertEquals(listOf(listOf("content://a")), runner.allCalls)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `rescan one success becomes a one-folder summary with its counts`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val runner = FakeRunner(
                oneResult = FolderRescanResult.Success(RescanCounts(updated = 1))
            )
            val viewModel = FolderRescanViewModel(runner, ioDispatcher = dispatcher)

            viewModel.rescanOne("content://a")
            testScheduler.advanceUntilIdle()

            assertEquals(
                FolderRescanUiState.Done(
                    RescanSummary(
                        folderResults = listOf(FolderRescanResult.Success(RescanCounts(updated = 1))),
                        totals = RescanCounts(updated = 1)
                    )
                ),
                viewModel.state.value
            )
            assertEquals(listOf("content://a"), runner.oneCalls)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `rescan one failure surfaces a recoverable folder result`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val runner = FakeRunner(
                oneResult = FolderRescanResult.Failed(
                    folderUri = "content://revoked",
                    reason = "missing",
                    recoverable = true
                )
            )
            val viewModel = FolderRescanViewModel(runner, ioDispatcher = dispatcher)

            viewModel.rescanOne("content://revoked")
            testScheduler.advanceUntilIdle()

            val done = viewModel.state.value as FolderRescanUiState.Done
            assertEquals(1, done.summary.folderResults.size)
            val failed = done.summary.folderResults.single() as FolderRescanResult.Failed
            assertEquals(true, failed.recoverable)
            assertEquals(RescanCounts(), done.summary.totals)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `runner failure on rescan all becomes a terminal error, not stranded running`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val runner = ThrowingRunner
            val viewModel = FolderRescanViewModel(runner, ioDispatcher = dispatcher)

            viewModel.rescanAll(listOf("content://a"))
            testScheduler.advanceUntilIdle()

            val state = viewModel.state.value as FolderRescanUiState.Error
            assertEquals(true, state.message.isNotBlank())
            assertEquals(false, runner.allCalls.isEmpty())
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `runner failure on rescan one becomes a terminal error, not stranded running`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val runner = ThrowingRunner
            val viewModel = FolderRescanViewModel(runner, ioDispatcher = dispatcher)

            viewModel.rescanOne("content://a")
            testScheduler.advanceUntilIdle()

            val state = viewModel.state.value as FolderRescanUiState.Error
            assertEquals(true, state.message.isNotBlank())
            assertEquals(false, runner.oneCalls.isEmpty())
        } finally {
            Dispatchers.resetMain()
        }
    }

    private object ThrowingRunner : FolderRescanRunner {
        val allCalls = mutableListOf<List<String>>()
        val oneCalls = mutableListOf<String>()

        override suspend fun rescanOne(folderUri: String): FolderRescanResult {
            oneCalls += folderUri
            error("boom")
        }

        override suspend fun rescanAll(folderUris: List<String>): RescanSummary {
            allCalls += folderUris
            error("boom")
        }
    }

    private class FakeRunner(
        private val summary: RescanSummary = RescanSummary(emptyList(), RescanCounts()),
        private val oneResult: FolderRescanResult =
            FolderRescanResult.Success(RescanCounts())
    ) : FolderRescanRunner {
        val allCalls = mutableListOf<List<String>>()
        val oneCalls = mutableListOf<String>()

        override suspend fun rescanOne(folderUri: String): FolderRescanResult {
            oneCalls += folderUri
            return oneResult
        }

        override suspend fun rescanAll(folderUris: List<String>): RescanSummary {
            allCalls += folderUris
            return summary
        }
    }
}
