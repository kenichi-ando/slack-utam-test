import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import utam.core.driver.DriverType
import utam.core.framework.base.RootPageObject
import utam.core.framework.consumer.UtamLoaderConfigImpl
import utam.core.framework.consumer.UtamLoaderImpl
import utam.core.selenium.factory.WebDriverFactory
import utam.slack.pageobjects.*
import java.util.*
import kotlin.reflect.KClass
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SampleSlackUiTest {
    private val driver = WebDriverFactory.getWebDriver(DriverType.chrome)
    private val loader = UtamLoaderImpl(UtamLoaderConfigImpl(), driver)
    private val props = javaClass.classLoader.getResourceAsStream("env.properties").use {
        Properties().apply { load(it) }
    }

    private fun <T : RootPageObject> from(type: KClass<T>): T = loader.load(type.java)

    @BeforeAll
    fun initialize() {
        driver.get(props.getProperty("slack.workspace.url"))
        from(Login::class).login(props.getProperty("slack.user.email"), props.getProperty("slack.user.password"))
        from(Redirect::class).waitAndClick()
    }

    @AfterAll
    fun terminate() {
        driver.quit()
    }

    @ParameterizedTest
    @ValueSource(strings = ["Hello", "こんにちは"])
    fun postMessage(message: String) {
        val client = from(Desktop::class).client
        client.generalChannel.click()
        client.messageInput.clearAndType(message)
        client.textSendButton.click()
        assertNotNull(client.messageTexts)
        assertEquals(message, client.messageTexts.last().text)
        Thread.sleep(2000)
    }

    @ParameterizedTest
    @ValueSource(strings = ["Hello", "こんにちは"])
    fun searchOnTopNavigation(message: String) {
        val desktop = from(Desktop::class)
        desktop.client.topNavSearch.click()
        desktop.searchModal.search(message)
        for (result in desktop.client.waitForSearchResults()) {
            println(result.text)
            assertTrue(result.text.uppercase().contains(message.uppercase()))
        }
        Thread.sleep(2000)
    }
}
