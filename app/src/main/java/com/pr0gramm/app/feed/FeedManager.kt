package com.pr0gramm.app.feed

import com.pr0gramm.app.api.pr0gramm.Api
import com.pr0gramm.app.util.BackgroundScheduler
import org.slf4j.LoggerFactory
import rx.Observable
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.subjects.BehaviorSubject
import rx.subjects.Subject
import rx.subscriptions.Subscriptions

class FeedManager(val feedService: FeedService, feed: Feed) {
    private val logger = LoggerFactory.getLogger("FeedService")

    var feed: Feed = feed
        private set

    private val subject: Subject<Update, Update> = BehaviorSubject.create<Update>().toSerialized()
    private var subscription: Subscription = Subscriptions.unsubscribed()

    /**
     * True, if this feed manager is currently performing a load operation.
     */
    val isLoading: Boolean get() = !subscription.isUnsubscribed

    val feedType: FeedType get() = feed.feedType

    val updates: Observable<Update> get() = subject
            .observeOn(AndroidSchedulers.mainThread())
            .startWith(Update.NewFeed(feed))

    /**
     * Stops all loading operations and resets the feed to the given value.
     */
    fun reset(feed: Feed = this.feed.copy(items = listOf(), isAtStart = false, isAtEnd = false)) {
        stop()
        publish(feed, remote = false)
    }

    /**
     * Resets the current feed and loads all items around the given offset.
     * Leave 'around' null to just load from the beginning.
     */
    fun restart(around: Long? = null) {
        stop()
        subscribeTo(feedService.load(feedQuery().copy(around = around)))
    }

    /**
     * Load the next page of the feed
     */
    fun next() {
        val oldest = feed.oldest
        if (feed.isAtEnd || isLoading || oldest == null)
            return

        subscribeTo(feedService.load(feedQuery().copy(older = oldest.id(feedType))))
    }

    /**
     * Load the previous page of the feed
     */
    fun previous() {
        val newest = feed.newest
        if (feed.isAtStart || isLoading || newest == null)
            return

        subscribeTo(feedService.load(feedQuery().copy(newer = newest.id(feedType))))
    }

    fun stop() {
        subscription.unsubscribe()
    }

    private fun subscribeTo(observable: Observable<Api.Feed>) {
        logger.info("Subscribing to new load-request now.")

        subscription.unsubscribe()
        subscription = observable
                .subscribeOn(BackgroundScheduler.instance())
                .unsubscribeOn(BackgroundScheduler.instance())
                .doOnSubscribe { subject.onNext(Update.LoadingStarted()) }
                .doOnUnsubscribe { subject.onNext(Update.LoadingStopped()) }
                .subscribe({ handleFeedUpdate(it) }, { publishError(it) })

    }

    private fun handleFeedUpdate(update: Api.Feed) {
        // check for invalid content type.
        if (update.error != null) {
            publishError(InvalidContentTypeException())
            return
        }

        val merged = feed.mergeWith(update)
        publish(merged, remote = true)
    }

    private fun publishError(err: Throwable) {
        subject.onNext(Update.Error(err))
    }

    /**
     * Update and publish a new feed.
     */
    private fun publish(newFeed: Feed, remote: Boolean) {
        feed = newFeed
        subject.onNext(Update.NewFeed(newFeed, remote))
    }

    private fun feedQuery(): FeedService.FeedQuery {
        return FeedService.FeedQuery(feed.filter, feed.contentType)
    }

    class InvalidContentTypeException : RuntimeException()

    sealed class Update {
        class LoadingStarted : Update()
        class LoadingStopped : Update()
        class Error(val err: Throwable) : Update()
        class NewFeed(val feed: Feed, val remote: Boolean = false) : Update()
    }
}