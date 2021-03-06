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
import utam.slack.pageobjects.Desktop
import utam.slack.pageobjects.Login
import utam.slack.pageobjects.Redirect
import java.util.*
import kotlin.reflect.KClass
import kotlin.test.assertEquals
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
        from(Login::class).run {
            email.clearAndType(props.getProperty("slack.user.email"))
            password.clearAndType(props.getProperty("slack.user.password"))
            signin.click()
        }
        from(Redirect::class).run {
            waitFor { link.isVisible }
            link.click()
        }
    }

    @AfterAll
    fun terminate() {
        driver.quit()
    }

    @ParameterizedTest
    @ValueSource(strings = ["Hello", "こんにちは"])
    fun postMessage(message: String) {
        from(Desktop::class).run {
            generalChannel.click()
            messageInputContainer.run {
                messageInput.clearAndType(message)
                textSendButton.click()
            }
            messages.last().waitForAbsence()
            assertEquals(message, messages.last().messageText.text)
        }
        Thread.sleep(1000)
    }

    @ParameterizedTest
    @ValueSource(strings = ["Hello", "こんにちは"])
    fun replyMessage(message: String) {
        from(Desktop::class).run {
            generalChannel.click()
            messageInputContainer.run {
                messageInput.clearAndType(message)
                textSendButton.click()
            }
            messages.last().waitForAbsence()
            messages.last().run {
                messageText.moveTo()
                startThread.click()
            }
            waitFor { replyContainer.isVisible }
            replyContainer.run {
                messageInput.clearAndType("Re: " + message)
                textSendButton.click()
            }
            closeFlexpaneButton.click()
            waitFor { messages.last().replyLink.isVisible }
            assertEquals("1 reply", messages.last().replyLink.text)
        }
        Thread.sleep(1000)
    }

    @ParameterizedTest
    @ValueSource(strings = ["Hello", "こんにちは"])
    fun searchOnTopNavigation(message: String) {
        from(Desktop::class).run {
            topNavSearch.click()
            searchInput.run {
                clearAndType(message)
                press("ENTER")
            }
            waitFor { searchResults.size > 0 }
            searchResults.forEach {
                assertTrue(it.text.uppercase().contains(message.uppercase()))
            }
        }
        Thread.sleep(1000)
    }
}
