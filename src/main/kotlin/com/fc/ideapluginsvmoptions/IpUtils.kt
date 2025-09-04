package com.fc.ideapluginsvmoptions

import com.intellij.openapi.diagnostic.Logger
import java.net.NetworkInterface
object IpUtils {
    private val LOG = Logger.getInstance(IpUtils::class.java)
    
    fun getPreferredIp(prefix: String = "10.10."): String {
        try {
            LOG.info("Searching for preferred IP with prefix: $prefix")
            NetworkInterface.getNetworkInterfaces()?.iterator()?.forEach { iface ->
                if (iface.isUp && !iface.isLoopback) {
                    LOG.info("Checking interface: ${iface.name}")
                    iface.inetAddresses.iterator().forEach { addr ->
                        val ip = addr.hostAddress
                        LOG.info("Found IP address: $ip")
                        if (ip.startsWith(prefix) && ip != "127.0.0.1") {
                            LOG.info("Selected IP (with prefix match): $ip")
                            return ip
                        }
                    }
                }
            }
            
            // 如果没有找到匹配前缀的IP，选择第一个非回环IPv4地址
            LOG.info("No IP with prefix found, looking for any non-loopback IPv4 address")
            NetworkInterface.getNetworkInterfaces()?.iterator()?.forEach { iface ->
                if (iface.isUp && !iface.isLoopback) {
                    iface.inetAddresses.iterator().forEach { addr ->
                        val ip = addr.hostAddress
                        // 检查是否为IPv4地址且不是回环地址
                        if (!ip.contains(":") && !ip.startsWith("127.")) {
                            LOG.info("Selected IP (first non-loopback IPv4): $ip")
                            return ip
                        }
                    }
                }
            }
        } catch (e: Exception) {
            LOG.error("Error while getting preferred IP", e)
            e.printStackTrace()
        }
        LOG.info("No suitable IP found, using fallback: 127.0.0.1")
        return "127.0.0.1" // Fallback
    }
}