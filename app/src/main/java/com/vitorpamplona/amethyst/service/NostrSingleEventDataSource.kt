package com.vitorpamplona.amethyst.service

import com.vitorpamplona.amethyst.model.LocalCache
import com.vitorpamplona.amethyst.model.Note
import java.util.Collections
import nostr.postr.JsonFilter

object NostrSingleEventDataSource: NostrDataSource("SingleEventFeed") {
  val eventsToWatch = Collections.synchronizedList(mutableListOf<String>())

  fun createRepliesAndReactionsFilter(): JsonFilter? {
    val reactionsToWatch = eventsToWatch.map { it.substring(0, 8) }

    if (reactionsToWatch.isEmpty()) {
      return null
    }

    return JsonFilter(
      tags = mapOf("e" to reactionsToWatch)
    )
  }

  fun createLoadEventsIfNotLoadedFilter(): JsonFilter? {
    val directEventsToLoad = eventsToWatch
      .mapNotNull { LocalCache.notes[it] }
      .filter { it.event == null }

    val threadingEventsToLoad = eventsToWatch
      .mapNotNull { LocalCache.notes[it] }
      .mapNotNull { it.replyTo }
      .flatten()
      .filter { it.event == null }

    val interestedEvents =
      (directEventsToLoad + threadingEventsToLoad)
      .map { it.idHex.substring(0, 8) }

    if (interestedEvents.isEmpty()) {
      return null
    }

    return JsonFilter(
      ids = interestedEvents
    )
  }

  val repliesAndReactionsChannel = requestNewChannel()
  val loadEventsChannel = requestNewChannel()

  override fun feed(): List<Note> {
    return eventsToWatch.map {
      LocalCache.notes[it]
    }.filterNotNull()
  }

  override fun updateChannelFilters() {
    repliesAndReactionsChannel.filter = createRepliesAndReactionsFilter()
    loadEventsChannel.filter = createLoadEventsIfNotLoadedFilter()
  }

  fun add(eventId: String) {
    eventsToWatch.add(eventId)
    resetFilters()
  }

  fun remove(eventId: String) {
    eventsToWatch.remove(eventId)
    resetFilters()
  }
}