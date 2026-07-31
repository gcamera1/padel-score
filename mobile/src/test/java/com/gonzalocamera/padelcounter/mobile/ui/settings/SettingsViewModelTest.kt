package com.gonzalocamera.padelcounter.mobile.ui.settings

import com.google.common.truth.Truth.assertThat
import com.gonzalocamera.padelcounter.mobile.MainDispatcherRule
import com.gonzalocamera.padelcounter.mobile.data.FakeMatchRepository
import com.gonzalocamera.padelcounter.shared.Decider
import com.gonzalocamera.padelcounter.shared.Match
import com.gonzalocamera.padelcounter.shared.MatchOrigin
import com.gonzalocamera.padelcounter.shared.Winner
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private fun match(id: String) = Match(
        id = id,
        startedAt = 1700000000000L,
        finishedAt = 1700003600000L,
        setsScore = listOf(listOf(6, 4), listOf(6, 3)),
        tieBreakUsed = false,
        decider = Decider.TB7,
        winner = Winner.MY,
        origin = MatchOrigin.MOBILE,
    )

    @Test
    fun `export then import into an empty history restores everything`() = runTest {
        val source = FakeMatchRepository(initialMatches = listOf(match("a"), match("b")))
        val exported = SettingsViewModel(source).exportJson(nowMillis = 123L)
        assertThat(exported.matchCount).isEqualTo(2)

        val target = FakeMatchRepository()
        val result = SettingsViewModel(target).importJson(exported.json)

        assertThat(result).isEqualTo(ImportResult.Success(imported = 2, skipped = 0))
        assertThat(target.insertCount).isEqualTo(2)
    }

    @Test
    fun `importing twice does not duplicate`() = runTest {
        val source = FakeMatchRepository(initialMatches = listOf(match("a"), match("b")))
        val json = SettingsViewModel(source).exportJson(nowMillis = 1L).json

        val target = FakeMatchRepository()
        val vm = SettingsViewModel(target)
        vm.importJson(json)
        val second = vm.importJson(json)

        assertThat(second).isEqualTo(ImportResult.Success(imported = 0, skipped = 2))
        assertThat(target.insertCount).isEqualTo(2)
    }

    @Test
    fun `import merges without dropping what was already there`() = runTest {
        val source = FakeMatchRepository(initialMatches = listOf(match("a"), match("b")))
        val json = SettingsViewModel(source).exportJson(nowMillis = 1L).json

        // El destino ya tiene "a" y además uno propio que no está en el backup.
        val target = FakeMatchRepository(initialMatches = listOf(match("a"), match("z")))
        val result = SettingsViewModel(target).importJson(json)

        assertThat(result).isEqualTo(ImportResult.Success(imported = 1, skipped = 1))
        assertThat(target.matchHistory.first().map { it.id })
            .containsExactly("a", "z", "b")
    }

    @Test
    fun `exporting an empty history is valid`() = runTest {
        val repo = FakeMatchRepository()
        val vm = SettingsViewModel(repo)

        val exported = vm.exportJson(nowMillis = 1L)

        assertThat(exported.matchCount).isEqualTo(0)
        assertThat(vm.importJson(exported.json))
            .isEqualTo(ImportResult.Success(imported = 0, skipped = 0))
    }

    @Test
    fun `an invalid file reports the reason and inserts nothing`() = runTest {
        val repo = FakeMatchRepository()

        val result = SettingsViewModel(repo).importJson("no soy un backup")

        assertThat(result).isInstanceOf(ImportResult.Invalid::class.java)
        assertThat((result as ImportResult.Invalid).reason).contains("no es un historial")
        assertThat(repo.insertCount).isEqualTo(0)
    }

    @Test
    fun `an archive from a newer version is rejected`() = runTest {
        val repo = FakeMatchRepository()

        val result = SettingsViewModel(repo)
            .importJson("""{"version":99,"exportedAt":1,"matches":[]}""")

        assertThat((result as ImportResult.Invalid).reason).contains("versión más nueva")
    }
}
