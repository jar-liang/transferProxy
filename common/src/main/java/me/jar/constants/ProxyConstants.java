package me.jar.constants;

import me.jar.utils.PlatformUtil;

import java.util.Map;

/**
 * @Description
 * @Date 2021/4/21-22:12
 */
public interface ProxyConstants {

    /**
     * 特定标识字节，用于标识数据流是否合法源发出
     */
    byte[] MARK_BYTE = new byte[] {2, 4, 6, 8};

    int WIN_OS = 1;

    int LINUX_OS = 2;

    int OTHER_OS = 3;

    String PROPERTY_NAME_WIN = "D:\\usr\\property\\property.txt";

    String PROPERTY_NAME_LINUX= "/usr/property/property.txt";

    String KEY_NAME_PORT = "listenning.port";

    Map<String, String> PROPERTY = PlatformUtil.getProperty();

    String PROPERTY_NAME_KEY = "key";

    String FAR_SERVER_IP = "far.ip";

    String FAR_SERVER_PORT = "far.port";
}
