package com.afilishop.app.ui

internal class VideoInteractionTracker {
    data class Session(val userId: String?, val generation: Long)
    data class Snapshot(val session: Session, val revision: Long, val startedDuringWrite: Boolean)
    data class Counts(val session: Session, val videoId: String, val revision: Long)
    data class Write(val session: Session, val videoId: String, val membership: Boolean)
    private var generation = 0L
    private var currentUserId: String? = null
    private var revision = 0L
    private var pendingMembership = 0
    private val countRevisions = mutableMapOf<String, Long>()
    private val pendingCounts = mutableMapOf<String, Int>()
    fun session(userId: String?): Session {
        if (userId != currentUserId) { reset(); currentUserId = userId }
        return Session(userId, generation)
    }
    fun isCurrent(session: Session, userId: String?) = session.generation == generation && session.userId == userId
    fun snapshot(userId: String?) = Snapshot(session(userId), revision, pendingMembership > 0)
    fun canApply(snapshot: Snapshot, userId: String?) = isCurrent(snapshot.session, userId) && snapshot.revision == revision && !snapshot.startedDuringWrite && pendingMembership == 0
    fun counts(videoId: String, userId: String?) = Counts(session(userId), videoId, countRevisions[videoId] ?: 0L)
    fun canApply(counts: Counts, userId: String?) = isCurrent(counts.session, userId) && counts.revision == (countRevisions[counts.videoId] ?: 0L) && (pendingCounts[counts.videoId] ?: 0) == 0
    fun begin(videoId: String, userId: String?, membership: Boolean): Write {
        val session = session(userId)
        if (membership) { pendingMembership++; revision++ }
        pendingCounts[videoId] = (pendingCounts[videoId] ?: 0) + 1
        bumpCounts(videoId)
        return Write(session, videoId, membership)
    }
    fun finish(write: Write, userId: String?): Boolean {
        if (!isCurrent(write.session, userId)) return false
        if (write.membership) { pendingMembership--; revision++ }
        val remaining = (pendingCounts[write.videoId] ?: 1) - 1
        if (remaining == 0) pendingCounts.remove(write.videoId) else pendingCounts[write.videoId] = remaining
        bumpCounts(write.videoId)
        return true
    }
    fun reset() { generation++; revision++; pendingMembership = 0; pendingCounts.clear(); countRevisions.clear() }
    private fun bumpCounts(videoId: String) { countRevisions[videoId] = (countRevisions[videoId] ?: 0L) + 1L }
}
