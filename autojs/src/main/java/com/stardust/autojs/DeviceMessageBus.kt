package com.stardust.autojs

import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

/**
 * A shared message bus that allows the DevPlugin (app module) to push
 * server-sent messages to the JS scripting engine (autojs module).
 *
 * JS scripts can access this via:
 *   Packages.com.stardust.autojs.DeviceMessageBus.INSTANCE.pollAck(seq)
 */
object DeviceMessageBus {

    private const val MAX_ACK_ENTRIES = 200
    private const val ACK_TTL_MS = 60_000L // 60 seconds

    private val ackMap = ConcurrentHashMap<String, JSONObject>()

    /**
     * Put an ACK message from the server. Called by DevPlugin.
     * Automatically evicts oldest entries when map exceeds MAX_ACK_ENTRIES,
     * and removes entries older than ACK_TTL_MS.
     */
    fun putAck(seq: Int, event: String, taskNo: String) {
        val json = JSONObject()
        json.put("seq", seq)
        json.put("event", event)
        json.put("taskNo", taskNo)
        json.put("time", System.currentTimeMillis())
        ackMap["ack_$seq"] = json
        evictIfNeeded()
    }

    /**
     * Poll and consume an ACK message by seq. Called from JS.
     * Returns null if no ACK with the given seq exists.
     */
    fun pollAck(seq: Int): JSONObject? {
        return ackMap.remove("ack_$seq")
    }

    /**
     * Poll raw JSON string for easier JS interop.
     */
    fun pollAckString(seq: Int): String? {
        val json = ackMap.remove("ack_$seq")
        return json?.toString()
    }

    private fun evictIfNeeded() {
        if (ackMap.size <= MAX_ACK_ENTRIES) {
            return
        }
        val now = System.currentTimeMillis()
        // First pass: remove all stale entries (older than TTL)
        val iter = ackMap.entries.iterator()
        while (iter.hasNext()) {
            val entry = iter.next()
            val time = entry.value.optLong("time", 0)
            if (now - time > ACK_TTL_MS) {
                iter.remove()
            }
        }
        // Second pass: if still over limit, remove oldest entries
        if (ackMap.size > MAX_ACK_ENTRIES) {
            val entries = ackMap.entries.sortedBy { it.value.optLong("time", 0) }
            val toRemove = entries.take(ackMap.size - MAX_ACK_ENTRIES / 2)
            for (entry in toRemove) {
                ackMap.remove(entry.key)
            }
        }
    }
}