package com.fc.ideapluginsvmoptions

import com.intellij.execution.JavaRunConfigurationBase
import com.intellij.execution.RunManager
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import java.util.regex.Pattern

class UpdateDubboIpAction : AnAction() {

    /**
     * 在动作执行前更新其状态（是否可用/可见）
     */
    override fun update(e: AnActionEvent) {
        val project = e.project
        val presentation = e.presentation

        if (project == null) {
            presentation.isEnabledAndVisible = false
            return
        }

        // 获取当前选中的运行配置
        val selectedConfiguration = RunManager.getInstance(project).selectedConfiguration

        // 只有当选中的是一个Java配置时，我们的按钮才可用
        val isJavaConfig = selectedConfiguration?.configuration is JavaRunConfigurationBase
        presentation.isEnabledAndVisible = isJavaConfig
    }

    /**
     * 用户点击按钮后执行的动作
     */
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val runManager = RunManager.getInstance(project)
        val selectedConfiguration = runManager.selectedConfiguration?.configuration

        if (selectedConfiguration !is JavaRunConfigurationBase) {
            return
        }

        // 1. 获取本地IP和要设置的VM Options
        val ip = IpUtils.getPreferredIp()
        val dubboOptions = "-DDUBBO_IP_TO_BIND=$ip -DDUBBO_IP_TO_REGISTRY=$ip"

        // 2. 获取当前已有的VM Options
        val currentVmOptions = selectedConfiguration.vmParameters ?: ""

        // 3. (高级) 移除旧的Dubbo IP设置，避免重复添加
        // 使用正则表达式匹配并替换旧的参数
        val pattern = Pattern.compile("-D(DUBBO_IP_TO_BIND|DUBBO_IP_TO_REGISTRY)=\\S*")
        val matcher = pattern.matcher(currentVmOptions)
        val cleanedVmOptions = matcher.replaceAll("").trim().replace("\\s+".toRegex(), " ")

        // 4. 组合新的VM Options
        val newVmOptions = if (cleanedVmOptions.isBlank()) {
            dubboOptions
        } else {
            "$cleanedVmOptions $dubboOptions"
        }

        // 5. 将新的VM Options写回配置中
        selectedConfiguration.vmParameters = newVmOptions

        // 6. (可选但推荐) 给用户一个反馈提示
        showNotification(project, "VM Options Updated for '${selectedConfiguration.name}' with IP: $ip")
    }

    private fun showNotification(project: Project, content: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("dubbo-ip-updater.notification") // 建议在plugin.xml中注册
            .createNotification(content, NotificationType.INFORMATION)
            .notify(project)
    }
}