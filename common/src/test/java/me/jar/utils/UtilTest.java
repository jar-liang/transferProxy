package me.jar.utils;

import me.jar.constants.ProxyConstants;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * @Description
 * @Date 2021/4/24-17:42
 */
public class UtilTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(UtilTest.class);

    @Test
    public void testGetPlatform() {
        int platform = PlatformUtil.getPlatform();
        LOGGER.info("Running platform is {}", platform == ProxyConstants.WIN_OS ? "Windows" : platform == ProxyConstants.LINUX_OS ? "Linux" : "Other OS");
    }

    @Test
    public void testGetProperty() {
        Map<String, String> property = PlatformUtil.getProperty();
        property.forEach((key, value) -> LOGGER.info("{} = {}", key, value));
    }
}
