package com.jarvis.ai.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * JarvisAccessibilityService — CRASH-PROOF version.
 *
 * Every public method and lifecycle callback is wrapped in try-catch.
 * The service NEVER crashes — any error is logged and silently handled.
 *
 * Modded by Piash
 */
class JarvisAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "JarvisA11y"

        @Volatile
        var instance: JarvisAccessibilityService? = null
            private set

        val isRunning: Boolean get() = instance != null

        val SOCIAL_PACKAGES = setOf(
            "com.whatsapp", "com.whatsapp.w4b",
            "org.telegram.messenger",
            "com.facebook.orca", "com.facebook.katana",
            "com.instagram.android", "com.twitter.android",
            "com.snapchat.android"
        )

        private const val WA_MESSAGE_LIST = "com.whatsapp:id/message_list"
        private const val WA_INPUT_FIELD = "com.whatsapp:id/entry"
        private const val WA_SEND_BUTTON = "com.whatsapp:id/send"
        private const val WA_CONTACT_NAME = "com.whatsapp:id/conversation_contact_name"

        private const val TG_MESSAGE_LIST = "org.telegram.messenger:id/chat_list_view"
        private const val TG_INPUT_FIELD = "org.telegram.messenger:id/message_input_field"
        private const val TG_SEND_BUTTON = "org.telegram.messenger:id/send_button"
    }

    // ------------------------------------------------------------------ //
    //  Event Flow                                                          //
    // ------------------------------------------------------------------ //

    private val _screenEvents = MutableSharedFlow<ScreenEvent>(
        replay = 0, extraBufferCapacity = 64
    )
    val screenEvents: SharedFlow<ScreenEvent> = _screenEvents.asSharedFlow()

    private var serviceScope: CoroutineScope? = null

    var currentPackage: String = ""
        private set

    // ------------------------------------------------------------------ //
    //  Lifecycle — ALL wrapped in try-catch                                //
    // ------------------------------------------------------------------ //

    override fun onServiceConnected() {
        try {
            super.onServiceConnected()
            instance = this
            serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

            // Configure service info programmatically (safe way)
            try {
                val info = serviceInfo
                if (info != null) {
                    info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK
                    info.feedbackType = AccessibilityServiceInfo.FEEDBACK_SPOKEN or
                            AccessibilityServiceInfo.FEEDBACK_GENERIC
                    info.flags = info.flags or
                            AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                            AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                            AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
                    info.notificationTimeout = 100
                    serviceInfo = info
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not set serviceInfo programmatically: ${e.message}")
                // Not fatal — XML config will be used instead
            }

            Log.i(TAG, "Jarvis Accessibility Service CONNECTED successfully")
        } catch (e: Exception) {
            Log.e(TAG, "CRITICAL: onServiceConnected crashed", e)
            instance = this // Still set instance so isRunning returns true
        }
    }

    override fun onDestroy() {
        try {
            instance = null
            serviceScope?.cancel()
            serviceScope = null
            Log.i(TAG, "Jarvis Accessibility Service DESTROYED")
        } catch (e: Exception) {
            Log.e(TAG, "onDestroy error", e)
            instance = null
        }
        try { super.onDestroy() } catch (_: Exception) {}
    }

    override fun onInterrupt() {
        Log.w(TAG, "Accessibility service interrupted")
    }

    // ------------------------------------------------------------------ //
    //  Event Dispatch — wrapped in try-catch, NEVER crashes               //
    // ------------------------------------------------------------------ //

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        try {
            if (event == null) return

            val pkg = event.packageName?.toString() ?: return
            currentPackage = pkg

            when (event.eventType) {
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                    serviceScope?.launch {
                        try {
                            _screenEvents.emit(
                                ScreenEvent.WindowChanged(
                                    packageName = pkg,
                                    className = event.className?.toString() ?: "",
                                    timestamp = System.currentTimeMillis()
                                )
                            )
                        } catch (_: Exception) {}
                    }
                }

                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                    if (pkg in SOCIAL_PACKAGES) {
                        serviceScope?.launch {
                            try {
                                _screenEvents.emit(
                                    ScreenEvent.ContentChanged(
                                        packageName = pkg,
                                        timestamp = System.currentTimeMillis()
                                    )
                                )
                            } catch (_: Exception) {}
                        }
                    }
                }

                AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED -> {
                    try {
                        val text = event.text?.joinToString(" ") ?: ""
                        if (text.isNotBlank() && pkg in SOCIAL_PACKAGES) {
                            serviceScope?.launch {
                                try {
                                    _screenEvents.emit(
                                        ScreenEvent.NotificationDetected(
                                            packageName = pkg,
                                            text = text,
                                            timestamp = System.currentTimeMillis()
                                        )
                                    )
                                } catch (_: Exception) {}
                            }
                        }
                    } catch (_: Exception) {}
                }

                else -> { /* Ignore */ }
            }
        } catch (e: Exception) {
            Log.e(TAG, "onAccessibilityEvent crashed (non-fatal)", e)
            // DO NOT rethrow — service must stay alive
        }
    }

    // ------------------------------------------------------------------ //
    //  PUBLIC API: Screen Reading — all wrapped in try-catch              //
    // ------------------------------------------------------------------ //

    fun readScreenText(): List<ScreenNode> {
        return try {
            val rootNode = rootInActiveWindow ?: return emptyList()
            val nodes = mutableListOf<ScreenNode>()
            traverseNode(rootNode, nodes, depth = 0)
            try { rootNode.recycle() } catch (_: Exception) {}
            nodes
        } catch (e: Exception) {
            Log.e(TAG, "readScreenText error", e)
            emptyList()
        }
    }

    fun readScreenTextFlat(): String {
        return try {
            readScreenText()
                .filter { it.text.isNotBlank() }
                .joinToString("\n") { it.text }
        } catch (e: Exception) {
            Log.e(TAG, "readScreenTextFlat error", e)
            ""
        }
    }

    fun readLastMessages(count: Int = 5): List<MessageInfo> {
        return try {
            val rootNode = rootInActiveWindow ?: return emptyList()
            val messages = mutableListOf<MessageInfo>()

            val messageListId = when (currentPackage) {
                "com.whatsapp", "com.whatsapp.w4b" -> WA_MESSAGE_LIST
                "org.telegram.messenger" -> TG_MESSAGE_LIST
                else -> null
            }

            if (messageListId != null) {
                val listNodes = rootNode.findAccessibilityNodeInfosByViewId(messageListId)
                listNodes?.firstOrNull()?.let { listNode ->
                    extractMessagesFromList(listNode, messages)
                    try { listNode.recycle() } catch (_: Exception) {}
                }
            } else {
                val allNodes = readScreenText()
                allNodes.takeLast(count).forEach { node ->
                    messages.add(MessageInfo(sender = "", text = node.text, timestamp = ""))
                }
            }

            try { rootNode.recycle() } catch (_: Exception) {}
            messages.takeLast(count)
        } catch (e: Exception) {
            Log.e(TAG, "readLastMessages error", e)
            emptyList()
        }
    }

    fun getCurrentChatName(): String? {
        return try {
            val rootNode = rootInActiveWindow ?: return null
            val nameId = when (currentPackage) {
                "com.whatsapp", "com.whatsapp.w4b" -> WA_CONTACT_NAME
                else -> null
            }
            val name = if (nameId != null) {
                rootNode.findAccessibilityNodeInfosByViewId(nameId)
                    ?.firstOrNull()?.text?.toString()
            } else null
            try { rootNode.recycle() } catch (_: Exception) {}
            name
        } catch (e: Exception) {
            Log.e(TAG, "getCurrentChatName error", e)
            null
        }
    }

    // ------------------------------------------------------------------ //
    //  PUBLIC API: Interaction — all wrapped in try-catch                 //
    // ------------------------------------------------------------------ //

    fun clickNodeByText(text: String, exact: Boolean = false): Boolean {
        return try {
            val rootNode = rootInActiveWindow ?: return false
            val target = findNodeByText(rootNode, text, exact)
            val result = if (target != null) performClickOnNode(target) else false
            try { rootNode.recycle() } catch (_: Exception) {}
            result
        } catch (e: Exception) {
            Log.e(TAG, "clickNodeByText error", e)
            false
        }
    }

    fun clickNodeById(viewId: String): Boolean {
        return try {
            val rootNode = rootInActiveWindow ?: return false
            val nodes = rootNode.findAccessibilityNodeInfosByViewId(viewId)
            val target = nodes?.firstOrNull()
            val result = if (target != null) performClickOnNode(target) else false
            try { rootNode.recycle() } catch (_: Exception) {}
            result
        } catch (e: Exception) {
            Log.e(TAG, "clickNodeById error", e)
            false
        }
    }

    fun typeText(text: String, viewId: String? = null): Boolean {
        return try {
            val rootNode = rootInActiveWindow ?: return false
            val inputNode = if (viewId != null) {
                rootNode.findAccessibilityNodeInfosByViewId(viewId)?.firstOrNull()
            } else {
                findFocusedInputNode(rootNode)
            }

            if (inputNode == null) {
                try { rootNode.recycle() } catch (_: Exception) {}
                return false
            }

            inputNode.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
            val args = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            }
            val result = inputNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
            try { rootNode.recycle() } catch (_: Exception) {}
            result
        } catch (e: Exception) {
            Log.e(TAG, "typeText error", e)
            false
        }
    }

    fun sendMessage(message: String): Boolean {
        return try {
            val (inputId, sendId) = when (currentPackage) {
                "com.whatsapp", "com.whatsapp.w4b" -> WA_INPUT_FIELD to WA_SEND_BUTTON
                "org.telegram.messenger" -> TG_INPUT_FIELD to TG_SEND_BUTTON
                else -> {
                    val typed = typeText(message)
                    if (!typed) return false
                    Thread.sleep(500)
                    return clickNodeByText("Send") || clickNodeByText("send") || clickNodeByText("পাঠান")
                }
            }

            if (!typeText(message, inputId)) return false

            for (attempt in 1..3) {
                Thread.sleep(200L * attempt)
                if (clickNodeById(sendId)) return true
            }
            Thread.sleep(300)
            clickNodeByText("Send") || clickNodeByText("send")
        } catch (e: Exception) {
            Log.e(TAG, "sendMessage error", e)
            false
        }
    }

    fun scroll(direction: ScrollDirection): Boolean {
        return try {
            val rootNode = rootInActiveWindow ?: return false
            val scrollable = findScrollableNode(rootNode)
            val action = when (direction) {
                ScrollDirection.UP -> AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
                ScrollDirection.DOWN -> AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
            }
            val result = scrollable?.performAction(action) ?: false
            try { rootNode.recycle() } catch (_: Exception) {}
            result
        } catch (e: Exception) {
            Log.e(TAG, "scroll error", e)
            false
        }
    }

    fun tapAtCoordinates(x: Float, y: Float, durationMs: Long = 100): Boolean {
        return try {
            val path = Path().apply { moveTo(x, y) }
            val stroke = GestureDescription.StrokeDescription(path, 0, durationMs)
            val gesture = GestureDescription.Builder().addStroke(stroke).build()
            dispatchGesture(gesture, null, null)
            true
        } catch (e: Exception) {
            Log.e(TAG, "tapAtCoordinates error", e)
            false
        }
    }

    fun swipe(startX: Float, startY: Float, endX: Float, endY: Float, durationMs: Long = 300): Boolean {
        return try {
            val path = Path().apply { moveTo(startX, startY); lineTo(endX, endY) }
            val stroke = GestureDescription.StrokeDescription(path, 0, durationMs)
            val gesture = GestureDescription.Builder().addStroke(stroke).build()
            dispatchGesture(gesture, null, null)
            true
        } catch (e: Exception) {
            Log.e(TAG, "swipe error", e)
            false
        }
    }

    fun pressBack(): Boolean = try { performGlobalAction(GLOBAL_ACTION_BACK) } catch (_: Exception) { false }
    fun pressHome(): Boolean = try { performGlobalAction(GLOBAL_ACTION_HOME) } catch (_: Exception) { false }
    fun openRecents(): Boolean = try { performGlobalAction(GLOBAL_ACTION_RECENTS) } catch (_: Exception) { false }
    fun openNotifications(): Boolean = try { performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS) } catch (_: Exception) { false }

    // ------------------------------------------------------------------ //
    //  INTERNAL: Tree Traversal — all safe                                //
    // ------------------------------------------------------------------ //

    private fun traverseNode(node: AccessibilityNodeInfo, result: MutableList<ScreenNode>, depth: Int) {
        try {
            val text = node.text?.toString() ?: ""
            val contentDesc = node.contentDescription?.toString() ?: ""
            val viewId = try { node.viewIdResourceName ?: "" } catch (_: Exception) { "" }

            if (text.isNotBlank() || contentDesc.isNotBlank()) {
                val bounds = Rect()
                node.getBoundsInScreen(bounds)
                result.add(ScreenNode(
                    text = text.ifBlank { contentDesc },
                    viewId = viewId,
                    className = node.className?.toString() ?: "",
                    bounds = bounds,
                    isClickable = node.isClickable,
                    isEditable = node.isEditable,
                    depth = depth
                ))
            }

            for (i in 0 until node.childCount) {
                try {
                    val child = node.getChild(i) ?: continue
                    traverseNode(child, result, depth + 1)
                    try { child.recycle() } catch (_: Exception) {}
                } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            Log.w(TAG, "traverseNode error at depth $depth", e)
        }
    }

    private fun extractMessagesFromList(listNode: AccessibilityNodeInfo, messages: MutableList<MessageInfo>) {
        try {
            for (i in 0 until listNode.childCount) {
                try {
                    val child = listNode.getChild(i) ?: continue
                    val textParts = mutableListOf<String>()
                    extractAllText(child, textParts)
                    if (textParts.isNotEmpty()) {
                        messages.add(MessageInfo(
                            sender = textParts.firstOrNull() ?: "",
                            text = textParts.drop(1).joinToString(" "),
                            timestamp = ""
                        ))
                    }
                    try { child.recycle() } catch (_: Exception) {}
                } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            Log.w(TAG, "extractMessagesFromList error", e)
        }
    }

    private fun extractAllText(node: AccessibilityNodeInfo, result: MutableList<String>) {
        try {
            node.text?.toString()?.let { if (it.isNotBlank()) result.add(it) }
            for (i in 0 until node.childCount) {
                try {
                    val child = node.getChild(i) ?: continue
                    extractAllText(child, result)
                    try { child.recycle() } catch (_: Exception) {}
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
    }

    private fun findNodeByText(node: AccessibilityNodeInfo, text: String, exact: Boolean): AccessibilityNodeInfo? {
        return try {
            val nodeText = node.text?.toString() ?: ""
            val match = if (exact) nodeText.equals(text, ignoreCase = true)
            else nodeText.contains(text, ignoreCase = true)
            if (match) return node

            for (i in 0 until node.childCount) {
                try {
                    val child = node.getChild(i) ?: continue
                    val found = findNodeByText(child, text, exact)
                    if (found != null) return found
                    try { child.recycle() } catch (_: Exception) {}
                } catch (_: Exception) {}
            }
            null
        } catch (_: Exception) { null }
    }

    private fun findFocusedInputNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        return try {
            if (node.isFocused && node.isEditable) return node
            for (i in 0 until node.childCount) {
                try {
                    val child = node.getChild(i) ?: continue
                    val found = findFocusedInputNode(child)
                    if (found != null) return found
                    try { child.recycle() } catch (_: Exception) {}
                } catch (_: Exception) {}
            }
            null
        } catch (_: Exception) { null }
    }

    private fun findScrollableNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        return try {
            if (node.isScrollable) return node
            for (i in 0 until node.childCount) {
                try {
                    val child = node.getChild(i) ?: continue
                    val found = findScrollableNode(child)
                    if (found != null) return found
                    try { child.recycle() } catch (_: Exception) {}
                } catch (_: Exception) {}
            }
            null
        } catch (_: Exception) { null }
    }

    private fun performClickOnNode(node: AccessibilityNodeInfo): Boolean {
        return try {
            if (node.isClickable) return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            var parent = node.parent
            while (parent != null) {
                if (parent.isClickable) {
                    val result = parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    try { parent.recycle() } catch (_: Exception) {}
                    return result
                }
                val gp = parent.parent
                try { parent.recycle() } catch (_: Exception) {}
                parent = gp
            }
            val bounds = Rect()
            node.getBoundsInScreen(bounds)
            tapAtCoordinates(bounds.centerX().toFloat(), bounds.centerY().toFloat())
        } catch (e: Exception) {
            Log.e(TAG, "performClickOnNode error", e)
            false
        }
    }

    // ------------------------------------------------------------------ //
    //  Data Classes                                                       //
    // ------------------------------------------------------------------ //

    data class ScreenNode(
        val text: String, val viewId: String, val className: String,
        val bounds: Rect, val isClickable: Boolean, val isEditable: Boolean, val depth: Int
    )

    data class MessageInfo(val sender: String, val text: String, val timestamp: String)

    enum class ScrollDirection { UP, DOWN }

    sealed class ScreenEvent {
        data class WindowChanged(val packageName: String, val className: String, val timestamp: Long) : ScreenEvent()
        data class ContentChanged(val packageName: String, val timestamp: Long) : ScreenEvent()
        data class NotificationDetected(val packageName: String, val text: String, val timestamp: Long) : ScreenEvent()
    }
}
