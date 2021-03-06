package com.pr0gramm.app.ui.fragments

import android.app.Dialog
import android.os.Bundle
import android.widget.ProgressBar
import com.pr0gramm.app.BuildConfig
import com.pr0gramm.app.R
import com.pr0gramm.app.services.DownloadService
import com.pr0gramm.app.ui.base.BaseDialogFragment
import com.pr0gramm.app.ui.showDialog
import com.pr0gramm.app.util.AndroidUtility.checkMainThread
import com.pr0gramm.app.util.onErrorResumeEmpty
import com.trello.rxlifecycle.android.FragmentEvent
import rx.Observable

/**
 * This dialog shows the progress while downloading something.
 */
class DownloadUpdateDialog(private val progress: Observable<DownloadService.Status> = Observable.empty()) : BaseDialogFragment("DownloadUpdateDialog") {
    private val progressBar: ProgressBar by bindView(R.id.progress)

    init {
        logger.info("New instance of DownloadUpdateDialog")
        retainInstance = true
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = showDialog(activity) {
            layout(R.layout.progress_update)
        }

        if (!BuildConfig.DEBUG) {
            dialog.setCancelable(false)
            dialog.setCanceledOnTouchOutside(false)
        }

        return dialog
    }

    override fun onDialogViewCreated() {
        progressBar.isIndeterminate = true
        progressBar.max = 1000
        progress.compose(bindUntilEvent(FragmentEvent.DESTROY_VIEW))
                .onErrorResumeEmpty()
                .doAfterTerminate { this.dismiss() }
                .subscribe { this.updateStatus(it) }
    }

    private fun updateStatus(status: DownloadService.Status) {
        checkMainThread()

        progressBar.isIndeterminate = status.progress < 0
        progressBar.progress = (1000 * status.progress).toInt()
    }
}
