package com.fc.ideapluginsvmoptions

import com.intellij.execution.JavaRunConfigurationBase
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.RunConfigurationExtension
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.openapi.diagnostic.Logger
import java.util.regex.Pattern

class DubboIpRunConfigurationExtension : RunConfigurationExtension() {
    companion object {
        private val LOG = Logger.getInstance(DubboIpRunConfigurationExtension::class.java)
    }

//    override fun extendCreatedConfiguration(
//        configuration: RunConfigurationBase<*>,
//        location: Location<*>
//    ) {
//        LOG.info("extendCreatedConfiguration called for ${configuration.javaClass.simpleName}")
//        if (configuration is JavaRunConfigurationBase) {
//            val ip = IpUtils.getPreferredIp()
//            val dubboOptions = "-DDUBBO_IP_TO_BIND=$ip -DDUBBO_IP_TO_REGISTRY=$ip"
//
//            LOG.info("Generated Dubbo VM options: $dubboOptions")
//
//            // 使用正确的API来设置VM参数
//            val currentVmParameters = configuration.vmParameters
//            val newVmParameters = if (currentVmParameters.isNullOrBlank()) {
//                dubboOptions
//            } else {
//                "$currentVmParameters $dubboOptions"
//            }
//
//            configuration.vmParameters = newVmParameters
//            LOG.info("Updated VM options: '$newVmParameters'")
//        } else {
//            LOG.info("Configuration not applicable: ${configuration.javaClass.simpleName}")
//        }
//    }

    override fun isApplicableFor(configuration: RunConfigurationBase<*>): Boolean {
        val applicable = configuration is JavaRunConfigurationBase
        LOG.info("isApplicableFor ${configuration.javaClass.simpleName}: $applicable")
        return applicable
    }

    override fun <T : RunConfigurationBase<*>> updateJavaParameters(
        configuration: T,
        javaParameters: JavaParameters,
        runnerSettings: RunnerSettings?
    ) {
        LOG.info("AutoUpdateVmOptionsExtension: updateJavaParameters for '${configuration.name}'")
        if (configuration !is JavaRunConfigurationBase) {
            return
        }

        // --- 核心逻辑开始 ---

        // 1. 获取动态值和最终的VM Options字符串
        val ip = IpUtils.getPreferredIp()
        val dubboOptions = "-DDUBBO_IP_TO_BIND=$ip -DDUBBO_IP_TO_REGISTRY=$ip"

        val currentVmOptions = configuration.vmParameters ?: ""

        val pattern = Pattern.compile("-D(DUBBO_IP_TO_BIND|DUBBO_IP_TO_REGISTRY)=\\S*")
        val matcher = pattern.matcher(currentVmOptions)
        val cleanedVmOptions = matcher.replaceAll("").trim().replace("\\s+".toRegex(), " ")

        val newVmOptions = if (cleanedVmOptions.isBlank()) {
            dubboOptions
        } else {
            "$cleanedVmOptions $dubboOptions"
        }

        // 2.【关键】将新参数写回到持久化配置中（用于更新UI）
        // 检查值是否真的改变了，避免不必要的写入
        if (configuration.vmParameters != newVmOptions) {
            LOG.info("Updating persistent VM options from '${configuration.vmParameters}' to '$newVmOptions'")
            configuration.vmParameters = newVmOptions
        } else {
            LOG.info("Persistent VM options are already up-to-date.")
        }

        // 3.【关键】同时也要更新本次运行的参数
        // 清理一下临时的VM List，防止重复
        javaParameters.vmParametersList.clearAll()
        // 添加所有更新后的参数
        javaParameters.vmParametersList.addParametersString(newVmOptions)

        LOG.info("Final runtime VM parameters: '${javaParameters.vmParametersList.parametersString}'")

        // --- 核心逻辑结束 ---
    }
}