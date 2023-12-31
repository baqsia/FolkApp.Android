package ge.baqar.gogia.goefolk.job

import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import ge.baqar.gogia.goefolk.storage.db.FolkApiDao
import ge.baqar.gogia.goefolk.storage.usecase.FileSaveController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.TimeUnit

class SyncFilesAndDatabaseJob(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams), KoinComponent, CoroutineScope {

    override val coroutineContext = Dispatchers.IO + SupervisorJob()
    private val folkApiDao: FolkApiDao by inject()
    private val saveController: FileSaveController by inject()

    override fun doWork(): Result {
        launch {
            val ensembles = folkApiDao.ensembles()
            ensembles.forEach { ensemble ->
                var songs = folkApiDao.songsByEnsembleId(ensemble.referenceId)

                val removalSongs = songs.filter { song ->
                    val associatedFile =
                        saveController.exists(ensemble.nameEng, song.nameEng)
                    !associatedFile
                }.map { it.referenceId }
                folkApiDao.removeSongsByIds(removalSongs)

                songs = folkApiDao.songsByEnsembleId(ensemble.referenceId)

                if(songs.isEmpty())
                    folkApiDao.removeEnsemble(ensemble.referenceId)
            }
        }


        val firstWorkRequest = OneTimeWorkRequestBuilder<SyncFilesAndDatabaseJob>()
            .setInitialDelay(1, TimeUnit.HOURS)
            .build()

        WorkManager.getInstance(applicationContext)
            .enqueue(firstWorkRequest)

        return Result.success()
    }

    companion object {
        fun triggerNow(context: Context) {
            val firstWorkRequest = OneTimeWorkRequestBuilder<SyncFilesAndDatabaseJob>()
                .build()
            WorkManager.getInstance(context)
                .enqueue(firstWorkRequest)
        }
    }
}