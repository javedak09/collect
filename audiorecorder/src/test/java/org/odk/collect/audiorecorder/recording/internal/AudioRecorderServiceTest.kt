package org.odk.collect.audiorecorder.recording.internal

import android.app.Application
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.not
import org.hamcrest.Matchers.nullValue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.odk.collect.audiorecorder.AudioRecorderDependencyModule
import org.odk.collect.audiorecorder.R
import org.odk.collect.audiorecorder.RobolectricApplication
import org.odk.collect.audiorecorder.recorder.Output
import org.odk.collect.audiorecorder.recorder.Recorder
import org.odk.collect.audiorecorder.setupDependencies
import org.odk.collect.audiorecorder.support.FakeRecorder
import org.robolectric.Robolectric.buildService
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ServiceController

@RunWith(RobolectricTestRunner::class)
class AudioRecorderServiceTest {

    private val application: RobolectricApplication by lazy { ApplicationProvider.getApplicationContext() }
    private val recorder = FakeRecorder()

    @Before
    fun setup() {
        application.setupDependencies(
            object : AudioRecorderDependencyModule() {
                override fun providesRecorder(application: Application): Recorder {
                    return recorder
                }
            }
        )
    }

    @Test
    fun startAction_startsInForegroundWithNotification() {
        val service = buildService(AudioRecorderService::class.java, createStartIntent("123"))
            .create()
            .startCommand(0, 0)

        val notification = shadowOf(service.get()).lastForegroundNotification
        assertThat(notification, not(nullValue()))
        assertThat(shadowOf(notification).contentTitle, equalTo(application.getString(R.string.recording)))
        assertThat(shadowOf(notification.contentIntent).savedIntent.component?.className, equalTo(ReturnToAppActivity::class.qualifiedName))
    }

    @Test
    fun startAction_startsRecorder() {
        startService(createStartIntent("123"))
        assertThat(recorder.isRecording(), equalTo(true))
    }

    @Test
    fun startAction_whenRecordingInProgress_doesNothing() {
        val intent = createStartIntent("456")

        startService(intent)
        assertThat(recorder.isRecording(), equalTo(true))

        startService(intent)
        assertThat(recorder.isRecording(), equalTo(true))
        assertThat(recorder.recordings.size, equalTo(1))
    }

    @Test
    fun stopAction_stopsSelf() {
        startService(createStartIntent("123"))

        val stopIntent = Intent(application, AudioRecorderService::class.java)
        stopIntent.action = AudioRecorderService.ACTION_STOP
        val service = startService(stopIntent)

        assertThat(shadowOf(service.get()).isStoppedBySelf, equalTo(true))
    }

    @Test
    fun stopAction_stopsRecorder() {
        startService(createStartIntent("123"))

        val stopIntent = Intent(application, AudioRecorderService::class.java)
        stopIntent.action = AudioRecorderService.ACTION_STOP
        startService(stopIntent)

        assertThat(recorder.isRecording(), equalTo(false))
    }

    @Test
    fun cleanUpAction_whileRecording_cancelsRecorder() {
        startService(createStartIntent("123"))

        val cancelIntent = Intent(application, AudioRecorderService::class.java)
        cancelIntent.action = AudioRecorderService.ACTION_CLEAN_UP
        startService(cancelIntent)

        assertThat(recorder.isRecording(), equalTo(false))
        assertThat(recorder.wasCancelled(), equalTo(true))
    }

    @Test
    fun cleanUpAction_whileRecording_stopsSelf() {
        startService(createStartIntent("123"))

        val cancelIntent = Intent(application, AudioRecorderService::class.java)
        cancelIntent.action = AudioRecorderService.ACTION_CLEAN_UP
        val service = startService(cancelIntent)

        assertThat(shadowOf(service.get()).isStoppedBySelf, equalTo(true))
    }

    @Test
    fun cleanUpAction_afterRecording_stopsSelf_andDeletesFiles() {
        startService(createStartIntent("123"))

        val stopIntent = Intent(application, AudioRecorderService::class.java)
        stopIntent.action = AudioRecorderService.ACTION_STOP
        startService(stopIntent)

        val cancelIntent = Intent(application, AudioRecorderService::class.java)
        cancelIntent.action = AudioRecorderService.ACTION_CLEAN_UP
        val service = startService(cancelIntent)

        assertThat(shadowOf(service.get()).isStoppedBySelf, equalTo(true))
        assertThat(recorder.file?.exists(), equalTo(false))
    }

    @Test
    fun whenUserKillsAppFromTaskManager_cancelsRecorder_stopsSelf() {
        val service = startService(createStartIntent("123"))

        service.get().onTaskRemoved(Intent())

        assertThat(recorder.isRecording(), equalTo(false))
        assertThat(recorder.wasCancelled(), equalTo(true))
        assertThat(shadowOf(service.get()).isStoppedBySelf, equalTo(true))
    }

    private fun createStartIntent(sessionId: String): Intent {
        val intent = Intent(application, AudioRecorderService::class.java)
        intent.action = AudioRecorderService.ACTION_START
        intent.putExtra(AudioRecorderService.EXTRA_SESSION_ID, sessionId)
        intent.putExtra(AudioRecorderService.EXTRA_OUTPUT, Output.AAC)
        return intent
    }

    private fun startService(intent: Intent): ServiceController<AudioRecorderService> {
        return buildService(AudioRecorderService::class.java, intent)
            .create()
            .startCommand(0, 0)
    }
}